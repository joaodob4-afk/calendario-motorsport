import re
import urllib.error
import urllib.request

BASE = "https://www.indycar.com"
LISTA = BASE + "/Schedule"
EVENTOS = [
    BASE + "/Schedule/2026/St-Petersburg",
    BASE + "/Schedule/2026/Indianapolis-500",
]


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


def texto_limpo(html):
    html = re.sub(r"<script[\s\S]*?</script>|<style[\s\S]*?</style>|<svg[\s\S]*?</svg>", "", html)
    t = re.sub(r"<[^>]+>", "\n", html)
    linhas = [re.sub(r"\s+", " ", l).strip() for l in t.split("\n")]
    return "\n".join(l for l in linhas if l)


def compacto(s):
    return re.sub(r"\s+", " ", s)


def main():
    status, html = baixar(LISTA)
    print("LISTA status:", status, "| tamanho:", len(html))

    print()
    print("##### PRIMEIRO CARTÃO (HTML cru, compactado) #####")
    cartoes = re.findall(
        r'<div class="event-card-container">([\s\S]*?)(?=<div class="event-card-container">|$)',
        html,
    )
    print("cartões encontrados:", len(cartoes))
    if cartoes:
        print(compacto(cartoes[0])[:2500])

    print()
    print("##### TEXTO DOS 3 PRIMEIROS CARTÕES #####")
    for c in cartoes[:3]:
        print("---")
        print(texto_limpo(c)[:600])

    for url in EVENTOS:
        print()
        print("=" * 60)
        print(url)
        st, h = baixar(url)
        print("  status:", st, "| tamanho:", len(h))
        if not h:
            continue

        t = texto_limpo(h)
        i = t.find("Practice")
        if i < 0:
            i = t.find("Qualif")
        print("  ##### TEXTO em volta de 'Practice' #####")
        print(t[max(0, i - 300) : i + 2200])

        if url.endswith("St-Petersburg"):
            m = re.search(r"Practice", h)
            if m:
                print()
                print("  ##### HTML cru em volta de 'Practice' #####")
                print(compacto(h[max(0, m.start() - 600) : m.end() + 1200]))


if __name__ == "__main__":
    main()