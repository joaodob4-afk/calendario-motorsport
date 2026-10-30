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
from utilitarios import baixar_texto as baixar, gravar_se_mudou, parse_utc  # noqa: E402

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
    """Extrai subEvent de JSON-LD simples, em lista ou dentro de @graph."""
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )

    def procurar(dados):
        if isinstance(dados, list):
            for item in dados:
                resultado = procurar(item)
                if resultado:
                    return resultado
        elif isinstance(dados, dict):
            subs = dados.get("subEvent")
            if isinstance(subs, dict):
                return [subs]
            if isinstance(subs, list) and subs:
                return subs
            grafo = dados.get("@graph")
            if isinstance(grafo, (dict, list)):
                return procurar(grafo)
        return []

    for bloco in blocos:
        try:
            dados = json.loads(bloco)
        except ValueError:
            continue
        resultado = procurar(dados)
        if resultado:
            return resultado
    return []
def traduzir_sessoes(subs):
    """Retorna lista de (nome_pt, datetime_utc) em ordem cronologica."""
    itens = []
    for s in subs:
        nome = (s.get("name") or "").split(" - ")[0].strip()
        itens.append((nome, parse_utc(s["startDate"])))
    itens.sort(key=lambda x: x[1])

    corridas = [i for i, (n, _) in enumerate(itens) if n.lower() == "race"]
    resultado = []
    for idx, (nome, dt) in enumerate(itens):
        baixo = nome.lower()
        if baixo.startswith("practice"):
            pt = "Treino Livre"
        elif baixo.startswith("qualifying"):
            g = re.search(r"group\s*([ab])", baixo)
            pt = "Classificação" + (f" Grupo {g.group(1).upper()}" if g else "")
        elif "sprint" in baixo:
            pt = "Corrida Sprint"
        elif "feature" in baixo:
            pt = "Corrida Feature"
        elif baixo == "race":
            # Etapa normal: 2 corridas (Sprint, Feature). Etapa "supersized"
            # (ex.: Baku 2026): Sprint + 2 Features. A 1a é sempre a Sprint.
            if len(corridas) == 1:
                pt = "Corrida Feature"
            else:
                pt = "Corrida Sprint" if corridas.index(idx) == 0 else "Corrida Feature"
        else:
            # Nome novo no site: mantém o texto original em vez de recusar a atualização.
            print(f"AVISO: sessão desconhecida {nome!r}; mantendo o nome original")
            pt = nome or "Sessão"
        resultado.append((pt, dt))

    # Nomes repetidos na mesma etapa ganham número (ex.: Feature 1 e 2).
    total = {}
    for pt, _ in resultado:
        total[pt] = total.get(pt, 0) + 1
    visto = {}
    final = []
    for pt, dt in resultado:
        if total[pt] > 1:
            visto[pt] = visto.get(pt, 0) + 1
            pt = f"{pt} {visto[pt]}"
        final.append((pt, dt))
    return final



def avisos_de_horario(slug, sessoes):
    """Lista (sem recusar a atualização) sessões com o mesmo dia e horário em Brasília.

    Não é um erro: o app apenas mostra as duas sessões. Serve de alerta no log.
    """
    vistos = {}
    avisos = []
    for nome, instante in sessoes:
        local = instante.astimezone(BRASILIA)
        chave = (local.strftime("%d/%m/%Y"), local.strftime("%H:%M"))
        anterior = vistos.get(chave)
        if anterior is not None:
            avisos.append(
                f"{slug}: {anterior!r} e {nome!r} aparecem no mesmo "
                f"horário de Brasília ({chave[0]} {chave[1]})"
            )
        vistos[chave] = nome
    return avisos


def montar_etapa(slug, html):
    if slug not in CIRCUITOS:
        raise ValueError(f"Circuito não mapeado: {slug}")
    circuito, pais, fuso = CIRCUITOS[slug]
    sessoes = traduzir_sessoes(sub_eventos(html))
    for aviso in avisos_de_horario(slug, sessoes):
        print("AVISO", aviso)

    if not sessoes:
        raise ValueError(f"{slug}: nenhuma sessão encontrada")

    local = ZoneInfo(fuso)
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
            etapas.append(montar_etapa(slug, baixar(f"{BASE}/en/racing/{temporada}/{slug}")))
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