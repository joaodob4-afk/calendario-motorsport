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
MINIMO_ETAPAS_VALIDAS = 1


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

        # No site, a data e a hora de cada cartão aparecem ANTES do link do
        # próprio cartão (ou seja, no fim do trecho do cartão anterior).
        inicio_trecho = achados[i - 1].start() if i > 0 else max(0, m.start() - 8000)
        anterior = linhas_de_texto(html[inicio_trecho : m.start()])

        data = hora = None
        for l in reversed(anterior):
            mm = re.fullmatch(r"([A-Z][a-z]{2}) (\d{1,2})", l)
            if mm and mm.group(1) in MESES and data is None:
                data = (MESES[mm.group(1)], int(mm.group(2)))
            mh = re.fullmatch(r"(\d{1,2}):(\d{2}) ?([AP]M) ET", l)
            if mh and hora is None:
                hora = (int(mh.group(1)), int(mh.group(2)), mh.group(3))
            if data and hora:
                break

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
    m = re.fullmatch(r"Race (\d+)", n)
    if m:
        return f"Corrida {m.group(1)}"
    if n == "Practice":
        return "Treino Livre"
    if n == "Final Practice":
        return "Treino Final"
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
            mp = re.match(r"^(?:NTT )?INDYCAR SERIES\s*[-–]\s*(.+)$", desc)
            if mp:
                nome = traduzir(mp.group(1))
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


def filtrar_corrida_da_etapa(cartao, sessoes):
    """Se a página do fim de semana lista as duas corridas, associa cada uma ao cartão correto."""
    slug = str(cartao.get("slug", "")).casefold().replace("-", "")
    correspondencia = re.search(r"race([12])$", slug)
    if not correspondencia:
        return sessoes

    corrida_da_etapa = f"Corrida {correspondencia.group(1)}"
    corridas_publicadas = {
        nome for nome, _, _ in sessoes
        if nome in ("Corrida 1", "Corrida 2")
    }
    if corridas_publicadas != {"Corrida 1", "Corrida 2"}:
        return sessoes

    return [
        sessao for sessao in sessoes
        if sessao[0] not in ("Corrida 1", "Corrida 2") or sessao[0] == corrida_da_etapa
    ]


def montar_etapa(cartao, html_evento):
    ano = cartao["ano"]
    sessoes = filtrar_corrida_da_etapa(cartao, extrair_sessoes(html_evento, ano))

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


def validar_nao_reduzir_calendario(etapas_novas, arquivo=SAIDA, temporada_nova=None):
    """Preserva dados conhecidos sem bloquear remoções/renomeações oficiais.

    O circuito identifica a etapa mesmo quando o número muda. Dados conhecidos
    só são preservados para sessões que continuam existindo com o mesmo nome.
    """
    arquivo = Path(arquivo)
    if not arquivo.exists():
        return

    try:
        anterior = json.loads(arquivo.read_text(encoding="utf-8"))
        if (
            not isinstance(anterior, dict)
            or not isinstance(anterior.get("etapas"), list)
            or not anterior["etapas"]
            or any(
                not isinstance(etapa, dict)
                or not isinstance(etapa.get("sessoes"), list)
                or not etapa.get("etapa")
                for etapa in anterior["etapas"]
            )
        ):
            raise ValueError("estrutura do calendário anterior inválida")
    except (OSError, json.JSONDecodeError, ValueError) as e:
        raise ValueError(
            f"não foi possível validar o calendário anterior ({e}). Nada foi alterado."
        ) from e

    temporada_atual = anterior.get("temporada")
    if (
        temporada_nova is not None
        and temporada_atual is not None
        and str(temporada_nova) != str(temporada_atual)
    ):
        return

    # Algumas pistas recebem mais de uma corrida na mesma temporada
    # (por exemplo, Milwaukee). Nesses casos, o circuito sozinho não
    # identifica uma etapa; usamos também o nome oficial da prova.
    contagem_circuitos = {}
    for etapa in anterior["etapas"] + etapas_novas:
        circuito = str(etapa.get("circuito", "")).strip().casefold()
        if circuito:
            contagem_circuitos[circuito] = contagem_circuitos.get(circuito, 0) + 1
    circuitos_com_multiplas_etapas = {
        circuito for circuito, quantidade in contagem_circuitos.items()
        if quantidade > 2
    }

    def identidade(etapa):
        circuito = str(etapa.get("circuito", "")).strip().casefold()
        nome = str(etapa.get("nome", "")).strip().casefold()
        if circuito and circuito in circuitos_com_multiplas_etapas and nome:
            return ("circuito_nome", circuito, nome)
        if circuito:
            return ("circuito", circuito)
        if nome:
            return ("nome", nome)
        return ("etapa", str(etapa.get("etapa", "")))

    def conhecido(valor):
        normalizado = str(valor or "").strip().casefold()
        return bool(normalizado) and normalizado not in {
            "a confirmar", "tbd", "tba", "null", "none"
        }

    novas_por_identidade = {identidade(etapa): etapa for etapa in etapas_novas}
    for etapa_atual in anterior["etapas"]:
        etapa_nova = novas_por_identidade.get(identidade(etapa_atual))
        # Etapas removidas/renomeadas oficialmente não devem ser recriadas.
        if etapa_nova is None:
            continue

        for campo in ("inicio", "fim"):
            valor_atual = etapa_atual.get(campo, "")
            if conhecido(valor_atual) and not conhecido(etapa_nova.get(campo, "")):
                etapa_nova[campo] = valor_atual

        sessoes_novas = {
            str(sessao.get("nome", "")).strip().casefold(): sessao
            for sessao in etapa_nova.get("sessoes", [])
            if isinstance(sessao, dict) and str(sessao.get("nome", "")).strip()
        }
        for sessao_atual in etapa_atual["sessoes"]:
            if not isinstance(sessao_atual, dict):
                raise ValueError("estrutura de sessão anterior inválida. Nada foi alterado.")
            nome_sessao = str(sessao_atual.get("nome", "")).strip()
            sessao_nova = sessoes_novas.get(nome_sessao.casefold())
            # Sessões removidas/renomeadas não devem ser ressuscitadas.
            if sessao_nova is None:
                continue

            for campo in ("data", "horario"):
                valor_atual = sessao_atual.get(campo, "")
                if conhecido(valor_atual) and not conhecido(sessao_nova.get(campo, "")):
                    sessao_nova[campo] = valor_atual


def main():
    print("Consultando páginas oficiais da IndyCar...")
    try:
        cartoes = extrair_cartoes(baixar(LISTA))
    except Exception as e:  # noqa: BLE001
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    print("Etapas encontradas:", len(cartoes))
    if len(cartoes) < MINIMO_ETAPAS_VALIDAS:
        print(f"ERRO: calendário incompleto ({len(cartoes)} etapas; mínimo {MINIMO_ETAPAS_VALIDAS}). Nada foi alterado.")
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

    try:
        validar_nao_reduzir_calendario(saida, temporada_nova=ano)
    except ValueError as e:
        print("ERRO:", e)
        return 1

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())