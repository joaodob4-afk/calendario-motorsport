"""Diagnóstico das últimas fontes de horários da Fórmula E (treinos, classificação, corrida).

Só LÊ as fontes e imprime a estrutura no log; não altera nenhum arquivo do app.
  1) base aberta sportstimes/f1 (licença MIT) no GitHub: lista as pastas de dados, acha a
     da Fórmula E e mostra as corridas e sessões dos arquivos mais recentes;
  2) páginas de venda de ingressos do site oficial, que podem trazer a programação do dia.
"""
import re
import json
import urllib.error
import urllib.request
from collections import Counter

REPO = "sportstimes/f1"
API = f"https://api.github.com/repos/{REPO}/contents/"
RAW = f"https://raw.githubusercontent.com/{REPO}/"

PASTAS_PROVAVEIS = ["formulae", "formula-e", "formulae-e", "fe", "formulaE"]
ANOS = [2027, 2026, 2025]
RAMOS = ["main", "master"]


def baixar(url):
    req = urllib.request.Request(
        url, headers={"User-Agent": "Mozilla/5.0", "Accept": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return None, ""


def baixar_json(url):
    status, texto = baixar(url)
    print(f"{url} | status {status} | {len(texto)} bytes")
    if status == 200 and texto:
        try:
            return json.loads(texto)
        except ValueError:
            print("  resposta não é JSON")
    return None


def listar(caminho):
    dados = baixar_json(API + caminho)
    return dados if isinstance(dados, list) else []


def resumir(dados, titulo):
    print(f"\n##### {titulo} #####")
    if not isinstance(dados, dict):
        print("formato inesperado:", type(dados).__name__)
        return
    print("chaves do arquivo:", sorted(dados.keys()))
    corridas = dados.get("races") or []
    print("corridas:", len(corridas))
    if not corridas:
        return

    print("campos da corrida:", sorted(corridas[0].keys()))

    chaves = Counter()
    for c in corridas:
        for k, v in (c.get("sessions") or {}).items():
            chaves[(k, "com horário" if v else "sem horário")] += 1
    print("sessões por tipo:", dict(chaves))

    for c in corridas[:2] + corridas[-2:]:
        print("\n corrida:", c.get("name"), "|", c.get("location"), "|", c.get("slug"))
        for k, v in (c.get("sessions") or {}).items():
            print("   ", k, "->", v)
    primeira = corridas[0]
    extras = {k: primeira[k] for k in list(primeira)[:10] if k != "sessions"}
    print("\n exemplo de campos:", extras)


def achar_arquivos():
    """Arquivos de dados da Fórmula E: pela API do GitHub, ou por endereços prováveis."""
    achados = []

    raiz = listar("_db")
    nomes = [i.get("name") for i in raiz]
    print("pastas em _db:", nomes)

    for item in raiz:
        nome = str(item.get("name", ""))
        if item.get("type") == "dir" and ("form" in nome.lower() and "e" in nome.lower() or nome.lower() == "fe"):
            arquivos = listar("_db/" + nome)
            print(f"arquivos em _db/{nome}:", [a.get("name") for a in arquivos])
            for a in sorted(arquivos, key=lambda x: x.get("name", ""))[-3:]:
                if a.get("download_url"):
                    achados.append((f"_db/{nome}/{a['name']}", a["download_url"]))

    if achados:
        return achados

    print("\nNada achado pela API; tentando endereços prováveis...")
    for ramo in RAMOS:
        for pasta in PASTAS_PROVAVEIS:
            for ano in ANOS:
                url = f"{RAW}{ramo}/_db/{pasta}/{ano}.json"
                status, _ = baixar(url)
                print(f"  {url} -> {status}")
                if status == 200:
                    achados.append((f"_db/{pasta}/{ano}.json", url))
        if achados:
            break
    return achados


SITE = "https://www.fiaformulae.com"
PAGINAS_DE_INGRESSO = ["/en/mexicocity", "/en/tickets"]


def testar_ingressos():
    print("\n##### 2) PÁGINAS DE INGRESSO DO SITE OFICIAL #####")
    for caminho in PAGINAS_DE_INGRESSO:
        status, texto = baixar(SITE + caminho)
        print(f"\n{SITE + caminho} | status {status} | {len(texto)} bytes")
        if not texto:
            continue
        sem_scripts = re.sub(r"<(script|style)\b.*?</\1>", " ", texto, flags=re.S | re.I)
        linhas = [" ".join(l.split()) for l in re.sub(r"<[^>]+>", "\n", sem_scripts).splitlines() if l.strip()]
        horarios = [
            l for l in linhas
            if re.search(r"\b\d{1,2}:\d{2}\b", l) or re.search(r"Practice|Qualifying|Race Day|Schedule", l, re.I)
        ]
        print("  linhas com horário ou 'schedule':", len(horarios))
        for linha in horarios[:15]:
            print("    ", linha[:120])


def main():
    achados = achar_arquivos()
    if not achados:
        print("\nNão achei dados da Fórmula E no repositório sportstimes/f1.")
    for nome, url in achados[-2:]:
        dados = baixar_json(url)
        resumir(dados, nome)

    testar_ingressos()


if __name__ == "__main__":
    main()