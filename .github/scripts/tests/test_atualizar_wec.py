"""Testes offline do atualizador do WEC e do JSON embutido no app."""
import importlib.util
import json
import re
import unittest
from datetime import date, datetime, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location(
    "atualizar_wec", RAIZ / ".github" / "scripts" / "atualizar_wec.py"
)
wec = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(wec)

ASSET = RAIZ / "app" / "src" / "main" / "assets" / "wec_calendar.json"


def pagina(titulo, periodo, nome_evento, sessoes, menu=""):
    ld = {
        "@context": "https://schema.org",
        "@type": "SportsEvent",
        "name": nome_evento,
        "location": {"name": "Barcelona", "address": "Barcelona, ESP"},
        "subEvent": [
            {"@type": "SportsEvent", "name": nome, "startDate": inicio}
            for nome, inicio in sessoes
        ],
    }
    return (
        "<html><head><script type=\"application/ld+json\">"
        + json.dumps(ld)
        + "</script></head><body>"
        + f"<nav>{menu}</nav><h1>{titulo}</h1><p>{periodo}</p></body></html>"
    )


SESSOES_BARCELONA = [
    ("Free Practice 1 - 6 Hours of Barcelona", "2026-10-16T11:30:00+02:00"),
    ("Free Practice 3 - LMGT3 - 6 Hours of Barcelona", "2026-10-17T08:50:00+02:00"),
    ("Qualifying - HYPERCAR - 6 Hours of Barcelona", "2026-10-17T15:40:00+02:00"),
    ("Hyperpole - HYPERCAR - 6 Hours of Barcelona", "2026-10-17T16:00:00+02:00"),
    ("Race - 6 Hours of Barcelona", "2026-10-18T12:00:00+02:00"),
]

HTML_BARCELONA = pagina(
    "6 Hours of Barcelona presented by Visit Qatar",
    "From 16 to 18 October 2026",
    "WEC 6 Hours of Barcelona 2026",
    SESSOES_BARCELONA,
    menu="From 1 to 3 January 2020",  # texto de menu/carrossel que deve ser ignorado
)


