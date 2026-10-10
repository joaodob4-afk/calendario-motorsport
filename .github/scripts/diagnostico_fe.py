import json
import re
import urllib.error
import urllib.request

BASE = "https://www.fiaformulae.com"

CANDIDATAS = [
    BASE + "/en/calendar",
    BASE + "/en/racing/calendar",
    BASE + "/en/race-calendar",
    BASE + "/en/calendar/season-13",
]

contador = 0


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        print("  ERRO:", e)
        return None, ""


def analisar(url):
    global contador
    print("=" * 60)
    print(url)
    status, html = baixar(url)
    print("  status:", status, "| tamanho:", len(html))
    if not html:
        return []

    contador += 1
    with open(f"fe_pagina_{contador}.html", "w", encoding="utf-8") as f:
        f.write(html)

    print("  JSON-LD:", len(re.findall(r"application/ld\+json", html)))
    print("  __NEXT_DATA__:", "__NEXT_DATA__" in html)
    print("  Jeddah:", len(re.findall("Jeddah", html)))

    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )
    for b in blocos:
        try:
            dados = json.loads(b)
        except ValueError:
            continue
        itens = dados if isinstance(dados, list) else [dados]
        for d in itens:
            if not isinstance(d, dict):
                continue
            print("  JSON-LD tipo:", d.get("@type"), "|", d.get("name"))
            for s in (d.get("subEvent") or [])[:12]:
                print("     ", s.get("name"), "|", s.get("startDate"), "->", s.get("endDate"))

    datas = re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", html)
    print("  Datas ISO com hora:", len(datas), datas[:6])

    hrefs = re.findall(r'href="([^"#]+)"', html)
    uteis = []
    for h in hrefs:
        if re.search(r"e-prix|/racing/|/calendar/|/race/|jeddah|season", h, re.I):
            if h not in uteis:
                uteis.append(h)
    print("  Links possivelmente úteis:", len(uteis))
    for h in uteis[:40]:
        print("     ", h)
    return uteis


def main():
    todos = []
    for url in CANDIDATAS:
        todos += analisar(url)

    # Tenta abrir a página de um evento (Jeddah) se algum link apareceu.
    jeddah = [h for h in todos if "jeddah" in h.lower()]
    for h in list(dict.fromkeys(jeddah))[:2]:
        url = h if h.startswith("http") else BASE + h
        analisar(url)


if __name__ == "__main__":
    main()