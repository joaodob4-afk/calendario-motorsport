"""Atualiza app/src/main/assets/fe_calendar.json a partir do site oficial da
Formula E (JSON-LD "ItemList" da pagina /en/calendar).

A pagina traz so datas (sem horarios de sessao); as sessoes ficam como
"A confirmar". Se a validacao falhar, NAO altera o arquivo e sai com erro.
"""
import json
import re
import sys
import urllib.request
from datetime import date
from pathlib import Path

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
    "London": "Londres",
    "Shanghai": "Xangai",
    "Tokyo": "Tóquio",
}


def baixar(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0", "Accept-Language": "en-US,en;q=0.9"},
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace")


def lista_do_calendario(html):
    blocos = re.findall(
        r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', html, flags=re.S
    )
    for b in blocos:
        try:
            dados = json.loads(b)
        except ValueError:
            continue
        if isinstance(dados, dict) and dados.get("itemListElement"):
            return dados
    raise ValueError("ItemList do calendário não encontrado")


def extrair_rodadas(dados):
    rodadas = []
    for el in dados["itemListElement"]:
        item = el["item"]
        nome = item["name"]
        m = re.match(r"R(\d+)\s+(.+?)\s+E-Prix", nome)
        if not m:
            raise ValueError(f"Nome de rodada inesperado: {nome!r}")

        endereco = (item.get("location") or {}).get("address") or {}
        cidade = m.group(2).strip()
        pais_codigo = endereco.get("addressCountry", "")

        rodadas.append(
            {
                "rodada": int(m.group(1)),
                "cidade": CIDADES.get(cidade, cidade),
                "pais": PAISES.get(pais_codigo, pais_codigo),
                "data": date.fromisoformat(item["startDate"]),
            }
        )

    rodadas.sort(key=lambda r: (r["data"], r["rodada"]))
    return rodadas


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


def fmt(d):
    return d.strftime("%d/%m/%Y")


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
        dados = lista_do_calendario(baixar(URL))
        rodadas = extrair_rodadas(dados)
    except Exception as e:
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    esperado = dados.get("numberOfItems")
    print("Rodadas encontradas:", len(rodadas), "| esperado:", esperado)

    if len(rodadas) < 10 or (esperado and esperado != len(rodadas)):
        print("ERRO: quantidade de rodadas inesperada. Nada foi alterado.")
        return 1

    etapas = montar(agrupar_etapas(rodadas))
    for e in etapas:
        print(f"  {e['etapa']:>2}  {e['circuito']} ({e['pais']})  {e['inicio']} a {e['fim']}")

    novo = {"temporada": "2026/27", "categoria": "Formula E", "etapas": etapas}
    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())