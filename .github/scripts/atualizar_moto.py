"""Atualiza os calendários de MotoGP, Moto2 e Moto3 em app/src/main/assets
(motogp_calendar.json, moto2_calendar.json, moto3_calendar.json) a partir da API de
calendário do motogp.com.

Os horários da API vêm com fuso (ex.: 2026-02-27T10:45:00+0700); o JSON sai em horário de
Brasília. Se qualquer validação falhar, NENHUM arquivo é alterado e o script sai com erro.
"""
import sys
from datetime import date, datetime
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import (  # noqa: E402
    baixar_json,
    gravar_se_mudou,
    ler_existente,
    obter_leitura_confirmada,
    total_de_sessoes,
)


def baixar(url):
    return baixar_json(url, tentativas=3)

SERVIDORES = [
    "https://api.motogp.pulselive.com",
    "https://api.pulselive.motogp.com",
]
CAMINHO = "/motogp/v1/events?seasonYear={ano}"
PASTA = Path("app/src/main/assets")
BRASILIA = ZoneInfo("America/Sao_Paulo")

# Sigla da classe na API -> (nome da categoria no app, arquivo)
CLASSES = {
    "MGP": ("MotoGP", "motogp_calendar.json"),
    "MT2": ("Moto2", "moto2_calendar.json"),
    "MT3": ("Moto3", "moto3_calendar.json"),
}

# Nome da sessão pela sigla da API (o nome da API traz patrocinador e espaços sobrando).
NOMES_SESSAO = {
    "FP1": "Treino Livre 1",
    "PR": "Treino",
    "FP2": "Treino Livre 2",
    "Q1": "Classificação 1",
    "Q2": "Classificação 2",
    "SPR": "Corrida Sprint",
    "WUP": "Warm-up",
    "RAC": "Corrida",
    "RAC2": "Corrida",
}
TIPOS_DE_SESSAO = {"PRACTICE", "QUALIFYING", "RACE", "WARM_UP"}

# Sigla do Grande Prêmio na API -> (nome do circuito no app, país)
ETAPAS = {
    "THA": ("Buriram", "Tailândia"),
    "BRA": ("Goiânia", "Brasil"),
    "USA": ("Austin", "Estados Unidos"),
    "SPA": ("Jerez", "Espanha"),
    "FRA": ("Le Mans", "França"),
    "CAT": ("Barcelona", "Espanha"),
    "ITA": ("Mugello", "Itália"),
    "HUN": ("Balaton Park", "Hungria"),
    "CZE": ("Brno", "República Tcheca"),
    "NED": ("Assen", "Holanda"),
    "GER": ("Sachsenring", "Alemanha"),
    "GBR": ("Silverstone", "Reino Unido"),
    "ARA": ("Aragón", "Espanha"),
    "RSM": ("Misano", "San Marino"),
    "AUT": ("Spielberg", "Áustria"),
    "JPN": ("Motegi", "Japão"),
    "INA": ("Mandalika", "Indonésia"),
    "AUS": ("Phillip Island", "Austrália"),
    "MAL": ("Sepang", "Malásia"),
    "QAT": ("Lusail", "Catar"),
    "POR": ("Portimão", "Portugal"),
    "VAL": ("Valencia", "Espanha"),
}


def obter_eventos(ano):
    """Eventos da temporada, tentando os dois nomes de servidor."""
    ultimo = None
    for servidor in SERVIDORES:
        try:
            url = servidor + CAMINHO.format(ano=ano)
            return obter_leitura_confirmada(
                lambda: baixar(url),
                assinatura_eventos,
                "MotoGP",
            )
        except Exception as e:  # noqa: BLE001
            ultimo = e
    raise ultimo


def grandes_premios(eventos):
    return [e for e in eventos if e.get("kind") == "GP"]


def assinatura_eventos(eventos):
    """Identidade dos GPs e das sessões publicadas pela API."""
    if not isinstance(eventos, list):
        raise ValueError("MotoGP: resposta da API não é uma lista")
    gps = grandes_premios(eventos)
    if not gps:
        raise ValueError("MotoGP: a API não trouxe Grandes Prêmios")

    assinatura = []
    sequencias = set()
    for evento in gps:
        sequencia = str(evento.get("sequence") or "")
        sigla = str(evento.get("shortname") or "")
        inicio = str(evento.get("date_start") or "")
        fim = str(evento.get("date_end") or "")
        if not sequencia or not sigla or not inicio or not fim or sequencia in sequencias:
            raise ValueError("MotoGP: Grande Prêmio com identificação incompleta ou repetida")
        sequencias.add(sequencia)

        broadcasts = evento.get("broadcasts") or []
        if not isinstance(broadcasts, list):
            raise ValueError(f"MotoGP: sessões inválidas no GP {sigla}")
        sessoes = tuple(sorted(
            (
                str((item.get("category") or {}).get("acronym") or "").upper(),
                str(item.get("shortname") or ""),
                str(item.get("type") or ""),
                str(item.get("kind") or ""),
                bool(item.get("date_start")),
            )
            for item in broadcasts
            if isinstance(item, dict)
        ))
        assinatura.append((sequencia, sigla, inicio, fim, sessoes))
    return tuple(assinatura)


