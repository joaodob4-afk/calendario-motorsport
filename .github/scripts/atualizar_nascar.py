"""Atualiza os calendários das três séries nacionais da NASCAR.

A fonte é o feed público da NASCAR em cf.nascar.com. O feed traz o calendário
com as sessões de pista e seus horários UTC; não depende de scraping de notícias.
"""
import sys
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import baixar_json, formatar_data, gravar_se_mudou  # noqa: E402

BASE = "https://cf.nascar.com/cacher"
BRASILIA = ZoneInfo("America/Sao_Paulo")
SERIES = {
    1: ("NASCAR Cup Series", "nascar_cup_calendar.json", None),
    2: ("NASCAR Xfinity Series", "nascar_xfinity_calendar.json", "series_2"),
    3: ("NASCAR Craftsman Truck Series", "nascar_truck_calendar.json", "series_3"),
}
PAISES_POR_PISTA = {
    "autódromo hermanos rodríguez": "México",
    "autodromo hermanos rodriguez": "México",
    "circuit gilles villeneuve": "Canadá",
    "canadian tire motorsport park": "Canadá",
}


def converter_instante(valor):
    """O feed publica start_time_utc sem sufixo; interpreta como UTC."""
    if not valor:
        raise ValueError("Horário de sessão ausente")
    instante = datetime.fromisoformat(str(valor).replace("Z", "+00:00"))
    if instante.tzinfo is None:
        instante = instante.replace(tzinfo=timezone.utc)
    return instante.astimezone(BRASILIA)


def nome_sessao(evento, tipo):
    if tipo == 3:
        return "Corrida"
    if tipo == 2:
        return evento or "Classificação"
    return evento or "Treino"


def extrair_corrida(dados, numero, categoria):
    if not isinstance(dados, dict):
        return None
    nome = str(dados.get("race_name") or "").strip()
    pista = str(dados.get("track_name") or "").strip()
    if not nome or not pista or not dados.get("race_id"):
        return None

    sessoes = []
    for item in dados.get("schedule") or []:
        if not isinstance(item, dict):
            continue
        tipo = item.get("run_type")
        if tipo not in (1, 2, 3):
            continue
        try:
            instante = converter_instante(item.get("start_time_utc"))
        except (TypeError, ValueError):
            continue
        sessoes.append({
            "_instante": instante,
            "nome": nome_sessao(str(item.get("event_name") or "").strip(), tipo),
            "data": formatar_data(instante),
            "horario": instante.strftime("%H:%M"),
        })

    sessoes.sort(key=lambda s: s["_instante"])
    if not sessoes:
        return None

    inicio = sessoes[0]["_instante"]
    fim = sessoes[-1]["_instante"]
    pais = "Estados Unidos"
    pista_normalizada = pista.casefold()
    for trecho, pais_mapeado in PAISES_POR_PISTA.items():
        if trecho in pista_normalizada:
            pais = pais_mapeado
            break

    return {
        "_ordem": inicio,
        "etapa": numero,
        "circuito": pista,
        "pais": pais,
        "inicio": formatar_data(inicio),
        "fim": formatar_data(fim),
        "sessoes": [
            {k: v for k, v in sessao.items() if k != "_instante"}
            for sessao in sessoes
        ],
    }


def extrair_temporada(temporada, series_id, resposta):
    categoria, _, chave = SERIES[series_id]
    corridas = resposta if chave is None else (
        resposta.get(chave, []) if isinstance(resposta, dict) else []
    )
    if not isinstance(corridas, list):
        raise ValueError(f"Resposta inválida para {categoria}")
    etapas = []
    for corrida in corridas:
        evento = extrair_corrida(corrida, len(etapas) + 1, categoria)
        if evento:
            etapas.append(evento)
    etapas.sort(key=lambda e: e["_ordem"])
    for indice, etapa in enumerate(etapas, 1):
        etapa["etapa"] = indice
        etapa.pop("_ordem", None)
    if not etapas:
        raise ValueError(f"Nenhuma etapa com sessões encontrada para {categoria}")
    return {"temporada": temporada, "categoria": categoria, "etapas": etapas}


def atualizar(temporada=None, saida=None, baixar=baixar_json):
    temporada = temporada or datetime.now(timezone.utc).year
    saida = saida or Path("app/src/main/assets")
    url_cup = f"{BASE}/{temporada}/1/race_list_basic.json"
    url_demais = f"{BASE}/{temporada}/race_list_basic.json"

    cup = baixar(url_cup)
    demais = baixar(url_demais)
    resultados = {}
    for series_id, (categoria, arquivo, _) in SERIES.items():
        resposta = cup if series_id == 1 else demais
        dados = extrair_temporada(temporada, series_id, resposta)
        gravou = gravar_se_mudou(saida / arquivo, dados)
        resultados[categoria] = {"etapas": len(dados["etapas"]), "alterado": gravou}
    return resultados


if __name__ == "__main__":
    for categoria, resultado in atualizar().items():
        print(f"{categoria}: {resultado['etapas']} etapas; alterado={resultado['alterado']}")
