import importlib.util
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
SPEC = importlib.util.spec_from_file_location("diagnostico_fe_sessoes", ROOT / "diagnostico_fe_sessoes.py")
diagnostico = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(diagnostico)


class DiagnosticoFormulaETest(unittest.TestCase):
    def test_slug_remove_acentos_e_pontuacao(self):
        self.assertEqual(diagnostico.slug_cidade("São Paulo"), "sao-paulo")
        self.assertEqual(diagnostico.slug_cidade("Mexico City"), "mexico-city")

    def test_descobre_urls_para_todas_as_rodadas_do_indice(self):
        html = '''<script type="application/ld+json">{
          "@type":"ItemList","itemListElement":[
            {"item":{"name":"R1 Sao Paulo E-Prix","startDate":"2026-12-01",
              "location":{"address":{"addressCountry":"BR"}}}},
            {"item":{"name":"R2 Mexico City E-Prix","startDate":"2027-01-01",
              "location":{"address":{"addressCountry":"MX"}}}}
          ]}</script>'''
        urls = diagnostico.descobrir_urls(html, temporada=13)
        self.assertEqual(len(urls), 2)
        self.assertIn("season=13", urls[0][2])
        self.assertIn("round=1-sao-paulo", urls[0][2])
        self.assertIn("round=2-mexico-city", urls[1][2])

    def test_rejeita_indice_sem_nome_de_rodada_reconhecivel(self):
        html = '''<script type="application/ld+json">{
          "itemListElement":[{"item":{"name":"Evento especial","startDate":"2027-01-01",
          "location":{"address":{"addressCountry":"BR"}}}}]}</script>'''
        with self.assertRaisesRegex(ValueError, "Nome de rodada inesperado"):
            diagnostico.descobrir_urls(html)


if __name__ == "__main__":
    unittest.main()
