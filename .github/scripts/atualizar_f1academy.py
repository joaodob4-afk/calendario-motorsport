"""Atualiza app/src/main/assets/f1academy_calendar.json a partir do site oficial da F1 Academy.

A página do calendário lista as etapas da temporada (links ...Results?raceid=N). Cada etapa
traz, no __NEXT_DATA__, a lista SessionResults com o nome e o horário de cada sessão (com o
fuso do circuito). Converte tudo para o horário de Brasília.
Sessões marcadas como "Unconfirmed" no site ficam com horário "A confirmar".
Se qualquer etapa falhar na validação, NÃO altera o arquivo e sai com erro.
"""
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import (  # noqa: E402
    baixar_texto as baixar,
    gravar_se_mudou,
    ler_existente,
    validar_encolhimento,
)

BASE = "https://www.f1academy.com"
CALENDARIO = BASE + "/Racing-Series/Calendar"
SAIDA = Path("app/src/main/assets/f1academy_calendar.json")
BRASILIA = ZoneInfo("America/Sao_Paulo")
FORMATO = "%d/%m/%Y"

# cidade como o site escreve no título ("Round 1: Shanghai 13-15 March 2026") -> (circuito, país)
CIRCUITOS = {
    "shanghai": ("Xangai", "China"),
    "jeddah": ("Jeddah", "Arábia Saudita"),
    "miami": ("Miami", "Estados Unidos"),
    "montreal": ("Montreal", "Canadá"),
    "silverstone": ("Silverstone", "Reino Unido"),
    "zandvoort": ("Zandvoort", "Holanda"),
    "singapore": ("Marina Bay", "Singapura"),
    "austin": ("Austin", "Estados Unidos"),
    "las vegas": ("Las Vegas", "Estados Unidos"),
    "barcelona": ("Barcelona", "Espanha"),
    "spa-francorchamps": ("Spa-Francorchamps", "Bélgica"),
    "monza": ("Monza", "Itália"),
    "abu dhabi": ("Yas Marina", "Emirados Árabes Unidos"),
}


# País como o site escreve (em inglês) -> português, para cidades fora da tabela acima.
# País que não estiver aqui é gravado como o site escreveu (o app mostra do mesmo jeito).
PAISES = {
    "australia": "Austrália", "austria": "Áustria", "azerbaijan": "Azerbaijão",
    "bahrain": "Bahrein", "belgium": "Bélgica", "brazil": "Brasil", "canada": "Canadá",
    "china": "China", "france": "França", "germany": "Alemanha", "great britain": "Reino Unido",
    "hungary": "Hungria", "italy": "Itália", "japan": "Japão", "malaysia": "Malásia",
    "mexico": "México", "monaco": "Mônaco", "netherlands": "Holanda", "qatar": "Catar",
    "saudi arabia": "Arábia Saudita", "singapore": "Singapura", "spain": "Espanha",
    "united arab emirates": "Emirados Árabes Unidos", "united kingdom": "Reino Unido",
    "united states": "Estados Unidos", "usa": "Estados Unidos",
}


def ids_das_etapas(html):
    """raceids da página de calendário (a temporada atual), em ordem crescente."""
    return sorted({int(i) for i in re.findall(r"raceid=(\d+)", html)})


def dados_da_pagina(html):
    """(cidade, país do site, lista de sessões brutas) tirados da página de uma etapa."""
    titulo = re.search(r"<title>(.*?)</title>", html, flags=re.S)
    cidade = re.search(r"Round\s+\d+:\s*(.+?)\s+\d{1,2}\b", titulo.group(1) if titulo else "")
    if not cidade:
        raise ValueError("título da página sem cidade")
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, flags=re.S)
    if not m:
        raise ValueError("página sem __NEXT_DATA__")
    page = json.loads(m.group(1))["props"]["pageProps"]["pageData"]
    sessoes = page.get("SessionResults") or []
    if not sessoes:
        raise ValueError("nenhuma sessão em SessionResults")
    return cidade.group(1).strip(), str(page.get("CountryName") or "").strip(), sessoes


