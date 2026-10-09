import re
import urllib.request
from html.parser import HTMLParser

URL = "https://www.fiaformula2.com/en/racing/2026"


class LinksParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() != "a":
            return

        for nome, valor in attrs:
            if nome.lower() == "href" and valor:
                if re.search(
                    r"/en/racing/2026/[^/?#]+",
                    valor
                ):
                    self.links.append(valor)


def main():
    print("Consultando páginas oficiais da F2...")

    requisicao = urllib.request.Request(
        URL,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept-Language": "en-US,en;q=0.9",
        },
    )

    with urllib.request.urlopen(
        requisicao, timeout=30
    ) as resposta:
        html = resposta.read().decode(
            "utf-8", errors="replace"
        )

    parser = LinksParser()
    parser.feed(html)

    links = sorted(set(parser.links))

    print("Links de etapas encontrados:", len(links))

    for link in links:
        if link.startswith("/"):
            link = "https://www.fiaformula2.com" + link
        print(link)

    if not links:
        print("Nenhum link de etapa encontrado.")
        print("O calendário não foi alterado.")

    print("Teste concluído. Nenhum arquivo foi alterado.")


if __name__ == "__main__":
    main()"""Atualiza app/src/main/assets/f2_calendar.json a partir do site oficial da F2.

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
URL = BASE + "/en/racing/2026"
SAIDA = Path("app/src/main/assets/f2_calendar.json")
BRASILIA = ZoneInfo("America/Sao_Paulo")

# slug do site -> (circuito, pais, fuso local do circuito)
CIRCUITOS = {
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
    def __init__(self):
        super().__init__()
        self.links = set()

    def handle_starttag(self, tag, attrs):
        if tag.lower() != "a":
            return
        for nome, valor in attrs:
            if nome.lower() == "href" and valor:
                m = re.search(r"/en/racing/2026/([^/?#]+)", valor)
                if m:
                    self.links.add(m.group(1))


def listar_slugs(html):
    p = LinksParser()
    p.feed(html)
    return sorted(p.links)


def sub_eventos(html):
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )
    for b in blocos:
        try:
            dados = json.loads(b)
        except ValueError:
            continue
        if isinstance(dados, dict) and dados.get("subEvent"):
            return dados["subEvent"]
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
            pos = corridas.index(idx)
            pt = "Corrida Feature" if pos == len(corridas) - 1 else "Corrida Sprint"
        else:
            raise ValueError(f"Sessão desconhecida: {nome!r}")
        resultado.append((pt, dt))
    return resultado


def montar_etapa(slug, html):
    if slug not in CIRCUITOS:
        raise ValueError(f"Circuito não mapeado: {slug}")
    circuito, pais, fuso = CIRCUITOS[slug]
    sessoes = traduzir_sessoes(sub_eventos(html))

    nomes = [n for n, _ in sessoes]
    if len(sessoes) < 3 or "Corrida Feature" not in nomes:
        raise ValueError(f"{slug}: sessões incompletas {nomes}")

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
    print("Consultando páginas oficiais da F2...")
    slugs = listar_slugs(baixar(URL))
    print("Etapas encontradas:", len(slugs))
    if len(slugs) < 10:
        print("ERRO: poucas etapas encontradas. Nada foi alterado.")
        return 1

    etapas = []
    for slug in slugs:
        try:
            etapas.append(montar_etapa(slug, baixar(f"{BASE}/en/racing/2026/{slug}")))
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

    novo = {"temporada": 2026, "categoria": "F2", "etapas": etapas}
    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())