def escolher_temporada(ano_atual, obter=obter_eventos):
    """Temporada com corridas ainda por fazer; se a atual acabou e a próxima já tem
    calendário publicado, passa para a próxima. Devolve (ano, eventos)."""
    atual = obter(ano_atual)
    gps = grandes_premios(atual)
    if not gps:
        raise ValueError(f"A API não trouxe Grandes Prêmios para {ano_atual}")

    if any(e.get("status") != "FINISHED" for e in gps):
        return ano_atual, atual

    try:
        proxima = obter(ano_atual + 1)
        if grandes_premios(proxima):
            return ano_atual + 1, proxima
    except Exception as e:  # noqa: BLE001
        print("Aviso: não foi possível ler a próxima temporada:", e)

    return ano_atual, atual


def ler_data_hora(texto):
    """'2026-02-27T10:45:00+0700' (ou com ':' no fuso) -> datetime com fuso."""
    return datetime.strptime(texto, "%Y-%m-%dT%H:%M:%S%z")


def formatar_data(d):
    return d.strftime("%d/%m/%Y")


def sessoes_da_classe(evento, sigla):
    """[(datetime com fuso do local, nome)] das sessões de pista da classe."""
    resultado = []
    for b in evento.get("broadcasts") or []:
        classe = ((b.get("category") or {}).get("acronym") or "").upper()
        if classe != sigla or b.get("type") != "SESSION":
            continue
        if b.get("kind") not in TIPOS_DE_SESSAO:
            continue

        nome = NOMES_SESSAO.get(b.get("shortname")) or " ".join(
            str(b.get("name") or "").split()
        )
        if not nome:
            continue

        try:
            momento = ler_data_hora(b["date_start"])
        except (KeyError, TypeError, ValueError):
            print("Aviso: sessão ignorada por horário inválido:", b.get("shortname"))
            continue

        resultado.append((momento, nome))

    return resultado


def periodo_do_evento(evento):
    """(início, fim) do fim de semana: datas locais das sessões das três classes;
    se não houver sessões, usa as datas do próprio evento."""
    dias = []
    for sigla in CLASSES:
        dias += [m.date() for m, _ in sessoes_da_classe(evento, sigla)]
    if dias:
        return min(dias), max(dias)

    inicio = date.fromisoformat(str(evento.get("date_start"))[:10])
    fim = date.fromisoformat(str(evento.get("date_end"))[:10])
    return inicio, fim


def local_da_etapa(evento):
    sigla = evento.get("shortname")
    if sigla in ETAPAS:
        return ETAPAS[sigla]

    circuito = evento.get("circuit") or {}
    nome = circuito.get("city") or circuito.get("name") or str(evento.get("name") or "").strip()
    pais = circuito.get("country") or evento.get("country") or ""
    return nome, pais


def nome_do_evento(evento):
    """Nome do evento como a API informa (espaços sobrando removidos); vazio se não houver."""
    return " ".join(str(evento.get("name") or "").split())


def montar(eventos, sigla, rotulo, ano):
    gps = grandes_premios(eventos)
    gps.sort(key=lambda e: (periodo_do_evento(e)[0], e.get("sequence") or 0))

    etapas = []
    for numero, evento in enumerate(gps, start=1):
        inicio, fim = periodo_do_evento(evento)
        circuito, pais = local_da_etapa(evento)

        sessoes = []
        vistos = set()
        for momento, nome in sorted(sessoes_da_classe(evento, sigla), key=lambda x: x[0]):
            em_brasilia = momento.astimezone(BRASILIA)
            item = (nome, formatar_data(em_brasilia), em_brasilia.strftime("%H:%M"))
            if item in vistos:
                continue
            vistos.add(item)
            sessoes.append({"nome": item[0], "data": item[1], "horario": item[2]})

        etapa = {"etapa": numero}
        nome = nome_do_evento(evento)
        if nome:  # sem nome na API, o app usa a tabela NomesDeEventos
            etapa["nome"] = nome
        etapa.update(
            {
                "circuito": circuito,
                "pais": pais,
                "inicio": formatar_data(inicio),
                "fim": formatar_data(fim),
                "sessoes": sessoes,
            }
        )
        etapas.append(etapa)

    return {"temporada": ano, "categoria": rotulo, "etapas": etapas}


def validar(novo, antigo):
    """Recusa calendário vazio e etapas com dados inválidos."""
    if not novo["etapas"]:
        raise ValueError(f"{novo['categoria']}: nenhuma etapa encontrada")

    for e in novo["etapas"]:
        if not e["circuito"].strip() or not e["pais"].strip():
            raise ValueError(f"{novo['categoria']}: etapa {e['etapa']} sem circuito ou país")
        if datetime.strptime(e["fim"], "%d/%m/%Y") < datetime.strptime(e["inicio"], "%d/%m/%Y"):
            raise ValueError(f"{novo['categoria']}: etapa {e['etapa']} com fim antes do início")



def main():
    ano_atual = datetime.now(BRASILIA).year
    ano, eventos = escolher_temporada(ano_atual)

    saidas = {}
    for sigla, (rotulo, arquivo) in CLASSES.items():
        novo = montar(eventos, sigla, rotulo, ano)
        validar(novo, ler_existente(PASTA / arquivo))
        saidas[arquivo] = novo

    # Só grava depois que as três classes passaram na validação.
    for arquivo, dados in saidas.items():
        caminho = PASTA / arquivo
        if not gravar_se_mudou(caminho, dados):
            print(f"{arquivo}: sem mudanças")
            continue
        print(f"{arquivo}: atualizado ({len(dados['etapas'])} etapas, {total_de_sessoes(dados)} sessões)")


if __name__ == "__main__":
    try:
        main()
    except Exception as erro:  # noqa: BLE001
        print("ERRO:", erro)
        sys.exit(1)
