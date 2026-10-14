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


if __name__ == "__main__":
    unittest.main()
