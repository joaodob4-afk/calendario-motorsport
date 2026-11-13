import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("fe", ROOT / "atualizar_fe.py")
fe = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(fe)


class DescobertaItemListJsonLdTest(unittest.TestCase):
    def test_item_list_em_objeto_raiz(self):
        html = '<script type="application/ld+json">{"itemListElement":[{"item":{"name":"R1 Sao Paulo E-Prix","startDate":"2027-01-01"}}]}</script>'
        self.assertEqual(len(fe.lista_do_calendario(html)["itemListElement"]), 1)

    def test_item_list_em_lista_raiz(self):
        html = '''<script type="application/ld+json">[
          {"@type":"WebSite","name":"Formula E"},
          {"@type":"ItemList","itemListElement":[{"item":{"name":"R1 Sao Paulo E-Prix","startDate":"2027-01-01"}}]}
        ]</script>'''
        self.assertEqual(fe.lista_do_calendario(html)["@type"], "ItemList")

    def test_item_list_dentro_de_graph(self):
        html = '''<script type="application/ld+json">{"@graph":[
          {"@type":"WebSite","name":"Formula E"},
          {"@type":"ItemList","itemListElement":[{"item":{"name":"R1 Sao Paulo E-Prix","startDate":"2027-01-01"}}]}
        ]}</script>'''
        self.assertEqual(fe.lista_do_calendario(html)["@type"], "ItemList")

    def test_ignora_json_ld_invalido_e_procura_proximo_bloco(self):
        html = '''<script type="application/ld+json">{inválido}</script>
        <script type="application/ld+json">{"itemListElement":[{"item":{"name":"R1 Sao Paulo E-Prix","startDate":"2027-01-01"}}]}</script>'''
        self.assertTrue(fe.lista_do_calendario(html)["itemListElement"])

    def test_rejeita_item_list_vazio_e_sem_item_list(self):
        html = '''<script type="application/ld+json">{"@type":"WebSite","name":"site"}</script>
        <script type="application/ld+json">{"@type":"ItemList","itemListElement":[]}</script>'''
        with self.assertRaisesRegex(ValueError, "ItemList"):
            fe.lista_do_calendario(html)

    def test_rejeita_numero_de_rodada_duplicado(self):
        dados = {"itemListElement": [
            {"item": {"name": "R1 Sao Paulo E-Prix", "startDate": "2027-01-01",
                      "location": {"address": {"addressCountry": "BR"}}}},
            {"item": {"name": "R1 Mexico City E-Prix", "startDate": "2027-01-15",
                      "location": {"address": {"addressCountry": "MX"}}}},
        ]}
        with self.assertRaisesRegex(ValueError, "Rodada duplicada"):
            fe.extrair_rodadas(dados)

    def test_rejeita_pais_ausente(self):
        dados = {"itemListElement": [
            {"item": {"name": "R1 Sao Paulo E-Prix", "startDate": "2027-01-01"}}
        ]}
        with self.assertRaisesRegex(ValueError, "País ausente"):
            fe.extrair_rodadas(dados)

    def test_rejeita_data_invalida(self):
        dados = {"itemListElement": [
            {"item": {"name": "R1 Sao Paulo E-Prix", "startDate": "data-invalida",
                      "location": {"address": {"addressCountry": "BR"}}}}
        ]}
        with self.assertRaisesRegex(ValueError, "Data inválida"):
            fe.extrair_rodadas(dados)

    def test_traduz_berlin_para_berlim(self):
        dados = {"itemListElement": [
            {"item": {"name": "R1 Berlin E-Prix", "startDate": "2027-01-01",
                      "location": {"address": {"addressCountry": "DE"}}}}
        ]}
        self.assertEqual(fe.extrair_rodadas(dados)[0]["cidade"], "Berlim")


    def test_indice_oficial_e_aceito_quando_duas_leituras_coincidem(self):
        from unittest.mock import patch
        rodadas = [{"rodada": 1, "cidade": "São Paulo", "pais": "Brasil", "data": fe.date(2027, 1, 1)}]
        with patch.object(fe, "baixar", return_value="<html></html>"), patch.object(
            fe, "lista_do_calendario", return_value={}
        ), patch.object(fe, "extrair_rodadas", side_effect=[rodadas, rodadas]):
            self.assertEqual(fe.obter_rodadas_confirmadas(), rodadas)

    def test_indice_oficial_instavel_e_rejeitado(self):
        from unittest.mock import patch
        parcial = [{"rodada": 1, "cidade": "São Paulo", "pais": "Brasil", "data": fe.date(2027, 1, 1)}]
        completo = parcial + [{"rodada": 2, "cidade": "Cidade do México", "pais": "México", "data": fe.date(2027, 1, 15)}]
        with patch.object(fe, "baixar", return_value="<html></html>"), patch.object(
            fe, "lista_do_calendario", return_value={}
        ), patch.object(fe, "extrair_rodadas", side_effect=[parcial, completo]):
            with self.assertRaisesRegex(ValueError, "mudou entre leituras"):
                fe.obter_rodadas_confirmadas()

    def test_rotulo_temporada_com_dois_anos(self):
        rodadas = [
            {"data": fe.date(2026, 12, 5)},
            {"data": fe.date(2027, 1, 10)},
        ]
        self.assertEqual(fe.rotulo_temporada(rodadas), "2026/27")

    def test_rotulo_temporada_com_um_ano(self):
        rodadas = [
            {"data": fe.date(2028, 3, 1)},
            {"data": fe.date(2028, 4, 1)},
        ]
        self.assertEqual(fe.rotulo_temporada(rodadas), "2028")

    def test_rotulo_temporada_rejeita_lista_vazia(self):
        with self.assertRaisesRegex(ValueError, "Não há datas"):
            fe.rotulo_temporada([])


