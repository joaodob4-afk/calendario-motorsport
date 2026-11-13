"""Atualiza app/src/main/assets/fe_calendar.json a partir do site oficial da
Formula E (JSON-LD "ItemList" da pagina /en/calendar).

A pagina traz so datas (sem horarios de sessao); as sessoes ficam como
"A confirmar". Se a validacao falhar, NAO altera o arquivo e sai com erro.
"""
import json
import re
import sys
from datetime import date, datetime
from html.parser import HTMLParser
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import baixar_texto as baixar, formatar_data as fmt, gravar_se_mudou  # noqa: E402

URL = "https://www.fiaformulae.com/en/calendar"
SAIDA = Path("app/src/main/assets/fe_calendar.json")

PAISES = {
    "SA": "Arábia Saudita",
    "MX": "México",
    "US": "Estados Unidos",
    "BR": "Brasil",
    "DE": "Alemanha",
    "MC": "Mônaco",
    "GB": "Reino Unido",
    "NL": "Holanda",
    "ES": "Espanha",
    "CN": "China",
    "JP": "Japão",
    "IT": "Itália",
    "FR": "França",
    "PT": "Portugal",
    "AU": "Austrália",
    "KR": "Coreia do Sul",
    "IN": "Índia",
    "ID": "Indonésia",
    "AE": "Emirados Árabes Unidos",
    "CA": "Canadá",
    "HU": "Hungria",
    "BE": "Bélgica",
    "AT": "Áustria",
    "CH": "Suíça",
    "SE": "Suécia",
}

CIDADES = {
    "Mexico City": "Cidade do México",
    "Sao Paulo": "São Paulo",
    "Monaco": "Monte Carlo",
    "London": "Brands Hatch",
    "Shanghai": "Xangai",
    "Tokyo": "Tóquio",
    "Berlin": "Berlim",
}

# Páginas oficiais de evento publicam a programação completa (horário local da pista).
# O JSON do calendário geral continua sendo a fonte do índice e das datas.
SLUGS_POR_CIDADE = {
    "Cidade do México": "mexicocity",
    "Austin": "austin",
    "Miami": "miami",
    "São Paulo": "saopaulo",
    "Sanya": "sanya",
    "Jeddah": "jeddah",
    "Monte Carlo": "monaco",
    "Berlim": "berlin",
    "Brands Hatch": "london",
    "Zandvoort": "zandvoort",
    "Madrid": "madrid",
    "Xangai": "shanghai",
    "Tóquio": "tokyo",
}

FUSOS_POR_CIDADE = {
    "Cidade do México": "America/Mexico_City",
    "Austin": "America/Chicago",
    "Miami": "America/New_York",
    "São Paulo": "America/Sao_Paulo",
    "Sanya": "Asia/Shanghai",
    "Jeddah": "Asia/Riyadh",
    "Monte Carlo": "Europe/Monaco",
    "Berlim": "Europe/Berlin",
    "Brands Hatch": "Europe/London",
    "Zandvoort": "Europe/Amsterdam",
    "Madrid": "Europe/Madrid",
    "Xangai": "Asia/Shanghai",
    "Tóquio": "Asia/Tokyo",
}

MESES = {
    "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
    "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12,
}


class _LeitorTabelas(HTMLParser):
    """Extrai células de tabelas sem depender de classes CSS do site."""

    def __init__(self):
        super().__init__()
        self.linhas = []
        self.linha = None
        self.celula = None

    def handle_starttag(self, tag, attrs):
        if tag == "tr":
            self.linha = []
        elif tag in ("td", "th") and self.linha is not None:
            self.celula = []

    def handle_data(self, data):
        if self.celula is not None:
            self.celula.append(data)

    def handle_endtag(self, tag):
        if tag in ("td", "th") and self.celula is not None:
            self.linha.append(" ".join(" ".join(self.celula).split()))
            self.celula = None
        elif tag == "tr" and self.linha is not None:
            if self.linha:
                self.linhas.append(self.linha)
            self.linha = None


