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
    "London": "Brands Hatch",
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
    """Localiza ItemList em JSON-LD raiz, lista ou dentro de @graph."""
    blocos = re.findall(
        r'<script[^>]*application/ld\\+json[^>]*>(.*?)</script>', html, flags=re.S
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



def preservar_detalhes_conhecidos(etapas_novas, arquivo=SAIDA, temporada_nova="2026/27"):
    """Preserva horários conhecidos para eventos que continuam no calendário oficial.

    A lista oficial já é validada em main() antes desta função ser chamada.
    Por isso, a ausência de uma sessão pode representar uma remoção oficial:
    não deve bloquear a atualização. Os dados são associados ao evento (cidade,
    país e data), não ao número da rodada, que pode mudar após renumeração.
    """
    if not arquivo.exists():
        return etapas_novas

    try:
        atual = json.loads(arquivo.read_text(encoding="utf-8"))
    except (OSError, ValueError) as erro:
        raise ValueError("Não foi possível validar o calendário anterior; arquivo não alterado") from erro

    if str(atual.get("temporada", "")).strip() != str(temporada_nova).strip():
        return etapas_novas

    def conhecido(valor):
        normalizado = str(valor or "").strip()
        return bool(normalizado) and normalizado.lower() not in {
            "a confirmar", "tbd", "null", "none"
        }

    def normalizar_circuito(valor):
        # Compatibilidade com o JSON anterior, que identificava Brands Hatch
        # apenas como "Londres".
        normalizado = str(valor or "").strip().casefold()
        return "brands hatch" if normalizado == "londres" else normalizado

    sessoes_anteriores = {}
    for etapa in atual.get("etapas", []):
        for sessao in etapa.get("sessoes", []):
            chave = (
                normalizar_circuito(etapa.get("circuito", "")),
                str(etapa.get("pais", "")).strip().casefold(),
                str(sessao.get("data", "")).strip(),
            )
            if all(chave):
                sessoes_anteriores[chave] = sessao

    for etapa in etapas_novas:
        for sessao in etapa.get("sessoes", []):
            chave = (
                str(etapa.get("circuito", "")).strip().casefold(),
                str(etapa.get("pais", "")).strip().casefold(),
                str(sessao.get("data", "")).strip(),
            )
            sessao_atual = sessoes_anteriores.get(chave)
            if sessao_atual is None:
                continue

            # A data é parte da identidade do evento; preservamos somente campos
            # pendentes, nunca substituímos valores publicados pela fonte oficial.
            valor_atual = sessao_atual.get("horario", "")
            if conhecido(valor_atual) and not conhecido(sessao.get("horario", "")):
                sessao["horario"] = valor_atual

    return etapas_novas

def main():
    print("Consultando calendário oficial da Fórmula E...")
    try:
        dados = lista_do_calendario(baixar(URL))
        rodadas = extrair_rodadas(dados)
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
    for e in etapas:
        print(f"  {e['etapa']:>2}  {e['circuito']} ({e['pais']})  {e['inicio']} a {e['fim']}")

    novo = {"temporada": "2026/27", "categoria": "Formula E", "etapas": etapas}
    try:
        novo["etapas"] = preservar_detalhes_conhecidos(
            novo["etapas"], temporada_nova=novo["temporada"]
        )
    except ValueError as e:
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    texto = json.dumps(novo, ensure_ascii=False, indent=2) + "\n"

    if SAIDA.exists() and SAIDA.read_text(encoding="utf-8") == texto:
        print("Sem mudanças no calendário.")
        return 0

    SAIDA.write_text(texto, encoding="utf-8")
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())