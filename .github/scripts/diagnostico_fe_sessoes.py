"""Diagnóstico das sessões da Fórmula E (só lê e imprime; não altera nada).

1ª rodada: a lista do /en/calendar não traz o link das corridas e a página não tem __NEXT_DATA__.
2ª rodada (este arquivo): descobre de onde o site tira os dados.
  A) olha o HTML do calendário: links, endereços de API e trechos com "Free Practice"/"Qualifying";
  B) testa a API pública que o site da Fórmula E já usou (api.formula-e.pulselive.com), que
     costuma ter as sessões de cada corrida com horário.
"""
import json
import re
import urllib.error
import urllib.request

BASE = "https://www.fiaformulae.com"
CALENDARIO = BASE + "/en/calendar"
PULSE = "https://api.formula-e.pulselive.com/formula-e/v1"
CABECALHOS = {
    "User-Agent": "Mozilla/5.0",
    "Accept": "application/json, text/html;q=0.9",
    "Accept-Language": "en-US,en;q=0.9",
    "Origin": BASE,
    "Referer": BASE + "/",
}


def baixar(url):
    req = urllib.request.Request(url, headers=CABECALHOS)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", url, "->", e)
        return None, ""


def contextos(html, palavra, quantos=4, raio=140):
    achados = [m.start() for m in re.finditer(palavra, html)]
    print(f"  '{palavra}': {len(achados)} ocorrências")
    for pos in achados[:quantos]:
        trecho = html[max(0, pos - raio): pos + raio].replace("\n", " ")
        print("     ...", trecho)


def parte_a():
    print("#" * 20, "A) HTML do calendário")
    status, html = baixar(CALENDARIO)
    print("status:", status, "| tamanho:", len(html))

    links = sorted(set(re.findall(r'href="([^"#]+)"', html)))
    uteis = [l for l in links if re.search(r"e-prix|race|calendar|schedule|weekend|round", l, re.I)]
    print("links úteis:", len(uteis))
    for l in uteis[:40]:
        print("   ", l)

    apis = sorted(set(re.findall(r'https?://[^\s"\'\\<>]*(?:api|pulselive|graphql|cdn|data)[^\s"\'\\<>]*', html)))
    print("endereços com api/pulselive/graphql/data:", len(apis))
    for a in apis[:25]:
        print("   ", a[:160])

    print("scripts:", [s[:90] for s in re.findall(r'<script[^>]+src="([^"]+)"', html)[:8]])
    print("tem self.__next_f (Next app router):", "__next_f" in html)
    for palavra in ("Free Practice", "Qualifying", "FREE PRACTICE"):
        contextos(html, palavra)


def parte_b():
    print("#" * 20, "B) API Pulselive da Fórmula E")
    status, texto = baixar(PULSE + "/championships")
    print("championships -> status", status, "| tamanho", len(texto))
    if status != 200 or not texto:
        return
    try:
        dados = json.loads(texto)
    except ValueError:
        print("  não é JSON:", texto[:200])
        return
    campeonatos = dados.get("championships") if isinstance(dados, dict) else dados
    if not isinstance(campeonatos, list):
        print("  formato inesperado:", str(dados)[:300])
        return
    print("  total:", len(campeonatos))
    for c in campeonatos[-5:]:
        print("   ", {k: c.get(k) for k in ("id", "name", "status", "startDate", "endDate") if k in c})

    for c in campeonatos[-2:]:
        cid = c.get("id")
        status, texto = baixar(f"{PULSE}/races?championshipId={cid}")
        print(f"races?championshipId={cid} -> status {status} | tamanho {len(texto)}")
        if status != 200 or not texto:
            continue
        try:
            dados = json.loads(texto)
        except ValueError:
            continue
        corridas = dados.get("races") if isinstance(dados, dict) else dados
        if not isinstance(corridas, list) or not corridas:
            print("  sem corridas:", str(dados)[:200])
            continue
        print("  corridas:", len(corridas), "| campos:", sorted(corridas[0].keys()))
        for corrida in corridas[:2] + corridas[-1:]:
            print("   corrida:", {k: corrida.get(k) for k in ("name", "city", "date", "sessions") if k in corrida})
            sessoes = corrida.get("sessions")
            if isinstance(sessoes, list):
                for s in sessoes:
                    print("      ", json.dumps(s, ensure_ascii=False)[:220])


def main():
    parte_a()
    parte_b()


if __name__ == "__main__":
    main()