class AtualizadorWecTest(unittest.TestCase):
    def test_descobre_etapas_e_ignora_prologue_e_outros_links(self):
        html = """
            <a href="https://www.fiawec.com/en/race/official-prologue-imola-2026">P</a>
            <a href="https://www.fiawec.com/en/race/6-hours-of-imola-2026">I</a>
            <a href="/en/race/6-hours-of-imola-2026">repetido</a>
            <a href="https://www.fiawec.com/en/race/qatar-1812km-2027">Q</a>
            <a href="https://www.fiawec.com/en/race/summary/4948">resumo</a>
            <a href="https://www.fiawec.com/en/race/calendar/5065">calendário</a>
        """
        self.assertEqual(
            wec.descobrir_corridas(html), ["6-hours-of-imola-2026", "qatar-1812km-2027"]
        )

    def test_traduz_nomes_das_sessoes(self):
        curto = "6 Hours of Barcelona"
        casos = {
            "Free Practice 1 - 6 Hours of Barcelona": "Treino Livre 1",
            "Free Practice 3 - LMGT3 - 6 Hours of Barcelona": "Treino Livre 3 LMGT3",
            "Qualifying - HYPERCAR - 6 Hours of Barcelona": "Classificação Hypercar",
            "Hyperpole - LMGT3 - 6 Hours of Barcelona": "Hyperpole LMGT3",
            "Race - 6 Hours of Barcelona": "Corrida",
            "Hyperpole 1 - HYPERCAR - 6 Hours of Barcelona": "Hyperpole 1 Hypercar",
        }
        for original, esperado in casos.items():
            with self.subTest(original=original):
                self.assertEqual(wec.traduzir_sessao(original, curto), esperado)

    def test_remove_nome_da_etapa_mesmo_escrito_diferente(self):
        self.assertEqual(
            wec.traduzir_sessao("Free Practice 2 - TotalEnergies 6 Hours of Spa-Francorchamps", ""),
            "Treino Livre 2",
        )

    def test_nome_curto_do_evento(self):
        self.assertEqual(
            wec.nome_curto_do_evento("WEC 6 Hours of Barcelona 2026"), "6 Hours of Barcelona"
        )

    def test_le_etapa_convertendo_para_brasilia(self):
        etapa = wec.ler_etapa("6-hours-of-barcelona-2026", HTML_BARCELONA)
        sessoes = {s["nome"]: s for s in etapa["sessoes"]}

        # 11:30 em Barcelona (UTC+2) são 06:30 em Brasília (UTC-3).
        self.assertEqual(sessoes["Treino Livre 1"]["data"], "16/10/2026")
        self.assertEqual(sessoes["Treino Livre 1"]["horario"], "06:30")
        self.assertEqual(sessoes["Corrida"]["data"], "18/10/2026")
        self.assertEqual(sessoes["Corrida"]["horario"], "07:00")
        self.assertEqual(
            [s["nome"] for s in etapa["sessoes"]],
            [
                "Treino Livre 1",
                "Treino Livre 3 LMGT3",
                "Classificação Hypercar",
                "Hyperpole Hypercar",
                "Corrida",
            ],
        )

    def test_periodo_vem_do_texto_logo_depois_do_titulo(self):
        etapa = wec.ler_etapa("6-hours-of-barcelona-2026", HTML_BARCELONA)

        self.assertEqual((etapa["inicio"], etapa["fim"]), ("16/10/2026", "18/10/2026"))
        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Barcelona", "Espanha"))

    def test_periodo_que_cruza_o_mes(self):
        html = pagina("Teste", "From 29 May to 1 June 2027", "WEC Teste 2027", [])
        etapa = wec.ler_etapa("6-hours-of-monza-2027", html)

        self.assertEqual((etapa["inicio"], etapa["fim"]), ("29/05/2027", "01/06/2027"))

    def test_corrida_de_24_horas_termina_no_dia_seguinte(self):
        html = pagina(
            "24 Hours of Le Mans",
            "From 10 to 14 June 2026",
            "WEC 24 Hours of Le Mans 2026",
            [("Race - 24 Hours of Le Mans", "2026-06-13T16:00:00+02:00")],
        )
        etapa = wec.ler_etapa("24-hours-of-le-mans-2026", html)

        self.assertEqual((etapa["inicio"], etapa["fim"]), ("10/06/2026", "14/06/2026"))
        self.assertEqual(etapa["circuito"], "Circuit de la Sarthe")

    def test_sem_horarios_publicados_a_etapa_fica_sem_sessoes(self):
        html = (
            "<html><body><h1>Qatar 1812km</h1><p>From 25 to 27 March 2027</p></body></html>"
        )
        etapa = wec.ler_etapa("qatar-1812km-2027", html)

        self.assertEqual(etapa["sessoes"], [])
        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Lusail", "Catar"))

    def test_sem_datas_na_pagina_gera_erro(self):
        with self.assertRaises(ValueError):
            wec.ler_etapa("6-hours-of-monza-2026", "<html><body><h1>Monza</h1></body></html>")

    def test_etapa_nova_usa_dados_da_pagina(self):
        html = pagina(
            "6 Hours of Novolandia", "From 1 to 3 May 2027", "WEC 6 Hours of Novolandia 2027", []
        )
        etapa = wec.ler_etapa("6-hours-of-novolandia-2027", html)

        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Barcelona", "Espanha"))

    def test_sessao_sem_fuso_e_ignorada(self):
        html = pagina(
            "Teste",
            "From 1 to 3 May 2027",
            "WEC Teste 2027",
            [("Race - Teste", "2027-05-03T12:00:00")],
        )
        self.assertEqual(wec.ler_etapa("x-2027", html)["sessoes"], [])

    def test_escolhe_a_temporada_com_etapa_por_acontecer(self):
        por_ano = {
            2026: [{"inicio": "06/11/2026", "fim": "08/11/2026"}],
            2027: [{"inicio": "25/03/2027", "fim": "27/03/2027"}],
        }
        self.assertEqual(wec.escolher_temporada(por_ano, date(2026, 10, 17)), 2026)
        self.assertEqual(wec.escolher_temporada(por_ano, date(2026, 11, 9)), 2027)
        self.assertEqual(wec.escolher_temporada(por_ano, date(2028, 1, 1)), 2027)

    def test_etapas_saem_numeradas_em_ordem_de_data(self):
        etapas = [
            {"circuito": "B", "pais": "x", "inicio": "10/07/2026", "fim": "12/07/2026", "sessoes": []},
            {"circuito": "A", "pais": "x", "inicio": "17/04/2026", "fim": "19/04/2026", "sessoes": []},
        ]
        dados = wec.montar(etapas, 2026)

        self.assertEqual([e["etapa"] for e in dados["etapas"]], [1, 2])
        self.assertEqual([e["circuito"] for e in dados["etapas"]], ["A", "B"])
        self.assertEqual(dados["categoria"], "WEC")

    def test_coleta_junta_as_etapas_por_ano(self):
        paginas = {
            wec.PAGINAS_INICIAIS[0]: (
                '<a href="/en/race/6-hours-of-barcelona-2026">a</a>'
                '<a href="/en/race/qatar-1812km-2027">b</a>'
            ),
            wec.BASE + "/en/race/6-hours-of-barcelona-2026": HTML_BARCELONA,
            wec.BASE + "/en/race/qatar-1812km-2027": (
                "<html><body><h1>Qatar</h1><p>From 25 to 27 March 2027</p></body></html>"
            ),
        }
        por_ano = wec.coletar(baixar=lambda url: paginas[url])

        self.assertEqual(sorted(por_ano), [2026, 2027])
        self.assertEqual(por_ano[2026][0]["circuito"], "Barcelona")

    def test_se_uma_pagina_falha_nada_e_atualizado(self):
        def baixar(url):
            if url.endswith("/en/"):
                return '<a href="/en/race/6-hours-of-barcelona-2026">a</a>'
            raise OSError("fora do ar")

        with self.assertRaises(OSError):
            wec.coletar(baixar=baixar)

    def test_erro_se_o_site_nao_listar_etapas(self):
        with self.assertRaises(ValueError):
            wec.coletar(baixar=lambda url: "<html></html>")

    def test_recusa_calendario_vazio_ou_muito_menor(self):
        atual = wec.montar(
            [
                {"circuito": "A", "pais": "x", "inicio": "17/04/2026", "fim": "19/04/2026",
                 "sessoes": [{"nome": "Corrida", "data": "19/04/2026", "horario": "08:00"}] * 4},
            ],
            2026,
        )
        vazio = {"temporada": 2026, "categoria": "WEC", "etapas": []}
        sem_sessoes = wec.montar(
            [{"circuito": "A", "pais": "x", "inicio": "17/04/2026", "fim": "19/04/2026", "sessoes": []}],
            2026,
        )

        with self.assertRaises(ValueError):
            wec.validar(vazio, None)
        with self.assertRaises(ValueError):
            wec.validar(sem_sessoes, atual)
        wec.validar(atual, atual)

        nova_temporada = wec.montar(
            [{"circuito": "A", "pais": "x", "inicio": "17/04/2027", "fim": "19/04/2027", "sessoes": []}],
            2027,
        )
        wec.validar(nova_temporada, atual)


