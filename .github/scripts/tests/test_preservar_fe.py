import importlib.util
import inspect
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("fe", ROOT / "atualizar_fe.py")
fe = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(fe)


class AtualizadorFormulaETest(unittest.TestCase):
    def test_atualizador_nao_mescla_horarios_do_json_anterior(self):
        self.assertNotIn("preservar_detalhes_conhecidos", inspect.getsource(fe.main))
        self.assertFalse(hasattr(fe, "preservar_detalhes_conhecidos"))

    def test_mapeia_london_para_brands_hatch(self):
        dados = {
            "itemListElement": [{
                "item": {
                    "name": "R12 London E-Prix",
                    "location": {"address": {"addressCountry": "GB"}},
                    "startDate": "2027-05-29"
                }
            }]
        }
        rodada = fe.extrair_rodadas(dados)[0]
        self.assertEqual(rodada["cidade"], "Brands Hatch")


if __name__ == "__main__":
    unittest.main()
