"""Atualiza o calendário da F1 (app/src/main/assets/f1_calendar.json) a partir da API Jolpica.

O app já baixa esses dados direto da Jolpica, mas se ela ficar fora do ar não sobra nenhuma
cópia. Este script guarda uma cópia no repositório, no MESMO formato do cache do app
(MRData.RaceTable.Races, estilo Ergast), para o app usar como reserva e como semente na
primeira abertura sem internet. Se qualquer validação falhar, nenhum arquivo é alterado.
"""
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import (  # noqa: E402
    baixar_json,
    gravar_se_mudou,
    obter_leitura_confirmada,
)

URL = "https://api.jolpi.ca/f1/alpha/schedules/{ano}/"
SAIDA = Path("app/src/main/assets/f1_calendar.json")

CAMPOS_DE_SESSAO = {
    "FP1": "FirstPractice",
    "FP2": "SecondPractice",
    "FP3": "ThirdPractice",
    "Q": "Qualifying",
    "SQ": "SprintQualifying",
    "SR": "Sprint",
    "S": "Sprint",
}


def _instante(timestamp):
    """'2026-03-06T01:30:00+00:00' ou '...Z' -> datetime em UTC, ou None se inválido."""
    if not isinstance(timestamp, str) or not timestamp.strip() or timestamp == "null":
        return None
    try:
        return datetime.fromisoformat(timestamp.replace("Z", "+00:00")).astimezone(
            timezone.utc
        )
    except ValueError:
        return None


def normalizar(resposta, ano_padrao=None):
    """Converte a resposta do endpoint alpha (data.events[].schedule[]) para o formato Ergast.

    É o mesmo que F1CalendarApi.normalizarProgramacaoDetalhada faz no app.
    """
    raiz = resposta["data"]
    ano = int(raiz.get("year") or ano_padrao or datetime.now().year)
    corridas = []
    for i, evento in enumerate(raiz.get("events", [])):
        rodada = evento.get("round")
        circuito = evento.get("circuit")
        if not isinstance(rodada, dict) or not isinstance(circuito, dict):
            raise ValueError(f"F1: evento {i + 1} sem rodada ou circuito")
        agenda = evento.get("schedule") or []
        if not isinstance(agenda, list):
            raise ValueError(f"F1: programação inválida na rodada {i + 1}")

        sessoes = {}
        data_corrida = hora_corrida = ""
        for item in agenda:
            if not isinstance(item, dict):
                continue
            instante = _instante(item.get("timestamp"))
            if instante is None:
                continue
            codigo = str(item.get("code", "")).upper()
            data = instante.date().isoformat()
            hora = instante.strftime("%H:%M:%SZ")
            if codigo == "R":
                data_corrida, hora_corrida = data, hora
            elif codigo in CAMPOS_DE_SESSAO:
                sessoes[CAMPOS_DE_SESSAO[codigo]] = {"date": data, "time": hora}

        if not data_corrida and agenda and isinstance(agenda[0], dict):
            primeira = _instante(agenda[0].get("timestamp"))
            if primeira is not None:
                data_corrida = primeira.date().isoformat()
        if not data_corrida:
            raise ValueError(f"F1: rodada {rodada.get('number') or i + 1} sem data de corrida")

        corrida = {
            "season": str(ano),
            "round": str(rodada.get("number") or i + 1),
            "raceName": rodada.get("name") or "Grande Prêmio",
            "Circuit": {
                "circuitId": circuito.get("id") or f"round_{i + 1}",
                "circuitName": circuito.get("name") or "Circuito",
                "Location": {
                    "locality": circuito.get("locality") or "",
                    "country": circuito.get("country") or "",
                },
            },
            "date": data_corrida,
        }
        if hora_corrida:
            corrida["time"] = hora_corrida
        corrida.update(sessoes)
        corridas.append(corrida)

    return {
        "MRData": {
            "series": "f1",
            "RaceTable": {"season": str(ano), "Races": corridas},
        }
    }


def assinatura_resposta(resposta):
    """Identidade das corridas e tipos de sessão publicados pela API."""
    raiz = resposta.get("data")
    eventos = raiz.get("events") if isinstance(raiz, dict) else None
    if not isinstance(eventos, list) or not eventos:
        raise ValueError("F1: a API não retornou uma lista de corridas")

    assinatura = []
    rodadas = set()
    for indice, evento in enumerate(eventos, start=1):
        if not isinstance(evento, dict):
            raise ValueError(f"F1: evento {indice} inválido")
        rodada = evento.get("round")
        circuito = evento.get("circuit")
        if not isinstance(rodada, dict) or not isinstance(circuito, dict):
            raise ValueError(f"F1: evento {indice} sem rodada ou circuito")
        # Usa os mesmos fallbacks de normalizar(): a API pode publicar um evento
        # antes de preencher o número da rodada ou o ID do circuito.
        numero = str(rodada.get("number") or indice)
        circuito_id = str(circuito.get("id") or f"round_{indice}")
        if numero in rodadas:
            raise ValueError(f"F1: número de rodada duplicado: {numero}")
        rodadas.add(numero)
        agenda = evento.get("schedule")
        if not isinstance(agenda, list):
            raise ValueError(f"F1: programação inválida na rodada {numero}")
        codigos = tuple(sorted(
            (
                str(item.get("code") or "").upper(),
                str(item.get("timestamp") or ""),
            )
            for item in agenda
            if isinstance(item, dict)
        ))
        assinatura.append((numero, circuito_id, codigos))
    return tuple(assinatura)


def _corridas(dados):
    return dados.get("MRData", {}).get("RaceTable", {}).get("Races", [])


def _total_de_sessoes(dados):
    return sum(
        1
        for c in _corridas(dados)
        for campo in CAMPOS_DE_SESSAO.values()
        if campo in c
    )


def validar(novo):
    """Valida corridas, datas e consistência básica do calendário."""
    corridas = _corridas(novo)
    if not corridas:
        raise ValueError("F1: nenhuma corrida encontrada")
    datas = [c["date"] for c in corridas]
    if len(set(datas)) != len(datas):
        raise ValueError("F1: duas corridas na mesma data")



def main():
    ano = datetime.now().year
    print(f"Consultando a Jolpica para a temporada {ano}...")
    try:
        resposta = obter_leitura_confirmada(
            lambda: baixar_json(URL.format(ano=ano)),
            assinatura_resposta,
            "F1",
        )
        novo = normalizar(resposta, ano)
        validar(novo)
    except Exception as erro:  # noqa: BLE001
        print("ERRO:", erro)
        print("Nada foi alterado.")
        return 1

    print("Corridas:", len(_corridas(novo)), "| sessões:", _total_de_sessoes(novo))
    if not gravar_se_mudou(SAIDA, novo):
        print("Sem mudanças no calendário.")
        return 0
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())
