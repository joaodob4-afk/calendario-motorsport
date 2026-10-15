"""Atualiza app/src/main/assets/f2_calendar.json a partir do site oficial da F2.

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

BASE = "https://www.fiaformula2.com"
TEMPORADAS_PREFERIDAS = (2027, 2026)
FIM_TEMPORADA_2026 = datetime(2026, 12, 7).date()
SAIDA = Path("app/src/main/assets/f2_calendar.json")
BRASILIA = ZoneInfo("America/Sao_Paulo")

# slug do site -> (circuito, pais, fuso local do circuito)
CIRCUITOS = {
    "sakhir": ("Sakhir", "Bahrein", "Asia/Bahrain"),
    "jeddah": ("Jeddah", "Arábia Saudita", "Asia/Riyadh"),
    "melbourne": ("Melbourne", "Austrália", "Australia/Melbourne"),
    "miami-gardens": ("Miami", "Estados Unidos", "America/New_York"),
    "montreal": ("Montreal", "Canadá", "America/Toronto"),
    "monte-carlo": ("Monte Carlo", "Mônaco", "Europe/Monaco"),
    "barcelona": ("Barcelona", "Espanha", "Europe/Madrid"),
    "spielberg": ("Spielberg", "Áustria", "Europe/Vienna"),
    "silverstone": ("Silverstone", "Reino Unido", "Europe/London"),
    "spa-francorchamps": ("Spa-Francorchamps", "Bélgica", "Europe/Brussels"),
    "budapest": ("Budapeste", "Hungria", "Europe/Budapest"),
    "monza": ("Monza", "Itália", "Europe/Rome"),
    "madrid": ("Madrid", "Espanha", "Europe/Madrid"),
    "baku": ("Baku City Circuit", "Azerbaijão", "Asia/Baku"),
    "istanbul": ("Istanbul Park", "Turquia", "Europe/Istanbul"),
    "mexico-city": ("Cidade do México", "México", "America/Mexico_City"),
    "lusail": ("Lusail", "Catar", "Asia/Qatar"),
    "yas-marina": ("Yas Marina", "Emirados Árabes Unidos", "Asia/Dubai"),
}


def baixar_com_url(url):
    """Baixa a página e devolve também a URL final após redirecionamentos."""
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace"), r.geturl()


def baixar(url):
    return baixar_com_url(url)[0]


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


def listar_slugs(html, temporada=2026):
    p = LinksParser(temporada)
    p.feed(html)
    return sorted(p.links)


def ordem_temporadas(hoje):
    """Prioriza a temporada mais recente publicada, com fallback para 2026."""
    return TEMPORADAS_PREFERIDAS


def descobrir_calendario():
    """Descobre calendários por URL oficial estável e usa rotas por ano como fallback.

    A rota /en/racing pode redirecionar para a temporada publicada no site.
    Só aceitamos a página se o ano final corresponder à temporada procurada,
    evitando trocar de temporada antes da hora.
    """
    temporadas = ordem_temporadas(datetime.now(BRASILIA).date())

    for temporada in temporadas:
        urls = (f"{BASE}/en/racing/{temporada}", f"{BASE}/en/racing")
        visitadas = set()
        for url in urls:
            if url in visitadas:
                continue
            visitadas.add(url)
            try:
                html, url_final = baixar_com_url(url)
            except Exception as erro:
                print(f"Calendário F2 {temporada} indisponível em {url}: {erro}")
                continue

            # Se a URL final expõe um ano, ele precisa coincidir com o alvo.
            # Algumas versões do site podem trocar a rota por uma URL canônica
            # sem ano; nesse caso, os links das etapas precisam carregar
            # explicitamente o ano solicitado, sem depender do endereço final.
            ano_url = re.search(r"/en/racing/(\d{4})(?:/|$)", url_final)
            if ano_url and int(ano_url.group(1)) != temporada:
                continue

            slugs = listar_slugs(html, temporada)
            if slugs:
                return temporada, slugs
        print(f"Calendário F2 {temporada} ainda não publicado como página de etapas.")
    return None, []


def sub_eventos(html):
    """Extrai subEvent de JSON-LD em objeto, lista ou grafo @graph."""
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )

    def procurar(dados):
        if isinstance(dados, dict):
            eventos = dados.get("subEvent")
            if isinstance(eventos, list) and eventos:
                return eventos
            # JSON-LD pode envolver a entidade em @graph ou em objetos aninhados.
            for valor in dados.values():
                encontrados = procurar(valor)
                if encontrados:
                    return encontrados
        elif isinstance(dados, list):
            for item in dados:
                encontrados = procurar(item)
                if encontrados:
                    return encontrados
        return []

    for bloco in blocos:
        try:
            dados = json.loads(bloco)
        except ValueError:
            continue
        eventos = procurar(dados)
        if eventos:
            return eventos
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


def validar_sessoes_sem_conflito(slug, sessoes):
    """Falha se duas sessões da etapa começarem no mesmo instante."""
    instantes = {}
    for nome, dt in sessoes:
        sessao_anterior = instantes.get(dt)
        if sessao_anterior is not None:
            raise ValueError(
                f"{slug}: {sessao_anterior!r} e {nome!r} começam "
                f"no mesmo instante ({dt.isoformat()})"
            )
        instantes[dt] = nome



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
    local = ZoneInfo(fuso)

    # O site oficial ainda publica os horários de Yas Marina como meia-noite
    # local (placeholder), inclusive com sessões diferentes no mesmo instante.
    # Nessa situação, preservamos as datas locais do evento e não inventamos horas.
    horarios_pendentes = (
        slug == "yas-marina"
        and bool(sessoes)
        and all(
            dt.astimezone(local).hour == 0
            and dt.astimezone(local).minute == 0
            for _, dt in sessoes
        )
    )

    if horarios_pendentes:
        print(f"{slug}: horários oficiais ainda não publicados; usando A confirmar")
    else:
        validar_horarios_exibidos(slug, sessoes)
        validar_sessoes_sem_conflito(slug, sessoes)

    if not sessoes:
        raise ValueError(f"{slug}: nenhuma sessão encontrada")

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
                "data": (dt.astimezone(local) if horarios_pendentes else dt.astimezone(BRASILIA)).strftime(fmt),
                "horario": "A confirmar" if horarios_pendentes else dt.astimezone(BRASILIA).strftime("%H:%M"),
            }
            for nome, dt in sessoes
        ],
    }



def main():
    print("Consultando páginas oficiais da F2...")
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

    novo = {"temporada": temporada, "categoria": "F2", "etapas": etapas}
    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())