def extrair_sessoes_evento(html, ano, fuso_local, rodada):
    """Lê a tabela oficial Schedule e converte horário da pista para Brasília."""
    leitor = _LeitorTabelas()
    leitor.feed(html)
    cabecalho = None
    sessoes = []

    for linha in leitor.linhas:
        normalizada = [celula.casefold() for celula in linha]
        if cabecalho is None:
            if (
                any("session" in celula for celula in normalizada)
                and any("date" in celula for celula in normalizada)
                and any("track time" in celula for celula in normalizada)
            ):
                cabecalho = True
            continue

        if len(linha) < 3:
            continue

        nome, texto_data, texto_hora = linha[0], linha[1], linha[2]
        data_match = re.search(
            r"\b(?:mon|tue|wed|thu|fri|sat|sun)\s+(\d{1,2})\s+([a-z]{3})\b",
            texto_data,
            flags=re.I,
        )
        hora_match = re.search(r"\b(\d{1,2}):(\d{2})\b", texto_hora)
        if not data_match or not hora_match:
            continue

        mes = MESES.get(data_match.group(2).casefold())
        if mes is None:
            continue
        data_local = date(ano, mes, int(data_match.group(1)))
        hora_local = datetime.strptime(
            f"{hora_match.group(1)}:{hora_match.group(2)}", "%H:%M"
        ).time()
        instante = datetime.combine(
            data_local, hora_local, tzinfo=ZoneInfo(fuso_local)
        ).astimezone(ZoneInfo("America/Sao_Paulo"))

        nome_normalizado = nome.casefold()
        if nome_normalizado in ("race", "e-prix"):
            nome_exibicao = f"Corrida (Rodada {rodada})"
        elif nome_normalizado.startswith("practice"):
            sufixo = nome[len("Practice"):].strip()
            nome_exibicao = "Treino Livre" + (f" {sufixo}" if sufixo else "")
        elif nome_normalizado == "qualifying":
            nome_exibicao = "Classificação"
        else:
            nome_exibicao = nome

        sessoes.append({
            "nome": nome_exibicao,
            "data": fmt(instante.date()),
            "horario": instante.strftime("%H:%M"),
        })

    if not any(re.search(r"Rodada\s+\d+", s["nome"], re.I) for s in sessoes):
        raise ValueError("Tabela oficial sem sessão de corrida")
    sessoes.sort(key=lambda sessao: (
        datetime.strptime(sessao["data"] + " " + sessao["horario"], "%d/%m/%Y %H:%M")
    ))
    return sessoes


def preencher_sessoes_proximo_evento(etapas, hoje=None, baixar_html=baixar):
    """Preenche as sessões do próximo evento quando a página oficial as publica."""
    hoje = hoje or date.today()
    proximas = [
        etapa for etapa in etapas
        if datetime.strptime(etapa["inicio"], "%d/%m/%Y").date() >= hoje
    ]
    if not proximas:
        print("Não há próximo evento para consultar sessões.")
        return False

    proxima = min(proximas, key=lambda etapa: datetime.strptime(
        etapa["inicio"], "%d/%m/%Y"
    ).date())
    cidade = proxima["circuito"]
    slug = SLUGS_POR_CIDADE.get(cidade)
    fuso = FUSOS_POR_CIDADE.get(cidade)
    if not slug or not fuso:
        print(f"Página/fuso do próximo evento ainda não mapeado: {cidade}")
        return False

    url = f"https://www.fiaformulae.com/en/{slug}"
    try:
        html = baixar_html(url)
        corrida = next(
            (s for s in proxima["sessoes"] if re.search(r"Rodada\s+\d+", s["nome"], re.I)),
            None,
        )
        if corrida is None:
            raise ValueError("Número da rodada não encontrado no calendário")
        rodada = int(re.search(r"Rodada\s+(\d+)", corrida["nome"], re.I).group(1))
        ano = datetime.strptime(proxima["inicio"], "%d/%m/%Y").year
        sessoes = extrair_sessoes_evento(html, ano, fuso, rodada)
        proxima["sessoes"] = sessoes
        # O calendário-base pode trazer apenas o dia da corrida. A etapa precisa
        # abranger também os dias de treinos e classificação publicados depois.
        ultimo_dia = max(
            datetime.strptime(sessao["data"], "%d/%m/%Y").date()
            for sessao in sessoes
        )
        fim_atual = datetime.strptime(proxima["fim"], "%d/%m/%Y").date()
        if ultimo_dia > fim_atual:
            proxima["fim"] = fmt(ultimo_dia)
        print(f"Sessões oficiais encontradas para {cidade}: {len(sessoes)}")
        for sessao in sessoes:
            print(f"  {sessao['data']} {sessao['horario']} - {sessao['nome']}")
        return True
    except Exception as erro:  # noqa: BLE001
        print(f"Sessões do próximo evento ainda não disponíveis ({cidade}): {erro}")
        return False



def lista_do_calendario(html):
    """Localiza ItemList em JSON-LD raiz, lista ou dentro de @graph."""
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )

    def procurar(dados):
        if isinstance(dados, list):
            for item in dados:
                resultado = procurar(item)
                if resultado is not None:
                    return resultado
        elif isinstance(dados, dict):
            elementos = dados.get("itemListElement")
            if isinstance(elementos, list) and elementos:
                return dados
            grafo = dados.get("@graph")
            if isinstance(grafo, (dict, list)):
                return procurar(grafo)
        return None

    for bloco in blocos:
        try:
            dados = json.loads(bloco)
        except ValueError:
            continue
        resultado = procurar(dados)
        if resultado is not None:
            return resultado
    raise ValueError("ItemList do calendário não encontrado")
