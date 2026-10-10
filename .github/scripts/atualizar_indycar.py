"""Atualiza app/src/main/assets/indycar_calendar.json a partir do site oficial
da IndyCar (indycar.com/Schedule + pagina de cada etapa).

Horarios do site estao em ET (America/New_York); o JSON sai em horario de
Brasilia. Se a validacao falhar, NAO altera o arquivo e sai com erro.
"""
import html as htmllib
import json
import re
import sys
import time
import urllib.request
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

BASE = "https://www.indycar.com"
LISTA = BASE + "/Schedule"
SAIDA = Path("app/src/main/assets/indycar_calendar.json")

NY = ZoneInfo("America/New_York")
BRASILIA = ZoneInfo("America/Sao_Paulo")

MESES = {
    m: i
    for i, m in enumerate(
        ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
        start=1,
    )
}

PROVINCIAS_CANADA = {"Ontario", "Quebec", "British Columbia", "Alberta", "Manitoba"}
PREFIXO = "NTT INDYCAR SERIES - "


def baixar(url):
    ultimo = None
    for tentativa in range(3):
        try:
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
            )
            with urllib.request.urlopen(req, timeout=30) as r:
                return r.read().decode("utf-8", errors="replace")
        except Exception as e:  # noqa: BLE001
            ultimo = e
            time.sleep(2 * (tentativa + 1))
    raise ultimo


def linhas_de_texto(trecho):
    trecho = re.sub(
        r"<script[\s\S]*?</script>|<style[\s\S]*?</style>|<svg[\s\S]*?</svg>", "", trecho
    )
    t = re.sub(r"<[^>]+>", "\n", trecho)
    t = htmllib.unescape(t).replace("\xa0", " ")
    linhas = [re.sub(r"\s+", " ", l).strip() for l in t.split("\n")]
    return [l for l in linhas if l]


def extrair_cartoes(html):
    """Retorna {slug: dados_do_cartao} da temporada mais recente da lista."""
    padrao = re.compile(r'<a href="(/Schedule/(\d{4})/([^"/?#]+))"[^>]*class="event-card-link"')
    achados = list(padrao.finditer(html))
    if not achados:
        raise ValueError("Nenhum cartão de etapa encontrado")

    ano = max(int(m.group(2)) for m in achados)
    cartoes = {}

    for i, m in enumerate(achados):
        if int(m.group(2)) != ano:
            continue
        slug = m.group(3)
        if slug in cartoes:
            continue

        fim = achados[i + 1].start() if i + 1 < len(achados) else m.end() + 8000
        bloco = html[m.start() : min(fim, m.start() + 12000)]

        def pegar(classe):
            r = re.search(r'class="' + classe + r'"[^>]*>(.*?)</', bloco, re.S)
            return htmllib.unescape(r.group(1)).strip() if r else ""

        linhas = linhas_de_texto(bloco)
        data = hora = None
        for l in linhas:
            mm = re.fullmatch(r"([A-Z][a-z]{2}) (\d{1,2})", l)
            if mm and mm.group(1) in MESES and data is None:
                data = (MESES[mm.group(1)], int(mm.group(2)))
            mh = re.fullmatch(r"(\d{1,2}):(\d{2}) ?([AP]M) ET", l)
            if mh and hora is None:
                hora = (int(mh.group(1)), int(mh.group(2)), mh.group(3))

        cartoes[slug] = {
            "slug": slug,
            "ano": ano,
            "titulo": pegar("event-card-title"),
            "pista": pegar("event-card-track-name"),
            "local": pegar("event-card-track-location"),
            "data": data,
            "hora": hora,
        }
    return cartoes


def converter_hora(ano, mes, dia, h, m, ampm):
    h = h % 12 + (12 if ampm.upper() == "PM" else 0)
    return datetime(ano, mes, dia, h, m, tzinfo=NY)


def traduzir(nome):
    n = nome.strip()
    if n.lower() == "pre-race":
        return None
    m = re.fullmatch(r"Practice (\d+)", n)
    if m:
        return f"Treino Livre {m.group(1)}"
    if n in ("Qualifications", "Qualification"):
        return "Classificação"
    if n.startswith("Qualifying"):
        return "Classificação" + n[len("Qualifying"):]
    if n == "Race":
        return "Corrida"
    if n == "Warmup":
        return "Warm-up"
    if n == "Carb Day Final Practice":
        return "Treino Final (Carb Day)"
    if n == "Pit Stop Competition":
        return "Competição de Pit Stop"
    return n


