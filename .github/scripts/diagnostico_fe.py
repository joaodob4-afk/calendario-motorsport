"""Diagnóstico das páginas de programação do site oficial da Fórmula E (fiaformulae.com).

As páginas de cada etapa ficam em /en/calendar/<temporada>/r<N>-<cidade>?tab=schedule.
Só LÊ o site e imprime no log onde estão os horários (JSON da página, JSON-LD ou texto);
não altera nenhum arquivo do app.
"""
import json
import re
import urllib.error
import urllib.request
from collections import Counter

SITE = "https://www.fiaformulae.com"
CALENDARIO = SITE + "/en/calendar"
SUFIXO = "?tab=schedule&area=schedule"

# Temporada passada (existe, vem das referências da Wikipedia) e as da nova (podem não ter ainda).
CANDIDATAS = [
    "/en/calendar/2025-26/r3-miami",
    "/en/calendar/2025-26/r2-mexico-city",
    "/en/calendar/2026-27/r1-jeddah",
    "/en/calendar/2026-27/r3-mexico-city",
]

PALAVRAS = re.compile(r"Free Practice|Practice 1|Practice 2|Qualifying|Rookie", re.I)


def baixar(url):
    req = urllib.request.Request(
        url, headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.geturl(), r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, url, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return None, url, ""


def links_de_etapas(html):
    """Endereços /en/calendar/<temporada>/r<N>-<cidade> que aparecem no HTML."""
    achados = re.findall(r"/en/calendar/(\d{4}-\d{2})/(r\d+-[a-z0-9-]+)", html)
    vistos = []
    for temporada, final in achados:
        item = f"/en/calendar/{temporada}/{final}"
        if item not in vistos:
            vistos.append(item)
    return vistos


def hosts_no_html(html):
    """Servidores citados no HTML: mostra de onde a página busca os dados."""
    contagem = Counter(re.findall(r"https?://([a-z0-9.-]+)", html))
    return [(h, n) for h, n in contagem.most_common(25) if "." in h]


def contexto(html, regex, quantos=3, largura=220):
    saida = []
    for m in regex.finditer(html):
        trecho = html[max(0, m.start() - largura): m.end() + largura]
        saida.append(" ".join(trecho.split()))
        if len(saida) >= quantos:
            break
    return saida


def analisar(url):
    status, final, html = baixar(url)
    print(f"\n=== {url}\n  status {status} | final {final} | {len(html)} bytes")
    if not html:
        return

    scripts = re.findall(r"<script([^>]*)>", html)
    ids = [re.search(r'id="([^"]+)"', s).group(1) for s in scripts if re.search(r'id="([^"]+)"', s)]
    print("  scripts com id:", ids[:10], "| __NEXT_DATA__:", "__NEXT_DATA__" in html)
    tipos = sorted(set(re.findall(r'"@type"\s*:\s*"([A-Za-z]+)"', html)))
    print("  tipos JSON-LD:", tipos, "| subEvent:", "subEvent" in html)

    total = len(PALAVRAS.findall(html))
    print("  ocorrências de Practice/Qualifying/Rookie no HTML:", total)
    for c in contexto(html, PALAVRAS):
        print("   ...", c[:520])

    texto = re.sub(r"<(script|style)\b.*?</\1>", " ", html, flags=re.S | re.I)
    texto = re.sub(r"<[^>]+>", "\n", texto)
    linhas = [" ".join(l.split()) for l in texto.splitlines() if l.strip()]
    horarios = [l for l in linhas if re.search(r"\b\d{1,2}:\d{2}\b", l)]
    print("  linhas visíveis com horário:", len(horarios))
    for l in horarios[:12]:
        print("    ", l[:110])


def main():
    print("##### CALENDÁRIO #####")
    status, final, html = baixar(CALENDARIO)
    print("status", status, "| final", final, "|", len(html), "bytes")
    links = links_de_etapas(html)
    print("links de etapa encontrados:", len(links))
    for l in links[:30]:
        print("  ", l)
    print("servidores citados na página:")
    for host, n in hosts_no_html(html):
        print("  ", n, host)

    urls = [SITE + c + SUFIXO for c in CANDIDATAS]
    urls += [SITE + l + SUFIXO for l in links[:2] if SITE + l + SUFIXO not in urls]
    print("\n##### PÁGINAS DE PROGRAMAÇÃO #####")
    for url in urls:
        analisar(url)


if __name__ == "__main__":
    main()