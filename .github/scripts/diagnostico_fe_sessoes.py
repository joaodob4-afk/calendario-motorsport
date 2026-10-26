"""Diagnóstico das sessões da Fórmula E no site oficial (só lê e imprime; não altera nada).

A página /en/calendar já é lida pelo atualizar_fe.py, mas só traz as datas das corridas.
Este script abre também as páginas de cada corrida (links que a própria lista informa) e
mostra onde aparecem os horários de treino, classificação e corrida: JSON-LD, __NEXT_DATA__
ou datas soltas no HTML.
"""
import json
import re
import urllib.request

BASE = "https://www.fiaformulae.com"
CALENDARIO = BASE + "/en/calendar"
ISO = re.compile(r"20\d\d-\d\d-\d\dT\d\d:\d\d")
PALAVRAS = ("Free Practice", "Qualifying", "Race", "Practice", "Session")


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.status, r.read().decode("utf-8", errors="replace")


def blocos_json_ld(html):
    saida = []
    for b in re.findall(r"<script[^>]*application/ld\+json[^>]*>(.*?)</script>", html, flags=re.S):
        try:
            saida.append(json.loads(b))
        except ValueError:
            pass
    return saida


def achar(no, caminho, saida, criterio):
    if isinstance(no, dict):
        if criterio(no):
            saida.append((caminho, no))
        for k, v in no.items():
            achar(v, f"{caminho}.{k}", saida, criterio)
    elif isinstance(no, list):
        for i, v in enumerate(no):
            achar(v, f"{caminho}[{i}]", saida, criterio)


def tem_data_e_nome(d):
    tem_data = any(isinstance(v, str) and ISO.match(v) for v in d.values())
    tem_nome = any(
        isinstance(v, str) and any(p.lower() in v.lower() for p in PALAVRAS) for v in d.values()
    )
    return tem_data and tem_nome


def analisar_pagina(url):
    print("=" * 60)
    print("Página:", url)
    try:
        status, html = baixar(url)
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return
    titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
    print("  status:", status, "| tamanho:", len(html), "| título:", titulo.group(1).strip() if titulo else "?")

    ld = blocos_json_ld(html)
    print("  JSON-LD blocos:", len(ld))
    achados = []
    for i, bloco in enumerate(ld):
        achar(bloco, f"ld[{i}]", achados, tem_data_e_nome)
    for caminho, obj in achados[:12]:
        print("   ", caminho, json.dumps(obj, ensure_ascii=False)[:300])

    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, flags=re.S)
    print("  __NEXT_DATA__:", bool(m))
    if m:
        try:
            dados = json.loads(m.group(1))
            props = dados.get("props", {}).get("pageProps", {})
            print("    chaves de pageProps:", list(props.keys())[:15])
            achados = []
            achar(props, "pageProps", achados, tem_data_e_nome)
            print("    objetos com nome e data:", len(achados))
            for caminho, obj in achados[:12]:
                print("     ", caminho, json.dumps(obj, ensure_ascii=False)[:300])
        except ValueError:
            print("    JSON inválido")

    print("  datas ISO soltas:", ISO.findall(html)[:8])
    nome_arquivo = re.sub(r"[^a-z0-9]+", "_", url.lower())[-60:]
    with open(f"fe_pagina_{nome_arquivo}.html", "w", encoding="utf-8") as f:
        f.write(html)


def main():
    print("Baixando:", CALENDARIO)
    status, html = baixar(CALENDARIO)
    print("status:", status)
    itens = []
    for bloco in blocos_json_ld(html):
        achados = []
        achar(bloco, "ld", achados, lambda d: isinstance(d.get("itemListElement"), list))
        for _, lista in achados:
            itens = lista["itemListElement"]
            break
        if itens:
            break
    print("corridas na lista:", len(itens))

    urls = []
    for el in itens:
        item = el.get("item") if isinstance(el, dict) else None
        if not isinstance(item, dict):
            continue
        print("  ", item.get("name"), "|", item.get("startDate"), "|", item.get("url"), "| chaves:", sorted(item.keys()))
        if item.get("url"):
            urls.append(item["url"])

    # Links de corridas que aparecem no HTML da página, caso a lista não traga a url.
    if not urls:
        urls = sorted(set(re.findall(r'href="(/en/(?:calendar|race|races|e-prix)[^"#?]*)"', html)))[:6]
        urls = [BASE + u for u in urls]
        print("links encontrados no HTML:", urls)

    for url in urls[:3] + urls[-1:]:
        analisar_pagina(url if url.startswith("http") else BASE + url)


if __name__ == "__main__":
    main()