def extrair_rodadas(dados):
    rodadas = []
    numeros_vistos = set()
    for el in dados["itemListElement"]:
        item = el.get("item") if isinstance(el, dict) else None
        if not isinstance(item, dict):
            raise ValueError("Item de rodada ausente ou inválido")

        nome = item.get("name")
        m = re.match(r"R(\d+)\s+(.+?)\s+E-Prix", nome or "")
        if not m:
            raise ValueError(f"Nome de rodada inesperado: {nome!r}")

        numero = int(m.group(1))
        if numero in numeros_vistos:
            raise ValueError(f"Rodada duplicada na fonte oficial: R{numero}")
        numeros_vistos.add(numero)

        endereco = (item.get("location") or {}).get("address") or {}
        cidade = m.group(2).strip()
        pais_codigo = endereco.get("addressCountry", "")
        pais = PAISES.get(pais_codigo, pais_codigo)
        if not pais:
            raise ValueError(f"País ausente na rodada R{numero}: {nome!r}")

        try:
            data_evento = date.fromisoformat(item["startDate"])
        except (KeyError, TypeError, ValueError) as erro:
            raise ValueError(f"Data inválida na rodada R{numero}: {item.get('startDate')!r}") from erro

        rodadas.append(
            {
                "rodada": numero,
                "cidade": CIDADES.get(cidade, cidade),
                "pais": pais,
                "data": data_evento,
            }
        )

    rodadas.sort(key=lambda r: (r["data"], r["rodada"]))
    return rodadas


def obter_rodadas_confirmadas():
    """Confirma o índice oficial em duas leituras antes de substituir o calendário."""
    primeira = extrair_rodadas(lista_do_calendario(baixar(URL)))
    segunda = extrair_rodadas(lista_do_calendario(baixar(URL)))
    if primeira != segunda:
        raise ValueError("Índice oficial mudou entre leituras; atualização cancelada")
    return segunda


def agrupar_etapas(rodadas):
    """Rodadas seguidas na mesma cidade (até 3 dias) viram uma etapa."""
    grupos = []
    for r in rodadas:
        if (
            grupos
            and grupos[-1][-1]["cidade"] == r["cidade"]
            and (r["data"] - grupos[-1][-1]["data"]).days <= 3
        ):
            grupos[-1].append(r)
        else:
            grupos.append([r])
    return grupos


def rotulo_temporada(rodadas):
    """Monta o rótulo a partir dos anos presentes nas datas da fonte oficial."""
    anos = sorted({r["data"].year for r in rodadas})
    if not anos:
        raise ValueError("Não há datas para determinar a temporada")
    if len(anos) == 1:
        return str(anos[0])
    return f"{anos[0]}/{anos[-1] % 100:02d}"


def montar(grupos):
    etapas = []
    for n, g in enumerate(grupos, start=1):
        etapas.append(
            {
                "etapa": n,
                "circuito": g[0]["cidade"],
                "pais": g[0]["pais"],
                "inicio": fmt(g[0]["data"]),
                "fim": fmt(g[-1]["data"]),
                "sessoes": [
                    {
                        "nome": f"Corrida (Rodada {r['rodada']})",
                        "data": fmt(r["data"]),
                        "horario": "A confirmar",
                    }
                    for r in g
                ],
            }
        )
    return etapas



def main():
    print("Consultando calendário oficial da Fórmula E...")
    try:
        rodadas = obter_rodadas_confirmadas()
    except Exception as e:
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    print("Rodadas encontradas na fonte oficial:", len(rodadas))

    # A fonte oficial define a quantidade: não fixamos um mínimo nem comparamos
    # com a temporada anterior, pois inclusões e remoções podem ser legítimas.
    if not rodadas:
        print("ERRO: a fonte não forneceu nenhuma rodada. Nada foi alterado.")
        return 1

    etapas = montar(agrupar_etapas(rodadas))
    preencher_sessoes_proximo_evento(etapas)
    for e in etapas:
        print(f"  {e['etapa']:>2}  {e['circuito']} ({e['pais']})  {e['inicio']} a {e['fim']}")

    novo = {"indice_confirmado": True, "temporada": rotulo_temporada(rodadas), "categoria": "Formula E", "etapas": etapas}
    if not gravar_se_mudou(SAIDA, novo):
        print("Sem mudanças no calendário.")
        return 0

    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())