class CalendarioWecEmbutidoTest(unittest.TestCase):
    NOMES = re.compile(
        r"^(Treino Livre \d( LMGT3| Hypercar)?|Classificação( LMGT3| Hypercar)?|"
        r"Hyperpole( \d)?( .+)?|Corrida|Warm-up)$"
    )

    def setUp(self):
        self.dados = json.loads(ASSET.read_text(encoding="utf-8"))

    def test_estrutura(self):
        self.assertEqual(self.dados["categoria"], "WEC")
        self.assertTrue(str(self.dados["temporada"]).strip())
        self.assertTrue(self.dados["etapas"])

    def test_numeracao_e_datas(self):
        etapas = self.dados["etapas"]
        self.assertEqual([e["etapa"] for e in etapas], list(range(1, len(etapas) + 1)))
        anterior = None
        for e in etapas:
            inicio = datetime.strptime(e["inicio"], "%d/%m/%Y")
            fim = datetime.strptime(e["fim"], "%d/%m/%Y")
            self.assertLessEqual(inicio, fim)
            if anterior:
                self.assertGreaterEqual(inicio, anterior)
            anterior = inicio
            self.assertTrue(e["circuito"].strip())
            self.assertTrue(e["pais"].strip())

    def test_sessoes_validas_e_perto_do_periodo(self):
        for e in self.dados["etapas"]:
            inicio = datetime.strptime(e["inicio"], "%d/%m/%Y")
            fim = datetime.strptime(e["fim"], "%d/%m/%Y")
            for s in e["sessoes"]:
                with self.subTest(etapa=e["etapa"], sessao=s["nome"]):
                    self.assertRegex(s["nome"], self.NOMES)
                    self.assertRegex(s["horario"], r"^\d{2}:\d{2}$")
                    dia = datetime.strptime(s["data"], "%d/%m/%Y")
                    self.assertTrue(inicio - timedelta(days=2) <= dia <= fim + timedelta(days=2))


if __name__ == "__main__":
    unittest.main()
