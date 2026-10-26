"""Diagnóstico do site da F1 Academy (só lê e imprime; não altera nenhum arquivo do app).

Imprime, para cada etapa da temporada, todas as sessões que o site informa
(nome, sigla, tipo, início, fim, se não está confirmada). Serve para descobrir nomes de
sessão novos e sessões repetidas.
"""
import json
import re
import urllib.request

BASE = "https://www.f1academy.com"
CALENDARIO = BASE + "/Racing-Series/Calendar"


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace")


def sessoes_da_pagina(html):
    """(título, lista de sessões) de uma página de etapa."""
    titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
    titulo = titulo.group(1).strip() if titulo else "?"
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, flags=re.S)
    if not m:
        return titulo, None
    page = json.loads(m.group(1))["props"]["pageProps"]["pageData"]
    return titulo, page.get("SessionResults") or []


def main():
    print("Baixando calendário:", CALENDARIO)
    ids = sorted({int(i) for i in re.findall(r"raceid=(\d+)", baixar(CALENDARIO))})
    print("raceids:", ids)
    for raceid in ids:
        print("=" * 60)
        try:
            titulo, sessoes = sessoes_da_pagina(
                baixar(f"{BASE}/Racing-Series/Results?raceid={raceid}")
            )
        except Exception as e:  # noqa: BLE001
            print(raceid, "ERRO:", e)
            continue
        print(raceid, "|", titulo)
        if sessoes is None:
            print("  sem __NEXT_DATA__")
            continue
        for s in sessoes:
            print(
                "  ",
                f"{s.get('SessionName')!r:28}",
                f"sigla={s.get('SessionShortName')!r:6}",
                f"tipo={s.get('SessionType')!r:12}",
                "início=", s.get("SessionStartTime"),
                "fim=", s.get("SessionEndTime"),
                "NAO_CONFIRMADA" if s.get("Unconfirmed") else "confirmada",
            )


if __name__ == "__main__":
    main()