class SessoesOficiaisEventoTest(unittest.TestCase):
    HTML_MEXICO = """
    <html><body>
      <h2>Schedule</h2>
      <table>
        <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
        <tr><td>Practice 1</td><td>Fri 15 Jan</td><td>16:15 – 17:00</td></tr>
        <tr><td>Practice 2</td><td>Sat 16 Jan</td><td>07:30 – 08:15</td></tr>
        <tr><td>Qualifying</td><td>Sat 16 Jan</td><td>10:00 – 11:10</td></tr>
        <tr><td>Race</td><td>Sat 16 Jan</td><td>15:05 – 16:00</td></tr>
      </table>
    </body></html>
    """

    def test_extrai_sessoes_e_converte_horario_local_para_brasilia(self):
        sessoes = fe.extrair_sessoes_evento(
            self.HTML_MEXICO, 2027, "America/Mexico_City", 1
        )

        self.assertEqual(
            sessoes,
            [
                {"nome": "Treino Livre 1", "data": "15/01/2027", "horario": "19:15"},
                {"nome": "Treino Livre 2", "data": "16/01/2027", "horario": "10:30"},
                {"nome": "Classificação", "data": "16/01/2027", "horario": "13:00"},
                {"nome": "Corrida (Rodada 1)", "data": "16/01/2027", "horario": "18:05"},
            ],
        )

    def test_preenche_sessoes_apenas_para_o_proximo_evento(self):
        etapas = [
            {
                "etapa": 1,
                "circuito": "Cidade do México",
                "pais": "México",
                "inicio": "15/01/2027",
                "fim": "15/01/2027",
                "sessoes": [
                    {"nome": "Corrida (Rodada 1)", "data": "15/01/2027", "horario": "A confirmar"}
                ],
            },
            {
                "etapa": 2,
                "circuito": "Austin",
                "pais": "Estados Unidos",
                "inicio": "06/02/2027",
                "fim": "06/02/2027",
                "sessoes": [
                    {"nome": "Corrida (Rodada 2)", "data": "06/02/2027", "horario": "A confirmar"}
                ],
            },
        ]
        urls = []

        def baixar(url):
            urls.append(url)
            return self.HTML_MEXICO

        resultado = fe.preencher_sessoes_proximo_evento(
            etapas, hoje=fe.date(2026, 11, 12), baixar_html=baixar
        )

        self.assertTrue(resultado)
        self.assertEqual(urls, ["https://www.fiaformulae.com/en/mexicocity"])
        self.assertEqual(len(etapas[0]["sessoes"]), 4)
        self.assertEqual(etapas[0]["fim"], "16/01/2027")
        self.assertEqual(etapas[1]["sessoes"][0]["horario"], "A confirmar")


    def test_etapa_de_rodada_dupla_associa_cada_corrida_a_sua_rodada(self):
        html = """
        <table>
          <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
          <tr><td>Practice 1</td><td>Fri 30 Apr</td><td>09:00 – 09:45</td></tr>
          <tr><td>Qualifying</td><td>Sat 01 May</td><td>10:00 – 11:00</td></tr>
          <tr><td>Race</td><td>Sat 01 May</td><td>15:00 – 16:00</td></tr>
          <tr><td>Practice 2</td><td>Sun 02 May</td><td>09:00 – 09:45</td></tr>
          <tr><td>Qualifying</td><td>Sun 02 May</td><td>10:00 – 11:00</td></tr>
          <tr><td>Race</td><td>Sun 02 May</td><td>15:00 – 16:00</td></tr>
        </table>
        """
        sessoes = fe.extrair_sessoes_evento(
            html, 2027, "Europe/Monaco", [8, 9]
        )
        corridas = [s["nome"] for s in sessoes if s["nome"].startswith("Corrida")]
        self.assertEqual(corridas, ["Corrida (Rodada 8)", "Corrida (Rodada 9)"])

    def test_etapa_de_rodada_dupla_reconhece_eprix_unleashed(self):
        html = """
        <table>
          <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
          <tr><td>Practice 1</td><td>Fri 30 Apr</td><td>09:00 – 09:45</td></tr>
          <tr><td>Qualifying</td><td>Sat 01 May</td><td>10:00 – 11:00</td></tr>
          <tr><td>E-Prix Unleashed</td><td>Sat 01 May</td><td>13:00 – 13:30</td></tr>
          <tr><td>Race</td><td>Sun 02 May</td><td>15:00 – 16:00</td></tr>
        </table>
        """
        sessoes = fe.extrair_sessoes_evento(
            html, 2027, "Europe/Monaco", [8, 9]
        )
        corridas = [s["nome"] for s in sessoes if "Rodada" in s["nome"]]
        self.assertEqual(
            corridas,
            ["E-Prix Unleashed (Rodada 8)", "Corrida (Rodada 9)"],
        )

    def test_etapa_de_rodada_dupla_rejeita_programacao_com_uma_corrida(self):
        html = """
        <table>
          <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
          <tr><td>Race</td><td>Sat 01 May</td><td>15:00 – 16:00</td></tr>
        </table>
        """
        with self.assertRaisesRegex(ValueError, "Programação incompleta"):
            fe.extrair_sessoes_evento(
                html, 2027, "Europe/Monaco", [8, 9]
            )

    def test_nao_substitui_placeholder_se_pagina_nao_tiver_corrida(self):
        html = """
        <table>
          <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
          <tr><td>Practice 1</td><td>Fri 15 Jan</td><td>16:15 – 17:00</td></tr>
        </table>
        """
        with self.assertRaisesRegex(ValueError, "Programação incompleta"):
            fe.extrair_sessoes_evento(html, 2027, "America/Mexico_City", 1)


