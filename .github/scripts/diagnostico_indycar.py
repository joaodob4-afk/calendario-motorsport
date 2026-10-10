import json
import re
import urllib.error
import urllib.request

HTML = [
    "https://www.indycar.com/Schedule",
    "https://www.indycar.com/Schedule/2027",
]

JSONS = [
    "https://site.api.espn.com/apis/site/v2/sports/racing/irl/scoreboard?dates=2027",
    "https://site.api.espn.com/apis/site/v2/sports/racing/irl/scoreboard?dates=20270307",
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


def analisar_html(url):
    global contador
    print("=" * 60)
    print(url)
    status, html = baixar(url)
    print("  status:", status, "| tamanho:", len(html))
    if not html:
        return

    contador += 1
    with open(f"indy_pagina_{contador}.html", "w", encoding="utf-8") as f:
        f.write(html)

    print("  JSON-LD:", len(re.findall(r"application/ld\+json", html)))
    print("  __NEXT_DATA__:", "__NEXT_DATA__" in html)
    print("  St. Petersburg:", len(re.findall("Petersburg", html)))

    for i, b in enumerate(
        re.findall(r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S)
    ):
        print(f"  --- JSON-LD {i} ({len(b)} chars) ---")
        print(b[:1500])

    datas = re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", html)
    print("  Datas ISO com hora:", len(datas), datas[:8])

    hrefs = list(dict.fromkeys(re.findall(r'href="([^"#]+)"', html)))
    uteis = [h for h in hrefs if re.search(r"schedule|event|race|raceweekend", h, re.I)]
    print("  Links úteis:", len(uteis))
    for h in uteis[:30]:
        print("     ", h)

    for m in list(re.finditer("Petersburg", html))[:2]:
        ini = max(0, m.start() - 200)
        print("  --- contexto ---")
        print("  ", html[ini : m.end() + 200].replace("\n", " "))


def analisar_json(url):
    print("=" * 60)
    print(url)
    status, texto = baixar(url)
    print("  status:", status, "| tamanho:", len(texto))
    if not texto:
        return
    try:
        dados = json.loads(texto)
    except ValueError:
        print("  Não é JSON. Início:", texto[:300])
        return

    print("  chaves:", list(dados.keys())[:15])
    ligas = dados.get("leagues") or []
    if ligas:
        liga = ligas[0]
        print("  liga:", liga.get("name"), "| temporada:", (liga.get("season") or {}).get("year"))
        cal = liga.get("calendar") or []
        print("  calendar:", len(cal))
        for item in cal[:25]:
            if isinstance(item, dict):
                print("     ", item.get("label"), "|", item.get("startDate"), "->", item.get("endDate"))
            else:
                print("     ", item)
    eventos = dados.get("events") or []
    print("  events:", len(eventos))
    for e in eventos[:5]:
        print("     ", e.get("name"), "|", e.get("date"))
        for comp in (e.get("competitions") or [])[:6]:
            print("        ", (comp.get("type") or {}).get("text") or comp.get("name"), "|", comp.get("date"))


def main():
    for u in HTML:
        analisar_html(u)
    for u in JSONS:
        analisar_json(u)


if __name__ == "__main__":
    main()