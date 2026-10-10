import json
import re
import urllib.error
import urllib.request

BASE = "https://www.fiaformulae.com"
CALENDARIO = BASE + "/en/calendar"


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


def blocos_jsonld(html):
    return re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )


def achar_urls(obj, saida):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ("url", "@id") and isinstance(v, str) and v.startswith("http"):
                saida.append(v)
            else:
                achar_urls(v, saida)
    elif isinstance(obj, list):
        for v in obj:
            achar_urls(v, saida)


def main():
    status, html = baixar(CALENDARIO)
    print("status:", status, "| tamanho:", len(html))
    with open("fe_calendario.html", "w", encoding="utf-8") as f:
        f.write(html)

    print()
    print("##### JSON-LD (primeiros 2500 chars de cada) #####")
    urls = []
    for i, b in enumerate(blocos_jsonld(html)):
        print(f"--- bloco {i} ({len(b)} chars) ---")
        print(b[:2500])
        try:
            achar_urls(json.loads(b), urls)
        except ValueError:
            pass

    print()
    print("##### TODOS OS HREFS (distintos, primeiros 60) #####")
    hrefs = list(dict.fromkeys(re.findall(r'href="([^"#]+)"', html)))
    print("total distintos:", len(hrefs))
    for h in hrefs[:60]:
        print("  ", h)

    print()
    print("##### CONTEXTO DE 'Jeddah' #####")
    for m in list(re.finditer("Jeddah", html))[:3]:
        ini = max(0, m.start() - 250)
        print("---")
        print(html[ini : m.end() + 250].replace("\n", " "))

    print()
    print("##### PAGINAS DE EVENTO #####")
    candidatas = [u for u in dict.fromkeys(urls) if "jeddah" in u.lower()][:1]
    if not candidatas:
        candidatas = [BASE + h if h.startswith("/") else h for h in hrefs if "jeddah" in h.lower()][:1]
    for u in candidatas:
        print("Abrindo:", u)
        st, h = baixar(u)
        print("  status:", st, "| tamanho:", len(h))
        if not h:
            continue
        with open("fe_evento.html", "w", encoding="utf-8") as f:
            f.write(h)
        for i, b in enumerate(blocos_jsonld(h)):
            print(f"  --- JSON-LD {i} ({len(b)} chars) ---")
            print(b[:2500])
        print("  Datas ISO com hora:", re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", h)[:10])


if __name__ == "__main__":
    main()