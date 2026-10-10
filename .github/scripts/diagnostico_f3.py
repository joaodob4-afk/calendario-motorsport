import json
import re
import urllib.request
from html.parser import HTMLParser

BASE = "https://www.fiaformula3.com"
LISTA = BASE + "/en/racing/2026"


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        print("  status:", r.status)
        return r.read().decode("utf-8", errors="replace")


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.slugs = set()

    def handle_starttag(self, tag, attrs):
        if tag.lower() != "a":
            return
        for nome, valor in attrs:
            if nome.lower() == "href" and valor:
                m = re.search(r"/en/racing/2026/([^/?#]+)", valor)
                if m:
                    self.slugs.add(m.group(1))


def analisar(slug, html, indice):
    with open(f"f3_pagina_{indice}.html", "w", encoding="utf-8") as f:
        f.write(html)

    print("=" * 60)
    print(slug, "| tamanho:", len(html))
    print("JSON-LD:", len(re.findall(r"application/ld\+json", html)))
    print("__NEXT_DATA__:", "__NEXT_DATA__" in html)

    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )
    achou = False
    for b in blocos:
        try:
            dados = json.loads(b)
        except ValueError:
            continue
        if isinstance(dados, dict) and dados.get("subEvent"):
            achou = True
            print("Evento:", dados.get("name"))
            for s in dados["subEvent"]:
                print("  ", s.get("name"), "|", s.get("startDate"), "->", s.get("endDate"))
    if not achou:
        print("Sem subEvent no JSON-LD.")
        print("Datetimes ISO:", re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", html)[:10])
        print("Sprint:", len(re.findall("Sprint", html)), "| Feature:", len(re.findall("Feature", html)))


def main():
    print("Baixando lista:", LISTA)
    try:
        html = baixar(LISTA)
    except Exception as e:
        print("ERRO ao baixar a lista:", e)
        return

    p = Links()
    p.feed(html)
    slugs = sorted(p.slugs)
    print("Etapas encontradas:", len(slugs))
    for s in slugs:
        print("  ", s)

    if not slugs:
        with open("f3_lista.html", "w", encoding="utf-8") as f:
            f.write(html)
        print("Nenhum link. Primeiros 600 chars:")
        print(html[:600])
        return

    # Testa a primeira, uma do meio e a última etapa.
    amostra = [slugs[0], slugs[len(slugs) // 2], slugs[-1]]
    for i, slug in enumerate(dict.fromkeys(amostra)):
        url = f"{BASE}/en/racing/2026/{slug}"
        print()
        print("Baixando:", url)
        try:
            analisar(slug, baixar(url), i)
        except Exception as e:
            print("ERRO:", e)


if __name__ == "__main__":
    main()