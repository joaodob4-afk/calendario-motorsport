"""Atualiza app/src/main/assets/f3_calendar.json a partir do site oficial da F3.

Le a lista de etapas, abre cada pagina e usa os dados JSON-LD (subEvent)
com os horarios em UTC. Converte para horario de Brasilia.
Se qualquer etapa falhar na validacao, NAO altera o arquivo e sai com erro.
"""
import json
import re
import sys
from datetime import datetime
from html.parser import HTMLParser
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import (  # noqa: E402
    avisos_de_horario,
    baixar_texto as baixar,
    gravar_se_mudou,
    localizar_evento_jsonld,
    obter_leitura_confirmada,
    parse_utc,
    traduzir_sessoes_formula as traduzir_sessoes,
)

BASE = "https://www.fiaformula3.com"
# Ano atual primeiro; o anterior só serve de reserva no começo do ano, antes de o site publicar
# a nova temporada (assim o job não falha todos os dias de janeiro).
TEMPORADAS_PREFERIDAS = (datetime.now().year, datetime.now().year - 1)
SAIDA = Path("app/src/main/assets/f3_calendar.json")
BRASILIA = ZoneInfo("America/Sao_Paulo")

# slug do site -> (circuito, pais, fuso local do circuito)
CIRCUITOS = {
    "sakhir": ("Sakhir", "Bahrein", "Asia/Bahrain"),
    "jeddah": ("Jeddah", "Arábia Saudita", "Asia/Riyadh"),
    "melbourne": ("Melbourne", "Austrália", "Australia/Melbourne"),
    "miami-gardens": ("Miami", "Estados Unidos", "America/New_York"),
    "monte-carlo": ("Monte Carlo", "Mônaco", "Europe/Monaco"),
    "barcelona": ("Barcelona", "Espanha", "Europe/Madrid"),
    "spielberg": ("Spielberg", "Áustria", "Europe/Vienna"),
    "silverstone": ("Silverstone", "Reino Unido", "Europe/London"),
    "spa-francorchamps": ("Spa-Francorchamps", "Bélgica", "Europe/Brussels"),
    "budapest": ("Budapeste", "Hungria", "Europe/Budapest"),
    "monza": ("Monza", "Itália", "Europe/Rome"),
    "madrid": ("Madrid", "Espanha", "Europe/Madrid"),
    "baku": ("Baku City Circuit", "Azerbaijão", "Asia/Baku"),
    "lusail": ("Lusail", "Catar", "Asia/Qatar"),
    "yas-marina": ("Yas Marina", "Emirados Árabes Unidos", "Asia/Dubai"),
}


class LinksParser(HTMLParser):
    def __init__(self, temporada):
        super().__init__()
        self.temporada = temporada
        self.links = set()

    def handle_starttag(self, tag, attrs):
        if tag.lower() != "a":
            return
        for nome, valor in attrs:
            if nome.lower() == "href" and valor:
                m = re.search(rf"/en/racing/{self.temporada}/([^/?#]+)", valor)
                if m:
                    self.links.add(m.group(1))


def listar_slugs(html, temporada=None):
    temporada = temporada or TEMPORADAS_PREFERIDAS[0]
    p = LinksParser(temporada)
    p.feed(html)
    return sorted(p.links)


def descobrir_calendario():
    """Descobre somente a temporada atual, usando a rota oficial estável como alternativa."""
    for temporada in TEMPORADAS_PREFERIDAS:
        indice_observado = False
        urls = (f"{BASE}/en/racing/{temporada}", f"{BASE}/en/racing")
        for url in urls:
            amostras = []
            for _ in range(2):
                try:
                    html = baixar(url)
                except Exception as erro:
                    print(f"Calendário {temporada} indisponível em {url}: {erro}")
                    amostras = []
                    break
                slugs = listar_slugs(html, temporada)
                if not slugs:
                    amostras = []
                    break
                indice_observado = True
                amostras.append(slugs)

            # Uma resposta isolada pode estar truncada. Exigimos que duas leituras
            # independentes da mesma rota confirmem a mesma lista de etapas.
            if len(amostras) == 2 and amostras[0] == amostras[1]:
                return temporada, amostras[0]
            if len(amostras) == 2:
                print(f"Índice F3 {temporada} mudou entre leituras; tentando outra rota.")
        if temporada == TEMPORADAS_PREFERIDAS[0] and indice_observado:
            print("Índice F3 atual sem confirmação; preservando a temporada já publicada.")
            return None, []
        print(f"Calendário {temporada} ainda não publicado como página de etapas.")
    return None, []

