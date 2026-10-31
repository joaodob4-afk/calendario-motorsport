"""Funções que vários atualizadores de calendário (atualizar_*.py) usam.

Ficam aqui para não repetir o mesmo código em cada script. As regras de cada fonte
(como ler a página, montar as etapas, validar) continuam em cada atualizador.
"""
import json
import math
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


def validar_encolhimento(novo, antigo, nome, fracao=0.20):
    """Recusa um calendário bem menor que o já gravado para a MESMA temporada.

    Uma página que muda de layout costuma devolver só parte das etapas; gravar isso apagaria
    o calendário de todos os usuários. Outra temporada pode ter outra quantidade de etapas.
    Levanta ValueError (o script avisa e não altera nada).
    """
    if not antigo or novo.get("temporada") != antigo.get("temporada"):
        return
    havia = len(antigo.get("etapas", []))
    minimo = max(1, math.ceil(havia * fracao))
    if len(novo.get("etapas", [])) < minimo:
        raise ValueError(
            f"{nome}: {len(novo.get('etapas', []))} etapas (havia {havia}); "
            "se for mesmo uma mudança real, edite o JSON à mão"
        )


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
