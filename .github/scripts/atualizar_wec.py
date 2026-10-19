"""Atualiza o calendário do WEC (app/src/main/assets/wec_calendar.json) a partir do site
oficial fiawec.com.

Cada página de etapa do site traz as sessões em dados estruturados (JSON-LD), com data, hora e
fuso do circuito. O JSON sai em horário de Brasília. Se qualquer validação falhar, NENHUM
arquivo é alterado e o script sai com erro.
"""
import html as htmllib
import json
import math
import re
import sys
import time
import urllib.request
from datetime import date, datetime
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import (  # noqa: E402
    baixar_texto as baixar_texto_comum,
    gravar_se_mudou,
    ler_existente,
    total_de_sessoes,
)

BASE = "https://www.fiawec.com"
PAGINAS_INICIAIS = [BASE + "/en/", BASE + "/en/season/2026"]
ARQUIVO = Path("app/src/main/assets/wec_calendar.json")
BRASILIA = ZoneInfo("America/Sao_Paulo")
CATEGORIA = "WEC"

# Nome da etapa no app e país, pelo final do endereço da página (sem o ano).
ETAPAS = {
    "6-hours-of-imola": ("Imola", "Itália"),
    "totalenergies-6-hours-of-spa-francorchamps": ("Spa-Francorchamps", "Bélgica"),
    "24-hours-of-le-mans": ("Circuit de la Sarthe", "França"),
    "rolex-6-hours-of-sao-paulo": ("Interlagos", "Brasil"),
    "lone-star-le-mans": ("Austin", "Estados Unidos"),
    "6-hours-of-fuji": ("Fuji", "Japão"),
    "6-hours-of-barcelona": ("Barcelona", "Espanha"),
    "6-hours-of-monza": ("Monza", "Itália"),
    "qatar-1812km": ("Lusail", "Catar"),
    "6-hours-of-silverstone": ("Silverstone", "Reino Unido"),
    "bapco-energies-8-hours-of-bahrain": ("Sakhir", "Bahrein"),
}

# Usado só quando uma etapa nova ainda não está na lista acima.
PAISES = {
    "ESP": "Espanha", "ITA": "Itália", "BEL": "Bélgica", "FRA": "França", "BRA": "Brasil",
    "USA": "Estados Unidos", "JPN": "Japão", "QAT": "Catar", "GBR": "Reino Unido",
    "BHR": "Bahrein", "BAH": "Bahrein", "GER": "Alemanha", "DEU": "Alemanha",
    "POR": "Portugal", "PRT": "Portugal", "MEX": "México", "AUS": "Austrália",
}

MESES = {
    nome: numero
    for numero, nome in enumerate(
        ["january", "february", "march", "april", "may", "june", "july", "august",
         "september", "october", "november", "december"],
        start=1,
    )
}

# Quanto o novo calendário pode encolher em relação ao atual antes de ser recusado.
FRACAO_MINIMA = 0.5

CABECALHOS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml",
    "Accept-Language": "en",
}


def baixar_texto(url):
    return baixar_texto_comum(url, tentativas=3, cabecalhos=CABECALHOS)


# ------------------------------------------------------------------ descoberta

RE_CORRIDA = re.compile(r"/en/race/([a-z0-9][a-z0-9-]*-(20\d{2}))(?=[\"'#?\s<&/])")


def descobrir_corridas(html):
    """[(endereço sem ano e ano, caminho completo)] das etapas do campeonato, sem o Prologue."""
    vistos = []
    for m in RE_CORRIDA.finditer(html):
        caminho = m.group(1)
        if "prologue" in caminho:
            continue
        if caminho not in vistos:
            vistos.append(caminho)
    return vistos


def ano_do_caminho(caminho):
    return int(caminho[-4:])


def base_do_caminho(caminho):
    return re.sub(r"-20\d{2}$", "", caminho)


# --------------------------------------------------------------- página da etapa

def blocos_json_ld(html):
    for m in re.finditer(
        r"<script[^>]*type=[\"']application/ld\+json[\"'][^>]*>(.*?)</script>",
        html,
        flags=re.S | re.I,
    ):
        try:
            yield json.loads(m.group(1).strip())
        except ValueError:
            continue


def achar_evento(obj):
    """Primeiro objeto com a lista 'subEvent' (as sessões)."""
    if isinstance(obj, dict):
        if isinstance(obj.get("subEvent"), list):
            return obj
        for valor in obj.values():
            achado = achar_evento(valor)
            if achado:
                return achado
    elif isinstance(obj, list):
        for item in obj:
            achado = achar_evento(item)
            if achado:
                return achado
    return None