def sub_eventos(html):
    """Extrai e normaliza sessões de estruturas JSON-LD simples ou aninhadas."""
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )
    encontrados = []
    vistos = set()

    def valor_escalar(valor):
        if isinstance(valor, dict):
            return valor.get("@value") or valor.get("name") or ""
        return valor if isinstance(valor, str) else ""

    def adicionar(valor):
        if isinstance(valor, dict) and isinstance(valor.get("@list"), list):
            for item in valor["@list"]:
                adicionar(item)
            return
        if not isinstance(valor, dict):
            return

        nome = valor_escalar(valor.get("name")).strip()
        inicio = valor_escalar(valor.get("startDate")).strip()
        if not nome or not inicio:
            return
        try:
            instante = parse_utc(inicio)
        except (TypeError, ValueError):
            return

        chave = (nome.casefold(), instante.isoformat())
        if chave in vistos:
            return
        vistos.add(chave)
        encontrados.append({"name": nome, "startDate": inicio})

    def percorrer(dados):
        if isinstance(dados, list):
            for item in dados:
                percorrer(item)
            return
        if not isinstance(dados, dict):
            return

        subs = dados.get("subEvent")
        if isinstance(subs, list):
            for item in subs:
                adicionar(item)
        elif isinstance(subs, dict):
            adicionar(subs)

        # JSON-LD pode distribuir o evento entre @graph, @included ou
        # propriedades aninhadas; percorremos o restante sem duplicar sessões.
        for chave, valor in dados.items():
            if chave != "subEvent" and isinstance(valor, (dict, list)):
                percorrer(valor)

    for bloco in blocos:
        try:
            dados = json.loads(bloco)
        except ValueError:
            continue
        percorrer(dados)

    return encontrados


def montar_etapa(slug, html):
    metadados = CIRCUITOS.get(slug)
    if metadados:
        circuito, pais, fuso = metadados
    else:
        localizacao = localizar_evento_jsonld(html)
        endereco = (localizacao or {}).get("endereco", "")
        partes = [parte.strip() for parte in endereco.split(",") if parte.strip()]
        circuito = (localizacao or {}).get("nome", "")
        if not circuito or len(partes) < 2 or partes[-1].casefold() == circuito.casefold():
            raise ValueError(f"{slug}: localização oficial sem circuito e país identificáveis")
        pais = partes[-1]
        fuso = None
    sessoes = traduzir_sessoes(sub_eventos(html))
    for aviso in avisos_de_horario(slug, sessoes):
        print("AVISO", aviso)

    if not sessoes:
        raise ValueError(f"{slug}: nenhuma sessão encontrada")

    local = ZoneInfo(fuso) if fuso else BRASILIA
    fmt = "%d/%m/%Y"
    return {
        "_inicio_utc": sessoes[0][1],
        "circuito": circuito,
        "pais": pais,
        "inicio": sessoes[0][1].astimezone(local).strftime(fmt),
        "fim": sessoes[-1][1].astimezone(local).strftime(fmt),
        "sessoes": [
            {
                "nome": nome,
                "data": dt.astimezone(BRASILIA).strftime(fmt),
                "horario": dt.astimezone(BRASILIA).strftime("%H:%M"),
            }
            for nome, dt in sessoes
        ],
    }



def obter_etapa_confirmada(slug, temporada):
    url = f"{BASE}/en/racing/{temporada}/{slug}"
    html = obter_leitura_confirmada(
        lambda: baixar(url),
        lambda pagina: sorted(
            (evento.get("name", ""), evento.get("startDate", ""))
            for evento in sub_eventos(pagina)
        ),
        f"F3 {slug}",
    )
    return montar_etapa(slug, html)


def main():
    print("Consultando páginas oficiais da F3...")
    temporada, slugs = descobrir_calendario()
    print("Temporada selecionada:", temporada)
    print("Etapas encontradas:", len(slugs))
    if not slugs:
        print("ERRO: nenhuma etapa encontrada. Nada foi alterado.")
        return 1

    etapas = []
    for slug in slugs:
        try:
            etapas.append(obter_etapa_confirmada(slug, temporada))
            print("OK  ", slug)
        except Exception as e:
            print("ERRO", slug, "->", e)
            print("Nada foi alterado.")
            return 1

    etapas.sort(key=lambda e: e["_inicio_utc"])
    for n, e in enumerate(etapas, start=1):
        e.pop("_inicio_utc")
        e["etapa"] = n
    etapas = [
        {k: e[k] for k in ("etapa", "circuito", "pais", "inicio", "fim", "sessoes")}
        for e in etapas
    ]

    novo = {"indice_confirmado": True, "temporada": temporada, "categoria": "F3", "etapas": etapas}
    if not gravar_se_mudou(SAIDA, novo):
        print("Sem mudanças no calendário.")
        return 0

    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())