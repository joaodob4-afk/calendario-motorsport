"""Diagnóstico das sessões da Fórmula E (só lê e imprime; não altera nada).

Rodadas anteriores: o /en/calendar só tem datas e a API pulselive não existe mais.
Esta rodada abre as páginas de cada corrida (results-and-standings?season=N&round=R-cidade),
que o site usa para resultados, e procura nelas a programação com horários.
A temporada 12 é a 2025/26 (já terminou, serve de modelo); a 13 é a 2026/27.
"""
import json
import re
from pathlib import Path
import urllib.error
import urllib.request

BASE = "https://www.fiaformulae.com/en/results-and-standings"
URLS = [
    f"{BASE}?season=12&round=17-london",   # existe (veio do site): corrida já disputada
    f"{BASE}?season=12&round=12-shanghai",
    f"{BASE}?season=13&round=1-jeddah",    # palpite: 1ª corrida de 2026/27
    f"{BASE}?season=13&round=2-jeddah",
    f"{BASE}?season=13",
]
ISO = re.compile(r"20\d\d-\d\d-\d\dT\d\d:\d\d[:\d.]*(?:Z|[+-]\d\d:?\d\d)?")
PALAVRAS = ["Free Practice", "Practice", "Qualifying", "sessionName", "startTime", "schedule", "Schedule"]


def baixar(url):
    req = urllib.request.Request(
        url, headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return None, ""


def texto_do_next(html):
    """Junta os pedaços self.__next_f.push([1,"..."]) em um texto só (já sem escapes)."""
    partes = []
    for bruto in re.findall(r'self\.__next_f\.push\(\[1,("(?:[^"\\]|\\.)*")\]\)', html):
        try:
            partes.append(json.loads(bruto))
        except ValueError:
            pass
    return "".join(partes)


def contextos(texto, palavra, quantos=3, raio=170):
    posicoes = [m.start() for m in re.finditer(re.escape(palavra), texto)]
    print(f"   '{palavra}': {len(posicoes)} ocorrências")
    for p in posicoes[:quantos]:
        print("      ...", texto[max(0, p - raio): p + raio].replace("\n", " "))


def analisar(url, indice):
    print("=" * 60)
    print("Página:", url)
    status, html = baixar(url)
    print("  status:", status, "| tamanho:", len(html))
    if status != 200 or not html:
        return

    arquivo = Path(f"fe_pagina_{indice}.html")
    arquivo.write_text(html, encoding="utf-8")
    print("  HTML salvo:", arquivo)

    titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
    print("  título:", titulo.group(1).strip() if titulo else "?")
    print("  __NEXT_DATA__:", "__NEXT_DATA__" in html, "| JSON-LD:", html.count("application/ld+json"))

    texto = texto_do_next(html)
    print("  texto do Next:", len(texto), "caracteres")
    base = texto or html
    for palavra in PALAVRAS[:5]:
        contextos(base, palavra, quantos=2)
    print("  datas ISO:", ISO.findall(base)[:12])


def main():
    for indice, url in enumerate(URLS, start=1):
        analisar(url, indice)


if __name__ == "__main__":
    main()