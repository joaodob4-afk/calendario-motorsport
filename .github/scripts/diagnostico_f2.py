import re
import urllib.request

URLS = [
    "https://www.fiaformula2.com/en/racing/2026/monza",
    "https://www.fiaformula2.com/en/racing/2026/baku",
]


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
    for i, url in enumerate(URLS):
        print("=" * 60)
        print(url)
        try:
            html = baixar(url)
        except Exception as e:
            print("ERRO ao baixar:", e)
            continue

        with open(f"pagina_{i}.html", "w", encoding="utf-8") as f:
            f.write(html)

        print("Tamanho do HTML:", len(html))
        print("JSON-LD:", len(re.findall(r"application/ld\+json", html)))
        print("__NEXT_DATA__:", "__NEXT_DATA__" in html)
        print("Menciona Sprint:", len(re.findall(r"Sprint", html)))
        print("Menciona Feature:", len(re.findall(r"Feature", html)))
        print("Menciona Qualifying:", len(re.findall(r"Qualifying", html)))
        horas = re.findall(r"\b\d{1,2}:\d{2}\b", html)
        print("Horários HH:MM encontrados:", len(horas), horas[:12])
        datas = re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", html)
        print("Datas ISO com hora:", len(datas), datas[:8])

        for m in list(re.finditer(r"Sprint", html))[:2]:
            ini = max(0, m.start() - 150)
            print("--- trecho ---")
            print(html[ini : m.end() + 250].replace("\n", " "))


if __name__ == "__main__":
    main()
