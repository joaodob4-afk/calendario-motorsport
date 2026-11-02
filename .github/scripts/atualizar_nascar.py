"""Atualiza os calendários NASCAR usando a agenda filtrada por série da API oficial.

Cada registro da agenda identifica series_id, race_id e race_name. Assim, sessões de
Cup, O'Reilly/Xfinity e Truck nunca são misturadas no mesmo evento.
"""
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import baixar_json, formatar_data, gravar_se_mudou  # noqa: E402

BASE = "https://feed.nascar.com/api/weekendschedule"
BRASILIA = ZoneInfo("America/Sao_Paulo")
SERIES = {
    1: ("NASCAR Cup Series", "nascar_cup_calendar.json"),
    2: ("NASCAR Xfinity Series", "nascar_xfinity_calendar.json"),
    3: ("NASCAR Craftsman Truck Series", "nascar_truck_calendar.json"),
}
PAISES_POR_PISTA = {
    "autódromo hermanos rodríguez": "México",
    "autodromo hermanos rodriguez": "México",
    "circuit gilles villeneuve": "Canadá",
    "canadian tire motorsport park": "Canadá",
}


def converter_instante(valor):
    """O feed publica start_time_utc em UTC."""
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


def extrair_temporada(temporada, series_id, resposta):
    """Agrupa as sessões por race_id, aceitando somente a série solicitada."""
    categoria, _ = SERIES[series_id]
    if not isinstance(resposta, list):
        raise ValueError(f"Resposta inválida para {categoria}")

    corridas = defaultdict(list)
    for item in resposta:
        if not isinstance(item, dict):
            continue
        try:
            serie_item = int(item.get("series_id"))
            race_id = int(item.get("race_id"))
            tipo = int(item.get("run_type"))
        except (TypeError, ValueError):
            continue

        if serie_item != series_id or tipo not in (1, 2, 3):
            continue

        nome = str(item.get("race_name") or "").strip()
        pista = str(item.get("track_name") or "").strip()
        if not nome or not pista:
            continue

        try:
            instante = converter_instante(item.get("start_time_utc"))
        except (TypeError, ValueError):
            continue

        corridas[race_id].append({
            "_instante": instante,
            "nome": nome_sessao(str(item.get("event_name") or "").strip(), tipo),
            "data": formatar_data(instante),
            "horario": instante.strftime("%H:%M"),
            "race_name": nome,
            "track_name": pista,
        })

    etapas = []
    for race_id, sessoes in corridas.items():
        sessoes.sort(key=lambda s: s["_instante"])
        inicio = sessoes[0]["_instante"]
        fim = sessoes[-1]["_instante"]
        nome_evento = sessoes[0]["race_name"]
        pista = sessoes[0]["track_name"]

        pais = "Estados Unidos"
        pista_normalizada = pista.casefold()
        for trecho, pais_mapeado in PAISES_POR_PISTA.items():
            if trecho in pista_normalizada:
                pais = pais_mapeado
                break

        etapas.append({
            "_ordem": inicio,
            "etapa": 0,
            "nomeEvento": nome_evento,
            "circuito": pista,
            "pais": pais,
            "inicio": formatar_data(inicio),
            "fim": formatar_data(fim),
            "sessoes": [
                {k: v for k, v in sessao.items()
                 if k not in ("_instante", "race_name", "track_name")}
                for sessao in sessoes
            ],
        })

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
    resultados = {}

    for series_id, (categoria, arquivo) in SERIES.items():
        url = (
            f"{BASE}?series_id={series_id}"
            f"&race_season={temporada}&v=2"
        )
        resposta = baixar(url)
        dados = extrair_temporada(temporada, series_id, resposta)
        gravou = gravar_se_mudou(saida / arquivo, dados)
        resultados[categoria] = {
            "etapas": len(dados["etapas"]),
            "alterado": gravou,
        }

    return resultados


if __name__ == "__main__":
    for categoria, resultado in atualizar().items():
        print(f"{categoria}: {resultado['etapas']} etapas; alterado={resultado['alterado']}")
