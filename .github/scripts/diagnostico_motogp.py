"""Diagnóstico da API de calendário da MotoGP.

Só LÊ a API e imprime a estrutura dos dados (MotoGP, Moto2 e Moto3) no log; não altera nenhum arquivo do app.
Serve para confirmar os nomes dos campos antes de escrever o atualizador.

Uso: python .github/scripts/diagnostico_motogp.py [ano]
"""
import json
import sys
import urllib.error
import urllib.request
from collections import Counter

# O mesmo serviço aparece documentado com os dois nomes de servidor; tenta os dois.
SERVIDORES = [
    "https://api.motogp.pulselive.com",
    "https://api.pulselive.motogp.com",
]
CAMINHO = "/motogp/v1/events?seasonYear={ano}"

# Siglas das classes na API: MotoGP, Moto2 e Moto3.
CLASSES = ["MGP", "MT2", "MT3"]

CABECALHOS = [
    {"User-Agent": "Mozilla/5.0", "Accept": "application/json"},
    {
        "User-Agent": "Mozilla/5.0",
        "Accept": "application/json",
        "Origin": "https://www.motogp.com",
        "Referer": "https://www.motogp.com/",
    },
]


def baixar(url, cabecalhos):
    req = urllib.request.Request(url, headers=cabecalhos)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:  # noqa: BLE001
        print("  ERRO:", e)
        return None, ""


def obter(ano):
    """Retorna (url, lista_de_eventos) da primeira combinação que funcionar."""
    for servidor in SERVIDORES:
        url = servidor + CAMINHO.format(ano=ano)
        for numero, cab in enumerate(CABECALHOS, start=1):
            status, texto = baixar(url, cab)
            print(f"{url} | cabeçalhos #{numero} | status {status} | {len(texto)} bytes")
            if status == 200 and texto:
                try:
                    dados = json.loads(texto)
                except ValueError:
                    print("  resposta não é JSON")
                    continue
                if isinstance(dados, list) and dados:
                    return url, dados
                print("  JSON sem lista de eventos:", type(dados).__name__)
    return None, None


def sessoes_da_classe(evento, sigla):
    return [
        b
        for b in evento.get("broadcasts") or []
        if ((b.get("category") or {}).get("acronym") or "").upper() == sigla
    ]


def resumir(eventos):
    print()
    print("##### RESUMO #####")
    print("eventos:", len(eventos))
    print("por kind:", dict(Counter(e.get("kind") for e in eventos)))
    print("por status:", dict(Counter(e.get("status") for e in eventos)))
    print("campos do evento:", sorted(eventos[0].keys()))

    siglas = Counter()
    for e in eventos:
        for b in e.get("broadcasts") or []:
            siglas[(b.get("category") or {}).get("acronym")] += 1
    print("sessões por sigla de classe (todas as etapas):", dict(siglas))

    print()
    print("##### ETAPAS (kind GP) #####")
    for e in eventos:
        if e.get("kind") != "GP":
            continue
        circuito = e.get("circuit") or {}
        print(
            e.get("sequence"),
            "|", e.get("shortname"),
            "|", e.get("name"),
            "|", e.get("country"),
            "|", circuito.get("name"),
            "|", circuito.get("city"),
            "|", e.get("time_zone"),
            "|", str(e.get("date_start"))[:10], "->", str(e.get("date_end"))[:10],
            "|", e.get("status"),
            "| classes:", [c.get("acronym") for c in e.get("categories") or []],
        )

    gps = [e for e in eventos if e.get("kind") == "GP"]
    if not gps:
        print("(nenhum evento com kind GP)")
        return

    primeiro = gps[0]

    for sigla in CLASSES:
        print()
        print("##### SESSÕES", sigla, "NA PRIMEIRA ETAPA:", primeiro.get("name"), "#####")
        for b in sessoes_da_classe(primeiro, sigla):
            print(
                b.get("date_start"),
                "->", b.get("date_end"),
                "|", b.get("shortname"),
                "|", b.get("name"),
                "|", b.get("type"),
                "|", b.get("kind"),
                "|", b.get("status"),
                "| dia", b.get("gp_day"),
            )

    for sigla in CLASSES:
        print()
        print("##### TIPOS DE SESSÃO", sigla, "EM TODAS AS ETAPAS #####")
        contagem = Counter()
        for e in gps:
            for b in sessoes_da_classe(e, sigla):
                contagem[(b.get("type"), b.get("kind"), b.get("shortname"), b.get("name"))] += 1
        for chave, qtd in sorted(contagem.items(), key=lambda x: str(x[0])):
            print(qtd, "x", chave)

    amostra = sessoes_da_classe(primeiro, "MGP")
    if amostra:
        print()
        print("##### UMA SESSÃO CRUA (JSON) #####")
        print(json.dumps(amostra[0], ensure_ascii=False)[:1500])


def main():
    ano = int(sys.argv[1]) if len(sys.argv) > 1 else 2026
    url, eventos = obter(ano)
    if not eventos:
        print("FALHA: não foi possível ler os eventos da MotoGP.")
        sys.exit(1)

    with open(f"motogp_events_{ano}.json", "w", encoding="utf-8") as f:
        json.dump(eventos, f, ensure_ascii=False)

    print("OK:", url)
    resumir(eventos)


if __name__ == "__main__":
    main()
