import importlib.util
import inspect
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def carregar_modulo(nome):
    spec = importlib.util.spec_from_file_location(nome, ROOT / f"atualizar_{nome}.py")
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


class AtualizadoresF2F3Test(unittest.TestCase):
    def test_atualizadores_nao_importam_dados_do_json_anterior(self):
        for nome in ("f2", "f3"):
            modulo = carregar_modulo(nome)
            self.assertNotIn("preservar_sessoes_conhecidas", inspect.getsource(modulo.main))
            self.assertFalse(hasattr(modulo, "preservar_sessoes_conhecidas"))


if __name__ == "__main__":
    unittest.main()
