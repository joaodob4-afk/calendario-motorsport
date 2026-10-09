import re
import urllib.request

URL = "https://www.fiaformula2.com/en/racing/2026/monza"


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept-Language": "en-US,en;q=0.9",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace")


def main():
    html = baixar(URL)
    with open("pagina_0.html", "w", encoding="utf-8") as f:
        f.write(html)

    print("##### JSON-LD #####")
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>',
        html,
        flags=re.S,
    )
    for i, b in enumerate(blocos):
        print(f"--- bloco {i} ({len(b)} chars) ---")
        print(b[:1800])

    print()
    print("##### CONTEXTO DOS DATETIMES #####")
    vistos = set()
    n = 0
    for m in re.finditer(
        r"20\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d+Z", html
    ):
        if m.group(0) in vistos:
            continue
        vistos.add(m.group(0))
        n += 1
        if n > 5:
            break
        ini = max(0, m.start() - 350)
        print(f"--- datetime {m.group(0)} ---")
        print(html[ini : m.end() + 120].replace("\n", " "))

    print()
    print("##### TITULOS DE SESSAO NA ORDEM #####")
    nomes = re.findall(
        r">((?:Practice|Qualifying|Sprint Race|Feature Race)[^<]{0,30})<",
        html,
    )
    print(nomes[:20])

    print()
    print("##### DIAS (cabecalhos de data) #####")
    dias = re.findall(
        r">((?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*,? ?\d{1,2}[^<]{0,15})<",
        html,
    )
    print(dias[:10])
    print("datetime= atributos:", re.findall(r'datetime="([^"]+)"', html)[:10])


if __name__ == "__main__":
    main()
