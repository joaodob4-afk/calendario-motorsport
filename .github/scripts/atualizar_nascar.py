"""Atualiza os calendários NASCAR usando os feeds oficiais por série.

Cup possui endpoint próprio. Xfinity/O'Reilly e Truck compartilham o feed
geral e são separados pelas chaves series_2 e series_3.
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
    """Interpreta horários sem fuso como UTC e converte para Brasília."""
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


def selecionar_corridas(resposta, series_id):
    """Seleciona somente a série pedida no formato real do feed NASCAR."""
    if isinstance(resposta, list):
        if series_id == 1:
            return resposta
        raise ValueError(f"Feed compartilhado inválido para a série {series_id}")

    if isinstance(resposta, dict):
        chave = f"series_{series_id}"
        for nome, corridas in resposta.items():
            if str(nome).casefold() == chave:
                if isinstance(corridas, list):
                    return corridas
                break

    raise ValueError(f"Feed NASCAR sem lista válida para series_{series_id}")


def extrair_temporada(temporada, series_id, resposta):
    """Converte a lista de corridas de uma única série em etapas do app."""
    categoria, _ = SERIES[series_id]
    corridas = selecionar_corridas(resposta, series_id)

    etapas = []
    for corrida in corridas:
        if not isinstance(corrida, dict):
            continue
        nome_evento = str(corrida.get("race_name") or "").strip()
        pista = str(corrida.get("track_name") or "").strip()
        if not nome_evento or not pista:
            continue

        sessoes = []
        for item in corrida.get("schedule") or []:
            if not isinstance(item, dict):
                continue
            try:
                tipo = int(item.get("run_type"))
                if tipo not in (1, 2, 3):
                    continue
                instante = converter_instante(item.get("start_time_utc"))
            except (TypeError, ValueError):
                continue
            sessoes.append({
                "_instante": instante,
                "nome": nome_sessao(str(item.get("event_name") or "").strip(), tipo),
                "data": formatar_data(instante),
                "horario": instante.strftime("%H:%M"),
            })

        if not sessoes:
            continue
        sessoes.sort(key=lambda s: s["_instante"])
        inicio, fim = sessoes[0]["_instante"], sessoes[-1]["_instante"]

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
                {k: v for k, v in sessao.items() if k != "_instante"}
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
    url_cup = f"{BASE}/{temporada}/1/race_list_basic.json"
    url_compartilhada = f"{BASE}/{temporada}/race_list_basic.json"

    respostas = {
        1: baixar(url_cup),
        2: baixar(url_compartilhada),
    }
    respostas[3] = respostas[2]
    resultados = {}

    for series_id, (categoria, arquivo) in SERIES.items():
        dados = extrair_temporada(temporada, series_id, respostas[series_id])
        gravou = gravar_se_mudou(saida / arquivo, dados)
        resultados[categoria] = {
            "etapas": len(dados["etapas"]),
            "alterado": gravou,
        }
    return resultados


if __name__ == "__main__":
    for categoria, resultado in atualizar().items():
        print(f"{categoria}: {resultado['etapas']} etapas; alterado={resultado['alterado']}")
