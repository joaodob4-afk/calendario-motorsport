"""Funções que vários atualizadores de calendário (atualizar_*.py) usam.

Ficam aqui para não repetir o mesmo código em cada script. As regras de cada fonte
(como ler a página, montar as etapas, validar) continuam em cada atualizador.
"""
import json
import os
import tempfile
import time
import urllib.request
from datetime import datetime

CABECALHOS_PAGINA = {"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"}
CABECALHOS_JSON = {"User-Agent": "Mozilla/5.0", "Accept": "application/json"}


def _baixar(url, cabecalhos, tentativas, converter):
    """Baixa e converte. Com mais de uma tentativa, espera 2 s, 4 s... entre elas."""
    ultimo = None
    for tentativa in range(tentativas):
        try:
            req = urllib.request.Request(url, headers=cabecalhos)
            with urllib.request.urlopen(req, timeout=30) as r:
                return converter(r.read())
        except Exception as erro:  # noqa: BLE001
            ultimo = erro
            if tentativa < tentativas - 1:
                time.sleep(2 * (tentativa + 1))
    raise ultimo


def baixar_texto(url, tentativas=1, cabecalhos=None):
    """Texto da página. Uma tentativa por padrão: o erro original sobe sem demora."""
    return _baixar(
        url,
        cabecalhos or CABECALHOS_PAGINA,
        tentativas,
        lambda b: b.decode("utf-8", errors="replace"),
    )


def baixar_json(url, tentativas=3):
    """JSON de uma API, já convertido (resposta inválida também conta como falha)."""
    return _baixar(
        url, CABECALHOS_JSON, tentativas, lambda b: json.loads(b.decode("utf-8"))
    )


def obter_leitura_confirmada(obter, assinatura, fonte):
    """Faz duas leituras independentes e só devolve a segunda se a estrutura coincidir."""
    primeira = obter()
    segunda = obter()
    if assinatura(primeira) != assinatura(segunda):
        raise ValueError(f"{fonte}: a fonte retornou dados diferentes em leituras consecutivas")
    return segunda


def parse_utc(texto):
    """'2026-03-06T10:30:00Z' -> datetime com fuso UTC."""
    return datetime.fromisoformat(texto.replace("Z", "+00:00"))


def localizar_evento_jsonld(html):
    """Extrai nome e endereço da localização do evento principal em JSON-LD."""
    import re

    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )

    def procurar(dados):
        if isinstance(dados, dict):
            local = dados.get("location")
            if isinstance(local, dict) and dados.get("subEvent"):
                return local
            for valor in dados.values():
                encontrado = procurar(valor)
                if encontrado:
                    return encontrado
        elif isinstance(dados, list):
            for item in dados:
                encontrado = procurar(item)
                if encontrado:
                    return encontrado
        return None

    for bloco in blocos:
        try:
            dados = json.loads(bloco)
        except ValueError:
            continue
        local = procurar(dados)
        if local:
            endereco = local.get("address")
            if isinstance(endereco, dict):
                pais = endereco.get("addressCountry") or ""
                if isinstance(pais, dict):
                    pais = pais.get("name") or pais.get("identifier") or ""
                partes = [
                    str(endereco.get("addressLocality") or "").strip(),
                    str(endereco.get("addressRegion") or "").strip(),
                    str(pais).strip(),
                ]
                texto_endereco = ", ".join(parte for parte in partes if parte)
                if not pais:
                    texto_endereco = ""
            else:
                texto_endereco = str(endereco or "").strip() if isinstance(endereco, str) else ""
            return {
                "nome": str(local.get("name") or "").strip(),
                "endereco": texto_endereco,
            }
    return None


def formatar_data(d):
    """date -> 'dd/mm/aaaa', o formato dos JSONs do app."""
    return d.strftime("%d/%m/%Y")


