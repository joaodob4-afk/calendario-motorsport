"""Atualiza app/src/main/assets/indycar_calendar.json a partir do site oficial
da IndyCar (indycar.com/Schedule + pagina de cada etapa).

Horarios do site estao em ET (America/New_York); o JSON sai em horario de
Brasilia. Se a validacao falhar, NAO altera o arquivo e sai com erro.
"""
import html as htmllib
import re
import sys
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo
sys.path.insert(0, str(Path(__file__).resolve().parent))
from utilitarios import baixar_texto, formatar_data as fmt, gravar_se_mudou  # noqa: E402


def baixar(url):
    return baixar_texto(url, tentativas=3)

BASE = "https://www.indycar.com"
LISTA = BASE + "/Schedule"
SAIDA = Path("app/src/main/assets/indycar_calendar.json")

NY = ZoneInfo("America/New_York")
BRASILIA = ZoneInfo("America/Sao_Paulo")

MESES = {
    m: i
    for i, m in enumerate(
        ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
        start=1,
    )
}

PROVINCIAS_CANADA = {"Ontario", "Quebec", "British Columbia", "Alberta", "Manitoba"}
PAISES_INTERNACIONAIS = {"Spain": "Espanha"}
PREFIXO = "NTT INDYCAR SERIES - "


def validar_cartoes_fonte(cartoes):
    """Aceita a quantidade publicada pela fonte, mas nunca uma lista vazia."""
    if not cartoes:
        raise ValueError("Nenhuma etapa encontrada na fonte oficial")
    return cartoes


def obter_cartoes_confirmados():
    """Confirma que duas leituras do índice oficial apresentam os mesmos cartões."""
    primeira = validar_cartoes_fonte(extrair_cartoes(baixar(LISTA)))
    segunda = validar_cartoes_fonte(extrair_cartoes(baixar(LISTA)))
    if primeira != segunda:
        raise ValueError("Índice oficial mudou entre leituras; atualização cancelada")
    return segunda


def linhas_de_texto(trecho):
    trecho = re.sub(
        r"<script[\s\S]*?</script>|<style[\s\S]*?</style>|<svg[\s\S]*?</svg>", "", trecho
    )
    t = re.sub(r"<[^>]+>", "\n", trecho)
    t = htmllib.unescape(t).replace("\xa0", " ")
    linhas = [re.sub(r"\s+", " ", l).strip() for l in t.split("\n")]
    return [l for l in linhas if l]


def extrair_cartoes(html):
    """Retorna {slug: dados_do_cartao} da temporada mais recente da lista."""
    padrao = re.compile(r'<a href="(/Schedule/(\d{4})/([^"/?#]+))"[^>]*class="event-card-link"')
    achados = list(padrao.finditer(html))
    if not achados:
        raise ValueError("Nenhum cartão de etapa encontrado")

    ano = max(int(m.group(2)) for m in achados)
    cartoes = {}

    for i, m in enumerate(achados):
        if int(m.group(2)) != ano:
            continue
        slug = m.group(3)
        if slug in cartoes:
            continue

        fim = achados[i + 1].start() if i + 1 < len(achados) else m.end() + 8000
        bloco = html[m.start() : min(fim, m.start() + 12000)]

        def pegar(classe):
            r = re.search(r'class="' + classe + r'"[^>]*>(.*?)</', bloco, re.S)
            return htmllib.unescape(r.group(1)).strip() if r else ""

        # No site, a data e a hora de cada cartão aparecem ANTES do link do
        # próprio cartão (ou seja, no fim do trecho do cartão anterior).
        inicio_trecho = achados[i - 1].start() if i > 0 else max(0, m.start() - 8000)
        anterior = linhas_de_texto(html[inicio_trecho : m.start()])

        data = hora = None
        for l in reversed(anterior):
            mm = re.fullmatch(r"([A-Z][a-z]{2}) (\d{1,2})", l)
            if mm and mm.group(1) in MESES and data is None:
                data = (MESES[mm.group(1)], int(mm.group(2)))
            mh = re.fullmatch(r"(\d{1,2}):(\d{2}) ?([AP]M) ET", l)
            if mh and hora is None:
                hora = (int(mh.group(1)), int(mh.group(2)), mh.group(3))
            if data and hora:
                break

        cartoes[slug] = {
            "slug": slug,
            "ano": ano,
            "titulo": pegar("event-card-title"),
            "pista": pegar("event-card-track-name"),
            "local": pegar("event-card-track-location"),
            "data": data,
            "hora": hora,
        }
    return cartoes