def extrair_sessoes(html, ano):
    linhas = linhas_de_texto(html)

    ini = next((i for i, l in enumerate(linhas) if l == "Download Weekend Schedule"), None)
    if ini is None:
        return []
    fim = next(
        (i for i, l in enumerate(linhas[ini:], start=ini) if l in ("Find Local Radio Station", "Event Details")),
        len(linhas),
    )

    sessoes = []
    dia = None
    i = ini + 1
    while i < fim:
        l = linhas[i]
        md = re.fullmatch(r"[A-Za-z]+, ([A-Z][a-z]{2}) (\d{1,2})", l)
        if md and md.group(1) in MESES:
            dia = (MESES[md.group(1)], int(md.group(2)))
            i += 1
            continue

        mh = re.fullmatch(r"(\d{1,2}):(\d{2}) ?([AP]M) ET", l)
        if mh and dia and i + 1 < fim:
            desc = linhas[i + 1]
            if desc.startswith(PREFIXO):
                nome = traduzir(desc[len(PREFIXO):])
                if nome:
                    dt = converter_hora(ano, dia[0], dia[1], int(mh.group(1)), int(mh.group(2)), mh.group(3))
                    sessoes.append((nome, dt, dia))
            i += 2
            continue
        i += 1

    # Remove repetições (mesmo dia e mesmo nome): fica a mais cedo.
    vistos = set()
    unicas = []
    for nome, dt, dia in sorted(sessoes, key=lambda s: s[1]):
        chave = (dia, nome)
        if chave in vistos:
            continue
        vistos.add(chave)
        unicas.append((nome, dt, dia))
    return unicas


def fmt(d):
    return d.strftime("%d/%m/%Y")


def montar_etapa(cartao, html_evento):
    ano = cartao["ano"]
    sessoes = extrair_sessoes(html_evento, ano)

    if sessoes:
        dias = [datetime(ano, d[0], d[1]) for _, _, d in sessoes]
        inicio, fim = min(dias), max(dias)
        lista = [
            {
                "nome": nome,
                "data": fmt(dt.astimezone(BRASILIA)),
                "horario": dt.astimezone(BRASILIA).strftime("%H:%M"),
            }
            for nome, dt, _ in sessoes
        ]
    else:
        # Programação ainda não publicada: usa a data da corrida do cartão.
        if not cartao["data"]:
            raise ValueError(f"{cartao['slug']}: sem programação e sem data no cartão")
        mes, dia = cartao["data"]
        inicio = fim = datetime(ano, mes, dia)
        if cartao["hora"]:
            h, m, ap = cartao["hora"]
            dt = converter_hora(ano, mes, dia, h, m, ap).astimezone(BRASILIA)
            lista = [{"nome": "Corrida", "data": fmt(dt), "horario": dt.strftime("%H:%M")}]
        else:
            lista = [{"nome": "Corrida", "data": fmt(inicio), "horario": "A confirmar"}]

    local = cartao["local"]
    regiao = local.split(",")[-1].strip() if "," in local else ""
    pais = "Canadá" if regiao in PROVINCIAS_CANADA else "Estados Unidos"

    if not cartao["titulo"]:
        raise ValueError(f"{cartao['slug']}: título ausente")

    return {
        "_ordem": inicio,
        "nome": cartao["titulo"],
        "circuito": cartao["pista"] or cartao["titulo"],
        "local": local,
        "pais": pais,
        "inicio": fmt(inicio),
        "fim": fmt(fim),
        "sessoes": lista,
    }


def main():
    print("Consultando páginas oficiais da IndyCar...")
    try:
        cartoes = extrair_cartoes(baixar(LISTA))
    except Exception as e:  # noqa: BLE001
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    print("Etapas encontradas:", len(cartoes))
    if len(cartoes) < 5:
        print("ERRO: poucas etapas. Nada foi alterado.")
        return 1

    etapas = []
    for slug, cartao in cartoes.items():
        try:
            html_evento = baixar(f"{BASE}/Schedule/{cartao['ano']}/{slug}")
            etapas.append(montar_etapa(cartao, html_evento))
            print("OK  ", slug)
        except Exception as e:  # noqa: BLE001
            print("ERRO", slug, "->", e)
            print("Nada foi alterado.")
            return 1

    etapas.sort(key=lambda e: e["_ordem"])
    saida = []
    for n, e in enumerate(etapas, start=1):
        e.pop("_ordem")
        e["etapa"] = n
        saida.append(
            {k: e[k] for k in ("etapa", "nome", "circuito", "local", "pais", "inicio", "fim", "sessoes")}
        )

    ano = next(iter(cartoes.values()))["ano"]
    novo = {"temporada": ano, "categoria": "IndyCar", "etapas": saida}
    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())