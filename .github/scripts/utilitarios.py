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


def traduzir_sessoes_formula(subs):
    """Normaliza sessões F2/F3 e preserva a ordem cronológica da fonte."""
    import re

    itens = []
    for sessao in subs:
        nome = (sessao.get("name") or "").split(" - ")[0].strip()
        itens.append((nome, parse_utc(sessao["startDate"])))
    itens.sort(key=lambda item: item[1])

    corridas = [i for i, (nome, _) in enumerate(itens) if nome.lower() == "race"]
    resultado = []
    for indice, (nome, instante) in enumerate(itens):
        normalizado = nome.lower()
        if normalizado.startswith("practice"):
            traduzido = "Treino Livre"
        elif normalizado.startswith("qualifying"):
            grupo = re.search(r"group\s*([ab])", normalizado)
            traduzido = "Classificação" + (
                f" Grupo {grupo.group(1).upper()}" if grupo else ""
            )
        elif "sprint" in normalizado:
            traduzido = "Corrida Sprint"
        elif "feature" in normalizado:
            traduzido = "Corrida Feature"
        elif normalizado == "race":
            # Em fins de semana normais, Race pode representar Sprint + Feature.
            # Quando há uma única Race, ela representa a Feature.
            if len(corridas) == 1:
                traduzido = "Corrida Feature"
            else:
                traduzido = (
                    "Corrida Sprint"
                    if corridas.index(indice) == 0
                    else "Corrida Feature"
                )
        else:
            print(f"AVISO: sessão desconhecida {nome!r}; mantendo o nome original")
            traduzido = nome or "Sessão"
        resultado.append((traduzido, instante))

    total = {}
    for nome, _ in resultado:
        total[nome] = total.get(nome, 0) + 1

    vistos = {}
    final = []
    for nome, instante in resultado:
        if total[nome] > 1:
            vistos[nome] = vistos.get(nome, 0) + 1
            nome = f"{nome} {vistos[nome]}"
        final.append((nome, instante))
    return final


def avisos_de_horario(slug, sessoes):
    """Lista sessões que compartilham dia e horário de Brasília, sem recusá-las."""
    from zoneinfo import ZoneInfo

    brasilia = ZoneInfo("America/Sao_Paulo")
    vistos = {}
    avisos = []
    for nome, instante in sessoes:
        local = instante.astimezone(brasilia)
        chave = (local.strftime("%d/%m/%Y"), local.strftime("%H:%M"))
        anterior = vistos.get(chave)
        if anterior is not None:
            avisos.append(
                f"{slug}: {anterior!r} e {nome!r} aparecem no mesmo "
                f"horário de Brasília ({chave[0]} {chave[1]})"
            )
        vistos[chave] = nome
    return avisos


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
