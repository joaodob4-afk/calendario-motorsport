"""Testes offline da trava contra calendários encolhidos."""
import importlib.util
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location(
    "utilitarios", RAIZ / ".github" / "scripts" / "utilitarios.py"
)
util = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(util)


def calendario(n, temporada=2026):
    return {"temporada": temporada, "etapas": [{"etapa": i + 1} for i in range(n)]}


class ValidarEncolhimentoTest(unittest.TestCase):
    def test_sem_arquivo_anterior_aceita(self):
        util.validar_encolhimento(calendario(1), None, "X")

    def test_rejeita_queda_grande_na_mesma_temporada(self):
        with self.assertRaises(ValueError):
            util.validar_encolhimento(calendario(3), calendario(20), "X")  # 3 de 20 (< 20%)

    def test_aceita_queda_pequena_e_crescimento(self):
        util.validar_encolhimento(calendario(4), calendario(20), "X")
        util.validar_encolhimento(calendario(18), calendario(20), "X")
        util.validar_encolhimento(calendario(25), calendario(20), "X")

    def test_outra_temporada_pode_ter_menos_etapas(self):
        util.validar_encolhimento(calendario(5, 2027), calendario(20, 2026), "X")


if __name__ == "__main__":
    unittest.main()
