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


if __name__ == "__main__":
    unittest.main()