class TodasEtapasFuturasTest(unittest.TestCase):
    def test_consulta_todas_as_etapas_futuras_sem_parar_na_primeira(self):
        etapas = [
            {
                "etapa": 1, "circuito": "Cidade do México", "pais": "México",
                "inicio": "15/01/2027", "fim": "16/01/2027",
                "sessoes": [
                    {"nome": "Corrida (Rodada 1)", "data": "16/01/2027", "horario": "A confirmar"}
                ],
            },
            {
                "etapa": 2, "circuito": "Austin", "pais": "Estados Unidos",
                "inicio": "06/02/2027", "fim": "06/02/2027",
                "sessoes": [
                    {"nome": "Corrida (Rodada 2)", "data": "06/02/2027", "horario": "A confirmar"}
                ],
            },
        ]
        html_austin = """
        <table>
          <tr><th>Session</th><th>Date</th><th>Track time</th></tr>
          <tr><td>Practice 1</td><td>Sat 06 Feb</td><td>09:00 – 09:45</td></tr>
          <tr><td>Qualifying</td><td>Sat 06 Feb</td><td>10:00 – 11:00</td></tr>
          <tr><td>Race</td><td>Sat 06 Feb</td><td>15:00 – 16:00</td></tr>
        </table>
        """
        urls = []

        def baixar(url):
            urls.append(url)
            if url.endswith("/mexicocity"):
                return SessoesOficiaisEventoTest.HTML_MEXICO
            if url.endswith("/austin"):
                return html_austin
            raise AssertionError(f"URL inesperada: {url}")

        resultado = fe.preencher_sessoes_eventos(
            etapas, hoje=fe.date(2027, 1, 1), baixar_html=baixar
        )

        self.assertTrue(resultado)
        self.assertEqual(
            urls,
            [
                "https://www.fiaformulae.com/en/mexicocity",
                "https://www.fiaformulae.com/en/austin",
            ],
        )
        self.assertEqual(etapas[0]["sessoes"][-1]["horario"], "18:05")
        self.assertEqual(etapas[1]["sessoes"][-1]["nome"], "Corrida (Rodada 2)")
        self.assertEqual(etapas[1]["sessoes"][-1]["horario"], "18:00")


if __name__ == "__main__":
    unittest.main()