def converter_hora(ano, mes, dia, h, m, ampm):
    h = h % 12 + (12 if ampm.upper() == "PM" else 0)
    return datetime(ano, mes, dia, h, m, tzinfo=NY)


def traduzir(nome):
    n = nome.strip()
    m = re.fullmatch(r"Practice (\d+)", n)
    if m:
        return f"Treino Livre {m.group(1)}"
    if n in ("Qualifications", "Qualification"):
        return "Classificação"
    if n.startswith("Qualifying"):
        return "Classificação" + n[len("Qualifying"):]
    if n == "Race":
        return "Corrida"
    m = re.fullmatch(r"Race (\d+)", n)
    if m:
        return f"Corrida {m.group(1)}"
    if n == "Practice":
        return "Treino Livre"
    if n == "Final Practice":
        return "Treino Final"
    if n == "High Line & Final Practice":
        return "Treino Final (High Line)"
    if n == "Warmup":
        return "Warm-up"
    if n == "Carb Day Final Practice":
        return "Treino Final (Carb Day)"
    if n == "Pit Stop Competition":
        return "Competição de Pit Stop"
    return n


def nome_de_sessao_publicada(nome):
    """Reconhece rótulos de sessão da página oficial mesmo sem o prefixo da série."""
    return bool(re.fullmatch(
        r"(?:Practice(?: \d+)?|Qualifications?|Qualifying.*|Race(?: \d+)?|"
        r"Final Practice|High Line & Final Practice|Warmup|"
        r"Carb Day Final Practice|Pit Stop Competition)",
        nome.strip(),
    ))


def extrair_sessoes(html, ano):
    linhas = linhas_de_texto(html)

    # O site oficial passou a usar "Toggle Schedule" nas páginas atuais;
    # versões antigas usavam "Download Weekend Schedule". Aceitamos ambos.
    ini = next((
        i for i, l in enumerate(linhas)
        if l in ("Download Weekend Schedule", "Toggle Schedule")
    ), None)
    if ini is None:
        return []
    fim = next(
        (i for i, l in enumerate(linhas[ini:], start=ini) if l in ("Find Local Radio Station", "Toggle Event Details", "Event Details")),
        len(linhas),
    )

    sessoes = []
    dia = None
    i = ini + 1
    while i < fim:
        l = linhas[i]
        md = re.fullmatch(r"[A-Za-z]+, ([A-Z][a-z]{2}) (\d{1,2})", l)
        if md and md.group(1) in MESES:
            dia = (MESES[md.group(1)], int(md.group(2)))
            i += 1
            continue

        mh = re.fullmatch(r"(\d{1,2}):(\d{2}) ?([AP]M) ET", l)
        if mh and dia and i + 1 < fim:
            desc = linhas[i + 1]
            mp = re.match(r"^(?:NTT )?INDYCAR SERIES\s*[-–]\s*(.+)$", desc)
            if mp:
                nome_bruto = mp.group(1)
            elif nome_de_sessao_publicada(desc):
                # Algumas páginas oficiais exibem os rótulos sem o prefixo da série.
                nome_bruto = desc
            else:
                i += 1
                continue

            nome = traduzir(nome_bruto)
            if nome:
                dt = converter_hora(ano, dia[0], dia[1], int(mh.group(1)), int(mh.group(2)), mh.group(3))
                sessoes.append((nome, dt, dia))
            i += 2
            continue
        i += 1

    # Remove repetições (mesmo dia e mesmo nome): fica a mais cedo.
    vistos = set()
    unicas = []
    for nome, dt, dia in sorted(sessoes, key=lambda s: s[1]):
        chave = (dia, nome)
        if chave in vistos:
            continue
        vistos.add(chave)
        unicas.append((nome, dt, dia))
    return unicas


def filtrar_corrida_da_etapa(cartao, sessoes):
    """Se a página do fim de semana lista as duas corridas, associa cada uma ao cartão correto."""
    slug = str(cartao.get("slug", "")).casefold().replace("-", "")
    correspondencia = re.search(r"race([12])$", slug)
    if not correspondencia:
        return sessoes

    corrida_da_etapa = f"Corrida {correspondencia.group(1)}"
    corridas_publicadas = {
        nome for nome, _, _ in sessoes
        if nome in ("Corrida 1", "Corrida 2")
    }
    if corridas_publicadas != {"Corrida 1", "Corrida 2"}:
        return sessoes

    return [
        sessao for sessao in sessoes
        if sessao[0] not in ("Corrida 1", "Corrida 2") or sessao[0] == corrida_da_etapa
    ]


