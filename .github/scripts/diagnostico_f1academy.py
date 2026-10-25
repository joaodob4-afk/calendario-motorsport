"""Diagnóstico do site da F1 Academy (só lê e imprime; não altera nenhum arquivo do app).

Mostra como o site publica os horários das sessões: JSON-LD (subEvent), __NEXT_DATA__
ou datas soltas no HTML. Com essa saída dá para escrever o atualizador de verdade.
"""
import json
import re
import urllib.request

BASE = "https://www.f1academy.com"
CALENDARIO = BASE + "/Racing-Series/Calendar"
# Ids conhecidos: 2025 = 15..21; 2026 deve continuar a partir de 22 (Las Vegas 2026 = 28).
IDS_RESERVA = [23, 25, 28]


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        print("  status:", r.status)
        return r.read().decode("utf-8", errors="replace")


def achar_ids(html):
    return sorted({int(i) for i in re.findall(r"raceid=(\d+)", html)})


def mostrar_json_ld(html):
    blocos = re.findall(
        r"<script[^>]*application/ld\+json[^>]*>(.*?)</script>", html, flags=re.S
    )
    print("JSON-LD blocos:", len(blocos))
    achou = False
    for b in blocos:
        try:
            dados = json.loads(b)
        except ValueError:
            continue
        itens = dados if isinstance(dados, list) else [dados]
        for item in itens:
            if isinstance(item, dict) and item.get("subEvent"):
                achou = True
                print("  Evento:", item.get("name"), "|", item.get("startDate"))
                for s in item["subEvent"]:
                    print("    ", s.get("name"), "|", s.get("startDate"), "->", s.get("endDate"))
    if not achou:
        print("  Sem subEvent no JSON-LD.")


def mostrar_next_data(html):
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, flags=re.S)
    print("__NEXT_DATA__:", bool(m))
    if not m:
        return
    try:
        dados = json.loads(m.group(1))
    except ValueError:
        print("  JSON inválido")
        return
    props = dados.get("props", {}).get("pageProps", {})
    print("  chaves de pageProps:", list(props.keys())[:15])
    texto = json.dumps(props)
    for palavra in ("Free Practice", "Qualifying", "Race 1", "Race 2", "Feature", "Reverse"):
        print("  ", palavra, "->", texto.count(palavra), "ocorrências")


ISO = re.compile(r"20\d\d-\d\d-\d\dT\d\d:\d\d")


def objetos_com_data(no, caminho, saida):
    """Coleta (caminho, objeto) de todo dict que tenha um valor com data ISO."""
    if isinstance(no, dict):
        if any(isinstance(v, str) and ISO.match(v) for v in no.values()):
            saida.append((caminho, no))
        for k, v in no.items():
            objetos_com_data(v, f"{caminho}.{k}", saida)
    elif isinstance(no, list):
        for i, v in enumerate(no):
            objetos_com_data(v, f"{caminho}[{i}]", saida)


def mostrar_objetos_com_data(html):
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, flags=re.S)
    if not m:
        return
    dados = json.loads(m.group(1))
    page = dados.get("props", {}).get("pageProps", {}).get("pageData", {})
    print("pageData tipo:", type(page).__name__, "| chaves:", list(page.keys())[:20] if isinstance(page, dict) else "-")
    achados = []
    objetos_com_data(page, "pageData", achados)
    print("Objetos com data ISO:", len(achados))
    for caminho, obj in achados[:24]:
        print("  ", caminho)
        print("     ", json.dumps(obj, ensure_ascii=False)[:420])


def analisar(raceid):
    url = f"{BASE}/Racing-Series/Results?raceid={raceid}"
    print("=" * 60)
    print("Baixando:", url)
    try:
        html = baixar(url)
    except Exception as e:  # noqa: BLE001
        print("ERRO:", e)
        return
    with open(f"f1academy_pagina_{raceid}.html", "w", encoding="utf-8") as f:
        f.write(html)
    titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
    print("Título:", titulo.group(1).strip() if titulo else "?", "| tamanho:", len(html))
    mostrar_json_ld(html)
    mostrar_next_data(html)
    mostrar_objetos_com_data(html)
    print("Datetimes ISO:", re.findall(r"20\d\d-\d\d-\d\dT\d\d:\d\d[^\"'<\s]*", html)[:12])


def main():
    print("Baixando calendário:", CALENDARIO)
    ids = []
    try:
        html = baixar(CALENDARIO)
        with open("f1academy_calendario.html", "w", encoding="utf-8") as f:
            f.write(html)
        titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
        print("Título:", titulo.group(1).strip() if titulo else "?")
        ids = achar_ids(html)
        print("raceids encontrados:", ids)
    except Exception as e:  # noqa: BLE001
        print("ERRO ao baixar o calendário:", e)
    amostra = [i for i in (22, 28) if not ids or i in ids] or IDS_RESERVA
    for raceid in dict.fromkeys(amostra):
        analisar(raceid)


if __name__ == "__main__":
    main()