def nome_curto_do_evento(nome):
    """'WEC 6 Hours of Barcelona 2026' -> '6 Hours of Barcelona'."""
    texto = " ".join(str(nome or "").split())
    texto = re.sub(r"^WEC\s+", "", texto, flags=re.I)
    return re.sub(r"\s+20\d{2}$", "", texto)


def traduzir_sessao(nome, evento_curto=""):
    """'Hyperpole - HYPERCAR - 6 Hours of Barcelona' -> 'Hyperpole Hypercar'."""
    texto = " ".join(str(nome).split())

    if evento_curto:
        sufixo = " - " + evento_curto
        if texto.lower().endswith(sufixo.lower()):
            texto = texto[: -len(sufixo)].rstrip()

    # Se o nome da etapa veio escrito de outro jeito, ainda remove o final "- ... Hours of ...".
    texto = re.sub(
        r"\s+-\s+(?:(?!\s-\s).)*?(?:Hours? of|Le Mans|1812\s*km).*$", "", texto, flags=re.I
    )

    texto = re.sub(r"\bFree Practice\b", "Treino Livre", texto, flags=re.I)
    texto = re.sub(r"\bQualifying\b", "Classificação", texto, flags=re.I)
    texto = re.sub(r"\bRace\b", "Corrida", texto, flags=re.I)
    texto = re.sub(r"\bWarm[ -]?up\b", "Warm-up", texto, flags=re.I)
    texto = re.sub(r"\bHYPERCAR\b", "Hypercar", texto)
    return re.sub(r"\s+-\s+", " ", texto).strip()


def ler_data_hora(texto):
    return datetime.fromisoformat(str(texto).strip().replace("Z", "+00:00"))


def sessoes_do_evento(evento):
    """[(datetime com o fuso do circuito, nome em português)]."""
    curto = nome_curto_do_evento(evento.get("name"))
    resultado = []
    for sub in evento.get("subEvent") or []:
        if not isinstance(sub, dict) or not sub.get("name") or not sub.get("startDate"):
            continue
        try:
            momento = ler_data_hora(sub["startDate"])
        except ValueError:
            print("Aviso: sessão ignorada por data inválida:", sub.get("name"))
            continue
        if momento.tzinfo is None:
            print("Aviso: sessão ignorada por falta de fuso:", sub.get("name"))
            continue
        resultado.append((momento, traduzir_sessao(sub["name"], curto)))
    return resultado


RE_PERIODO = re.compile(
    r"From\s+(\d{1,2})(?:\s+([A-Za-z]+))?\s+to\s+(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})"
)


def texto_visivel(html):
    sem_scripts = re.sub(r"<(script|style)\b.*?</\1>", " ", html, flags=re.S | re.I)
    return " ".join(htmllib.unescape(re.sub(r"<[^>]+>", " ", sem_scripts)).split())


def periodo_da_pagina(html):
    """(início, fim) do texto 'From 16 to 18 October 2026' que vem logo depois do título da
    página (o menu e o carrossel de outras etapas ficam fora dessa parte)."""
    inicio_do_titulo = re.search(r"<h1\b", html, flags=re.I)
    trecho = html[inicio_do_titulo.start():] if inicio_do_titulo else html
    m = RE_PERIODO.search(texto_visivel(trecho))
    if not m:
        return None

    dia1, mes1, dia2, mes2, ano = m.groups()
    mes_fim = MESES.get(mes2.lower())
    mes_ini = MESES.get(mes1.lower()) if mes1 else mes_fim
    if not mes_fim or not mes_ini:
        return None
    try:
        return date(int(ano), mes_ini, int(dia1)), date(int(ano), mes_fim, int(dia2))
    except ValueError:
        return None


def local_da_etapa(caminho, evento):
    base = base_do_caminho(caminho)
    if base in ETAPAS:
        return ETAPAS[base]

    local = (evento or {}).get("location") or {}
    nome = str(local.get("name") or "").strip() or base.replace("-", " ").title()
    codigo = str(local.get("address") or "").split(",")[-1].strip().upper()
    return nome, PAISES.get(codigo, codigo or "—")