def nome_da_sessao(sessao):
    """Nome em português, pelo nome que o site usa."""
    nome = (sessao.get("SessionName") or "").strip()
    baixo = nome.lower()
    if baixo.startswith("free practice") or baixo.startswith("practice"):
        return "Treino Livre"
    if baixo.startswith("qualifying"):
        return "Classificação"
    if "opening" in baixo:
        return "Corrida de Abertura"  # a corrida extra das etapas com três corridas
    if "reverse" in baixo:
        return "Corrida Grid Invertido"
    if "feature" in baixo:
        return "Corrida Feature"
    # Nome usado em 2025 (Race 1 / Race 2): a 1ª é a de grid invertido, a 2ª a Feature.
    if baixo == "race 1":
        return "Corrida Grid Invertido"
    if baixo == "race 2":
        return "Corrida Feature"
    print(f"AVISO: sessão desconhecida {nome!r}; mantendo o nome original")
    return nome or "Sessão"


def montar_etapa(html):
    cidade, pais_do_site, brutas = dados_da_pagina(html)
    if cidade.lower() in CIRCUITOS:
        circuito, pais = CIRCUITOS[cidade.lower()]
    else:
        # Cidade nova: usa o que o site informa, em vez de recusar a atualização.
        if not pais_do_site:
            raise ValueError(f"{cidade}: cidade fora da tabela e página sem país")
        circuito = cidade
        pais = PAISES.get(pais_do_site.lower(), pais_do_site)
        print(f"AVISO: cidade fora da tabela: {cidade!r} ({pais}); gravando como o site informa")

    itens = []
    for s in brutas:
        inicio = datetime.fromisoformat(s["SessionStartTime"])  # mantém o fuso do circuito
        itens.append((inicio, nome_da_sessao(s), bool(s.get("Unconfirmed"))))
    itens.sort(key=lambda x: x[0])

    sessoes = []
    for inicio, nome, nao_confirmada in itens:
        if nao_confirmada:
            # Horário provisório do site: vale só o dia (no horário local do circuito).
            data, horario = inicio.strftime(FORMATO), "A confirmar"
        else:
            em_brasilia = inicio.astimezone(BRASILIA)
            data, horario = em_brasilia.strftime(FORMATO), em_brasilia.strftime("%H:%M")
        sessao = {"nome": nome, "data": data, "horario": horario}
        # "Qualifying 1" e "Qualifying 2" (grupos) vêm no mesmo horário: mostra uma só vez.
        if sessao not in sessoes:
            sessoes.append(sessao)

    return {
        "_inicio": itens[0][0],
        "circuito": circuito,
        "pais": pais,
        "inicio": itens[0][0].strftime(FORMATO),  # dias no calendário local do circuito
        "fim": itens[-1][0].strftime(FORMATO),
        "sessoes": sessoes,
    }


def montar_calendario(etapas):
    """Ordena, numera e valida. Levanta ValueError se algo estiver fora do esperado."""
    if not etapas:
        raise ValueError("nenhuma etapa")
    etapas = sorted(etapas, key=lambda e: e["_inicio"].astimezone(BRASILIA))
    anos = {e["_inicio"].year for e in etapas}
    if len(anos) != 1:
        raise ValueError(f"etapas de mais de uma temporada: {sorted(anos)}")
    saida = []
    for n, e in enumerate(etapas, start=1):
        if datetime.strptime(e["fim"], FORMATO) < datetime.strptime(e["inicio"], FORMATO):
            raise ValueError(f"{e['circuito']}: fim antes do início")
        saida.append({
            "etapa": n,
            "circuito": e["circuito"],
            "pais": e["pais"],
            "inicio": e["inicio"],
            "fim": e["fim"],
            "sessoes": e["sessoes"],
        })
    return {"temporada": anos.pop(), "categoria": "F1 Academy", "etapas": saida}


def main():
    print("Consultando o site oficial da F1 Academy...")
    try:
        ids = ids_das_etapas(baixar(CALENDARIO))
    except Exception as e:  # noqa: BLE001
        print("ERRO ao baixar o calendário:", e)
        print("Nada foi alterado.")
        return 1
    print("Etapas encontradas:", ids)
    if not ids:
        print("ERRO: nenhuma etapa encontrada. Nada foi alterado.")
        return 1

    etapas = []
    for raceid in ids:
        try:
            etapas.append(montar_etapa(baixar(f"{BASE}/Racing-Series/Results?raceid={raceid}")))
            print("OK  ", raceid)
        except Exception as e:  # noqa: BLE001
            print("ERRO", raceid, "->", e)
            print("Nada foi alterado.")
            return 1

    try:
        novo = montar_calendario(etapas)
        validar_encolhimento(novo, ler_existente(SAIDA), "F1 Academy")
    except ValueError as e:
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    if not gravar_se_mudou(SAIDA, novo):
        print("Sem mudanças no calendário.")
        return 0
    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())