def filtrar_pre_race_indianapolis(cartao, sessoes):
    """Remove Pre-Race somente da 110th Indianapolis 500."""
    titulo = str(cartao.get("titulo", "")).strip().casefold()
    if titulo != "110th running of the indianapolis 500":
        return sessoes
    return [sessao for sessao in sessoes if sessao[0].casefold() != "pre-race"]



def separar_local_e_pais(local):
    """Separa os países internacionais conhecidos sem alterar locais domésticos."""
    partes = [parte.strip() for parte in str(local).split(",") if parte.strip()]
    if len(partes) < 2:
        return str(local).strip(), "Estados Unidos"

    regiao = partes[-1]
    if regiao in PROVINCIAS_CANADA:
        return ", ".join(partes), "Canadá"
    if regiao in PAISES_INTERNACIONAIS:
        return ", ".join(partes[:-1]), PAISES_INTERNACIONAIS[regiao]

    # Os demais locais publicados no calendário são tratados como locais dos EUA.
    return ", ".join(partes), "Estados Unidos"


def montar_etapa(cartao, html_evento):
    ano = cartao["ano"]
    sessoes = filtrar_corrida_da_etapa(cartao, extrair_sessoes(html_evento, ano))
    sessoes = filtrar_pre_race_indianapolis(cartao, sessoes)

    if sessoes:
        dias = [datetime(ano, d[0], d[1]) for _, _, d in sessoes]
        inicio, fim = min(dias), max(dias)
        lista = [
            {
                "nome": nome,
                "data": fmt(dt.astimezone(BRASILIA)),
                "horario": dt.astimezone(BRASILIA).strftime("%H:%M"),
            }
            for nome, dt, _ in sessoes
        ]
    else:
        # Programação ainda não publicada: usa a data da corrida do cartão.
        if not cartao["data"]:
            raise ValueError(f"{cartao['slug']}: sem programação e sem data no cartão")
        mes, dia = cartao["data"]
        inicio = fim = datetime(ano, mes, dia)
        if cartao["hora"]:
            h, m, ap = cartao["hora"]
            dt = converter_hora(ano, mes, dia, h, m, ap).astimezone(BRASILIA)
            lista = [{"nome": "Corrida", "data": fmt(dt), "horario": dt.strftime("%H:%M")}]
        else:
            lista = [{"nome": "Corrida", "data": fmt(inicio), "horario": "A confirmar"}]

    local, pais = separar_local_e_pais(cartao["local"])

    if not cartao["titulo"]:
        raise ValueError(f"{cartao['slug']}: título ausente")

    return {
        "_ordem": inicio,
        "nome": cartao["titulo"],
        "circuito": cartao["pista"] or cartao["titulo"],
        "local": local,
        "pais": pais,
        "inicio": fmt(inicio),
        "fim": fmt(fim),
        "sessoes": lista,
    }


def main():
    print("Consultando páginas oficiais da IndyCar...")
    try:
        cartoes = obter_cartoes_confirmados()
    except Exception as e:  # noqa: BLE001
        print("ERRO:", e)
        print("Nada foi alterado.")
        return 1

    print("Etapas encontradas:", len(cartoes))

    etapas = []
    for slug, cartao in cartoes.items():
        try:
            html_evento = baixar(f"{BASE}/Schedule/{cartao['ano']}/{slug}")
            etapas.append(montar_etapa(cartao, html_evento))
            print("OK  ", slug)
        except Exception as e:  # noqa: BLE001
            print("ERRO", slug, "->", e)
            print("Nada foi alterado.")
            return 1

    etapas.sort(key=lambda e: e["_ordem"])
    saida = []
    for n, e in enumerate(etapas, start=1):
        e.pop("_ordem")
        e["etapa"] = n
        saida.append(
            {k: e[k] for k in ("etapa", "nome", "circuito", "local", "pais", "inicio", "fim", "sessoes")}
        )

    ano = next(iter(cartoes.values()))["ano"]
    novo = {"indice_confirmado": True, "temporada": ano, "categoria": "IndyCar", "etapas": saida}
    if not gravar_se_mudou(SAIDA, novo):
        print("Sem mudanças no calendário.")
        return 0

    print("Calendário atualizado:", SAIDA)
    return 0


if __name__ == "__main__":
    sys.exit(main())