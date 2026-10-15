"""Atualiza app/src/main/assets/f3_calendar.json a partir do site oficial da F3.

Le a lista de etapas, abre cada pagina e usa os dados JSON-LD (subEvent)
com os horarios em UTC. Converte para horario de Brasilia.
Se qualquer etapa falhar na validacao, NAO altera o arquivo e sai com erro.
"""
import json
import re
import sys
import urllib.request
from datetime import datetime
from html.parser import HTMLParser
from pathlib import Path
from zoneinfo import ZoneInfo

BASE = "https://www.fiaformula3.com"
TEMPORADAS_PREFERIDAS = (datetime.now().year,)
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


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace")


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
        urls = (f"{BASE}/en/racing/{temporada}", f"{BASE}/en/racing")
        for url in urls:
            try:
                html = baixar(url)
            except Exception as erro:
                print(f"Calendário {temporada} indisponível em {url}: {erro}")
                continue
            slugs = listar_slugs(html, temporada)
            if slugs:
                return temporada, slugs
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
def parse_utc(texto):
    return datetime.fromisoformat(texto.replace("Z", "+00:00"))


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
            raise ValueError(f"Sessão desconhecida: {nome!r}")
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



def validar_horarios_exibidos(slug, sessoes):
    """Rejeita sessões distintas que ficam com a mesma data/hora exibida em Brasília."""
    vistos = {}
    for nome, instante in sessoes:
        local = instante.astimezone(BRASILIA)
        chave = (local.strftime("%d/%m/%Y"), local.strftime("%H:%M"))
        anterior = vistos.get(chave)
        if anterior is not None:
            raise ValueError(
                f"{slug}: {anterior!r} e {nome!r} aparecem no mesmo "
                f"horário de Brasília ({chave[0]} {chave[1]})"
            )
        vistos[chave] = nome


def montar_etapa(slug, html):
    if slug not in CIRCUITOS:
        raise ValueError(f"Circuito não mapeado: {slug}")
    circuito, pais, fuso = CIRCUITOS[slug]
    sessoes = traduzir_sessoes(sub_eventos(html))
    validar_horarios_exibidos(slug, sessoes)

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

    novo = {"temporada": temporada, "categoria": "F3", "etapas": etapas}
    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())