def ler_etapa(caminho, html):
    """Etapa do app (com sessões em horário de Brasília) a partir do HTML da página."""
    evento = None
    for bloco in blocos_json_ld(html):
        evento = achar_evento(bloco)
        if evento:
            break

    sessoes_locais = sessoes_do_evento(evento) if evento else []
    periodo = periodo_da_pagina(html)

    dias = [m.date() for m, _ in sessoes_locais]
    if periodo:
        inicio, fim = periodo
        if dias:
            inicio, fim = min(inicio, min(dias)), max(fim, max(dias))
    elif dias:
        inicio, fim = min(dias), max(dias)
    else:
        raise ValueError(f"{caminho}: não achei as datas da etapa na página")

    circuito, pais = local_da_etapa(caminho, evento)

    sessoes = []
    vistos = set()
    for momento, nome in sorted(sessoes_locais, key=lambda x: x[0]):
        em_brasilia = momento.astimezone(BRASILIA)
        item = (nome, em_brasilia.strftime("%d/%m/%Y"), em_brasilia.strftime("%H:%M"))
        if item in vistos:
            continue
        vistos.add(item)
        sessoes.append({"nome": item[0], "data": item[1], "horario": item[2]})

    return {
        "circuito": circuito,
        "pais": pais,
        "inicio": inicio.strftime("%d/%m/%Y"),
        "fim": fim.strftime("%d/%m/%Y"),
        "sessoes": sessoes,
    }


# ----------------------------------------------------------------- temporada

def parse_data(texto):
    return datetime.strptime(texto, "%d/%m/%Y").date()


def escolher_temporada(etapas_por_ano, hoje):
    """Ano com etapa ainda por acontecer (a mais próxima); se todas acabaram, o último ano."""
    anos = sorted(etapas_por_ano)
    for ano in anos:
        if max(parse_data(e["fim"]) for e in etapas_por_ano[ano]) >= hoje:
            return ano
    return anos[-1]


def montar(etapas, ano):
    ordenadas = sorted(etapas, key=lambda e: (parse_data(e["inicio"]), e["circuito"]))
    return {
        "temporada": ano,
        "categoria": CATEGORIA,
        "etapas": [{"etapa": i, **e} for i, e in enumerate(ordenadas, start=1)],
    }


def validar(novo, antigo):
    """Recusa calendário vazio ou muito menor que o atual da mesma temporada."""
    if not novo["etapas"]:
        raise ValueError("WEC: nenhuma etapa encontrada")

    for e in novo["etapas"]:
        if not e["circuito"].strip() or not e["pais"].strip():
            raise ValueError(f"WEC: etapa {e['etapa']} sem circuito ou país")
        if parse_data(e["fim"]) < parse_data(e["inicio"]):
            raise ValueError(f"WEC: etapa {e['etapa']} com fim antes do início")

    if antigo and str(antigo.get("temporada")) == str(novo["temporada"]):
        minimo_etapas = max(1, math.ceil(len(antigo["etapas"]) * FRACAO_MINIMA))
        if len(novo["etapas"]) < minimo_etapas:
            raise ValueError(f"WEC: {len(novo['etapas'])} etapas (havia {len(antigo['etapas'])})")
        minimo_sessoes = math.ceil(total_de_sessoes(antigo) * FRACAO_MINIMA)
        if total_de_sessoes(novo) < minimo_sessoes:
            raise ValueError(
                f"WEC: {total_de_sessoes(novo)} sessões (havia {total_de_sessoes(antigo)})"
            )


def coletar(baixar=baixar_texto):
    """Baixa o calendário e todas as páginas de etapa. Devolve {ano: [etapas]}."""
    corridas = []
    ultimo_erro = None
    for pagina in PAGINAS_INICIAIS:
        try:
            corridas = descobrir_corridas(baixar(pagina))
        except Exception as e:  # noqa: BLE001
            ultimo_erro = e
            continue
        if corridas:
            break
    if not corridas:
        raise ValueError(f"Nenhuma etapa encontrada no site do WEC ({ultimo_erro})")

    por_ano = {}
    for caminho in corridas:
        # Se uma página falhar, o erro sobe: melhor não atualizar do que perder uma etapa.
        html = baixar(f"{BASE}/en/race/{caminho}")
        etapa = ler_etapa(caminho, html)
        print(
            f"{caminho}: {len(etapa['sessoes'])} sessões, {etapa['inicio']} a {etapa['fim']}"
        )
        por_ano.setdefault(ano_do_caminho(caminho), []).append(etapa)
    return por_ano


def main():
    por_ano = coletar()
    hoje = datetime.now(BRASILIA).date()
    ano = escolher_temporada(por_ano, hoje)

    novo = montar(por_ano[ano], ano)
    validar(novo, ler_existente(ARQUIVO))

    if not gravar_se_mudou(ARQUIVO, novo):
        print("wec_calendar.json: sem mudanças")
        return

    print(
        f"wec_calendar.json: atualizado ({len(novo['etapas'])} etapas, "
        f"{total_de_sessoes(novo)} sessões, temporada {ano})"
    )


if __name__ == "__main__":
    try:
        main()
    except Exception as erro:  # noqa: BLE001
        print("ERRO:", erro)
        sys.exit(1)
