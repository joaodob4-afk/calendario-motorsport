"""Diagnóstico das fontes de horários da Fórmula E (treinos, classificação e corrida).

Só LÊ as fontes e imprime a estrutura no log; não altera nenhum arquivo do app.
Testa duas fontes:
  1) a API api.formula-e.pulselive.com (usada por apps de código aberto);
  2) as páginas de evento do site oficial fiaformulae.com.
"""
import json
import re
import urllib.error
import urllib.request

API = "https://api.formula-e.pulselive.com"
SITE = "https://www.fiaformulae.com"

CABECALHOS = [
    {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    {
        "User-Agent": "Mozilla/5.0",
        "Accept": "application/json",
        "Origin": SITE,
        "Referer": SITE + "/",
    },
]


def baixar(url, cabecalhos=None):
    req = urllib.request.Request(
        url, headers=cabecalhos or {"User-Agent": "Mozilla/5.0", "Accept-Language": "en"}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.geturl(), r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, url, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return None, url, ""


def api_json(caminho):
    url = API + caminho
    for numero, cab in enumerate(CABECALHOS, start=1):
        status, _, texto = baixar(url, cab)
        print(f"{url} | cabeçalhos #{numero} | status {status} | {len(texto)} bytes")
        if status == 200 and texto:
            try:
                return json.loads(texto)
            except ValueError:
                print("  resposta não é JSON")
    return None


def forma(obj, nivel=0, limite=2):
    """Resumo curto da estrutura de um JSON."""
    recuo = "  " * nivel
    if isinstance(obj, dict):
        for chave, valor in list(obj.items())[:25]:
            if isinstance(valor, (dict, list)) and nivel < limite:
                tipo = f"lista[{len(valor)}]" if isinstance(valor, list) else "objeto"
                print(f"{recuo}{chave}: {tipo}")
                forma(valor, nivel + 1, limite)
            else:
                print(f"{recuo}{chave}: {str(valor)[:70]!r}")
    elif isinstance(obj, list) and obj:
        print(f"{recuo}(primeiro item de {len(obj)})")
        forma(obj[0], nivel, limite)


def lista_de(obj, *chaves):
    """Primeira lista encontrada em obj (direto ou sob alguma das chaves)."""
    if isinstance(obj, list):
        return obj
    if isinstance(obj, dict):
        for chave in chaves:
            if isinstance(obj.get(chave), list):
                return obj[chave]
        for valor in obj.values():
            if isinstance(valor, list) and valor and isinstance(valor[0], dict):
                return valor
    return []


def testar_api():
    print("\n##### 1) API PULSELIVE #####")
    dados = api_json("/formula-e/v1/championships")
    campeonatos = lista_de(dados, "championships")
    if not campeonatos:
        print("Sem lista de campeonatos.")
        return

    print("campeonatos:", len(campeonatos))
    forma(campeonatos[0])
    for c in campeonatos[-4:]:
        print(" -", c.get("id"), "|", c.get("name"), "|", c.get("startDate"), "->", c.get("endDate"))

    for c in campeonatos[-2:]:
        cid = c.get("id")
        if not cid:
            continue
        print(f"\n--- corridas do campeonato {c.get('name')} ({cid}) ---")
        corridas_json = api_json(f"/formula-e/v1/races?championshipId={cid}")
        corridas = lista_de(corridas_json, "races")
        print("corridas:", len(corridas))
        if not corridas:
            continue
        forma(corridas[0], limite=3)
        for corrida in corridas[:3]:
            print("\n corrida:", corrida.get("name"), "|", corrida.get("city"), "|", corrida.get("date"))
            for sessao in lista_de(corrida, "sessions"):
                print("   sessão:", {k: sessao[k] for k in list(sessao)[:8]})


def testar_site():
    print("\n##### 2) SITE OFICIAL #####")
    status, final, html = baixar(SITE + "/en/calendar")
    print("calendário:", status, final, len(html), "bytes")

    # JSON-LD do calendário: endereços das etapas
    urls = []
    for bloco in re.findall(r"<script[^>]*ld\+json[^>]*>(.*?)</script>", html, flags=re.S):
        try:
            dados = json.loads(bloco.strip())
        except ValueError:
            continue
        print("JSON-LD:", dados.get("@type") if isinstance(dados, dict) else type(dados).__name__)
        achados = re.findall(r'"(?:url|@id)"\s*:\s*"(https?://[^"]+)"', json.dumps(dados))
        urls += [u for u in achados if "/calendar" in u or "/events" in u or "/race" in u]
    print("endereços de etapa no calendário:", urls[:5])

    candidatos = urls[:2] + [
        SITE + "/en/events/2026-27/jeddah",
        SITE + "/en/events/2025-26/berlin",
        SITE + "/en/events/2025-26/madrid",
    ]
    vistos = set()
    for url in candidatos:
        if url in vistos:
            continue
        vistos.add(url)
        status, final, pagina = baixar(url)
        print(f"\n{url} -> status {status} | final {final} | {len(pagina)} bytes")
        tipos = re.findall(r'"@type"\s*:\s*"([A-Za-z]+)"', pagina)
        print("  tipos JSON-LD:", sorted(set(tipos)))
        print("  tem subEvent:", "subEvent" in pagina)
        texto = re.sub(r"<[^>]+>", "\n", re.sub(r"<(script|style)\b.*?</\1>", " ", pagina, flags=re.S | re.I))
        linhas = [" ".join(l.split()) for l in texto.splitlines() if l.strip()]
        horarios = [
            l for l in linhas
            if re.search(r"\b\d{1,2}:\d{2}\b", l) and re.search(r"Practice|Qualifying|Race|Free", l, re.I)
        ]
        print("  linhas com horário de sessão:", len(horarios))
        for l in horarios[:12]:
            print("   ", l[:110])


if __name__ == "__main__":
    testar_api()
    testar_site()