def ler_existente(caminho):
    """JSON já gravado, ou None se não existir ou estiver ilegível."""
    try:
        return json.loads(caminho.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def serializar(dados):
    return json.dumps(dados, ensure_ascii=False, indent=2) + "\n"


def total_de_sessoes(dados):
    return sum(len(e.get("sessoes", [])) for e in dados.get("etapas", []))


def gravar_se_mudou(caminho, dados):
    """Grava o JSON só se mudou, substituindo o arquivo atomicamente."""
    texto = serializar(dados)
    if caminho.exists() and caminho.read_text(encoding="utf-8") == texto:
        return False

    descritor, temporario = tempfile.mkstemp(
        prefix=f".{caminho.name}.", suffix=".tmp", dir=caminho.parent
    )
    try:
        with os.fdopen(descritor, "w", encoding="utf-8") as arquivo:
            arquivo.write(texto)
            arquivo.flush()
            os.fsync(arquivo.fileno())
        os.replace(temporario, caminho)
    finally:
        if os.path.exists(temporario):
            os.unlink(temporario)
    return True


def _chave_texto(valor):
    """Normaliza nomes de etapas para comparação sem depender de acentos ou caixa."""
    import re
    import unicodedata

    texto = unicodedata.normalize("NFKD", str(valor or ""))
    texto = "".join(c for c in texto if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", " ", texto.casefold()).strip()


def _identidade_etapa(etapa):
    """Identidade estável da etapa; número da rodada não é suficiente entre temporadas."""
    nome = etapa.get("nomeEvento") or etapa.get("nome") or ""
    circuito = etapa.get("circuito") or ""
    pais = etapa.get("pais") or ""
    return _chave_texto(nome), _chave_texto(circuito), _chave_texto(pais)


def _data_calendario(texto):
    from datetime import datetime

    try:
        return datetime.strptime(str(texto), "%d/%m/%Y").date()
    except (TypeError, ValueError):
        return None


def preservar_sessoes_temporada_anterior(novo, anterior):
    """Completa sessões ainda sem horário usando a programação oficial anterior.

    As etapas são associadas por nome/circuito/país, nunca apenas pelo número.
    Datas atuais são mantidas; para sessões adicionadas, o dia é deslocado pela
    diferença entre o início das duas etapas. Horários oficiais atuais sempre
    prevalecem. Dados copiados recebem 'provisorio': true e não viram fonte
    para outra troca de temporada.
    """
    from copy import deepcopy
    from datetime import timedelta

    etapas_novas = novo.get("etapas") if isinstance(novo, dict) else None
    etapas_antigas = anterior.get("etapas") if isinstance(anterior, dict) else None
    if not isinstance(etapas_novas, list) or not isinstance(etapas_antigas, list):
        return 0
    if str(novo.get("temporada", "")) == str(anterior.get("temporada", "")):
        return 0

    antigas_por_identidade = {}
    antigas_por_circuito = {}
    for etapa in etapas_antigas:
        if not isinstance(etapa, dict):
            continue
        sessoes = etapa.get("sessoes")
        if not isinstance(sessoes, list):
            continue
        # Nunca propaga um horário que já era provisório.
        oficiais = [
            s for s in sessoes
            if isinstance(s, dict)
            and not s.get("provisorio")
            and str(s.get("horario") or "").strip().casefold() not in
            ("", "a confirmar", "tbc", "tba")
        ]
        if not oficiais:
            continue
        nome, circuito, pais = _identidade_etapa(etapa)
        if not circuito or not pais:
            continue
        antigas_por_identidade.setdefault((nome, circuito, pais), []).append(etapa)
        antigas_por_circuito.setdefault((circuito, pais), []).append(etapa)

    atualizadas = 0
    for etapa in etapas_novas:
        if not isinstance(etapa, dict):
            continue
        nome, circuito, pais = _identidade_etapa(etapa)
        if not circuito or not pais:
            continue

        candidatas = antigas_por_identidade.get((nome, circuito, pais), [])
        if len(candidatas) != 1:
            candidatas = antigas_por_circuito.get((circuito, pais), [])
        if len(candidatas) != 1:
            continue
        anterior_etapa = candidatas[0]

        sessoes_atuais = etapa.get("sessoes")
        if not isinstance(sessoes_atuais, list):
            sessoes_atuais = []
            etapa["sessoes"] = sessoes_atuais

        inicio_anterior = _data_calendario(anterior_etapa.get("inicio"))
        inicio_atual = _data_calendario(etapa.get("inicio"))
        deslocamento = (
            (inicio_atual - inicio_anterior)
            if inicio_atual and inicio_anterior else timedelta(0)
        )

        por_nome = {}
        for sessao in sessoes_atuais:
            if isinstance(sessao, dict):
                por_nome.setdefault(_chave_texto(sessao.get("nome")), []).append(sessao)

        for antiga in anterior_etapa.get("sessoes", []):
            if not isinstance(antiga, dict):
                continue
            horario = str(antiga.get("horario") or "").strip()
            if not horario or horario.casefold() in ("a confirmar", "tbc", "tba"):
                continue
            chave = _chave_texto(antiga.get("nome"))
            if not chave:
                continue

            correspondentes = por_nome.get(chave, [])
            if correspondentes:
                destino = correspondentes[0]
                horario_atual = str(destino.get("horario") or "").strip()
                if horario_atual and horario_atual.casefold() not in ("a confirmar", "tbc", "tba"):
                    continue
                destino["horario"] = horario
                if not destino.get("data"):
                    data_antiga = _data_calendario(antiga.get("data"))
                    if data_antiga:
                        destino["data"] = (data_antiga + deslocamento).strftime("%d/%m/%Y")
                destino["provisorio"] = True
                atualizadas += 1
                continue

            copia = deepcopy(antiga)
            data_antiga = _data_calendario(copia.get("data"))
            if data_antiga:
                copia["data"] = (data_antiga + deslocamento).strftime("%d/%m/%Y")
            copia["provisorio"] = True
            sessoes_atuais.append(copia)
            por_nome.setdefault(chave, []).append(copia)
            atualizadas += 1

    return atualizadas


def preservar_horarios_f1_temporada_anterior(novo, anterior):
    """Preserva horários de sessões da F1 por circuito quando a API ainda não os publicou."""
    from copy import deepcopy
    from datetime import date

    def corridas(dados):
        return (
            dados.get("MRData", {})
            .get("RaceTable", {})
            .get("Races", [])
            if isinstance(dados, dict) else []
        )

    temporada_nova = str((novo.get("MRData", {}).get("RaceTable", {}) or {}).get("season", ""))
    temporada_antiga = str((anterior.get("MRData", {}).get("RaceTable", {}) or {}).get("season", ""))
    if temporada_nova and temporada_nova == temporada_antiga:
        return 0

    def identidade(corrida):
        circuito = corrida.get("Circuit") or {}
        local = circuito.get("Location") or {}
        return (
            _chave_texto(circuito.get("circuitId")),
            _chave_texto(circuito.get("circuitName")),
            _chave_texto(local.get("country")),
        )

    antigas_por_id = {}
    antigas_por_nome = {}
    for corrida in corridas(anterior):
        circuito_id, circuito_nome, pais = identidade(corrida)
        if not pais or not (circuito_id or circuito_nome):
            continue
        antigas_por_id.setdefault((circuito_id, pais), []).append(corrida)
        antigas_por_nome.setdefault((circuito_nome, pais), []).append(corrida)

    atualizadas = 0
    for corrida in corridas(novo):
        circuito_id, circuito_nome, pais = identidade(corrida)
        candidatas = antigas_por_id.get((circuito_id, pais), []) if circuito_id else []
        if len(candidatas) != 1:
            candidatas = antigas_por_nome.get((circuito_nome, pais), []) if circuito_nome else []
        if len(candidatas) != 1:
            continue
        antiga = candidatas[0]

        try:
            deslocamento = (
                date.fromisoformat(corrida["date"]) - date.fromisoformat(antiga["date"])
            )
        except (KeyError, TypeError, ValueError):
            deslocamento = None

        for campo in ("time", "FirstPractice", "SecondPractice", "ThirdPractice",
                      "Qualifying", "SprintQualifying", "SprintShootout", "Sprint"):
            valor_antigo = antiga.get(campo)
            if campo == "time":
                if corrida.get(campo):
                    continue
                if not valor_antigo:
                    continue
                corrida[campo] = valor_antigo
                corrida["time_provisorio"] = True
                atualizadas += 1
                continue

            if not isinstance(valor_antigo, dict) or valor_antigo.get("provisorio"):
                continue
            valor_atual = corrida.get(campo)
            if isinstance(valor_atual, dict) and valor_atual.get("time"):
                continue
            copia = deepcopy(valor_antigo)
            if deslocamento and copia.get("date"):
                try:
                    copia["date"] = (
                        date.fromisoformat(copia["date"]) + deslocamento
                    ).isoformat()
                except ValueError:
                    pass
            copia["provisorio"] = True
            corrida[campo] = copia
            atualizadas += 1

    return atualizadas
