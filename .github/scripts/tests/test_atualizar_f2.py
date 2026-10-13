"""Testes de regressão offline para a tradução de sessões da F2."""
import importlib.util
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SCRIPT = RAIZ / ".github" / "scripts" / "atualizar_f2.py"
SPEC = importlib.util.spec_from_file_location("atualizar_f2", SCRIPT)
f2 = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(f2)


class AtualizadorF2SessoesTest(unittest.TestCase):
    def test_ordenacao_cronologica_e_sessoes_de_um_fim_de_semana_normal(self):
        subs = [
            {"name": "Race - Feature", "startDate": "2026-03-08T00:25:00Z"},
            {"name": "Qualifying", "startDate": "2026-03-06T03:55:00Z"},
            {"name": "Race - Sprint", "startDate": "2026-03-07T03:10:00Z"},
            {"name": "Practice", "startDate": "2026-03-06T00:00:00Z"},
        ]
        sessoes = f2.traduzir_sessoes(subs)
        self.assertEqual(
            [nome for nome, _ in sessoes],
            ["Treino Livre", "Classificação", "Corrida Sprint", "Corrida Feature"],
        )
        self.assertEqual(sessoes, sorted(sessoes, key=lambda item: item[1]))

    def test_preserva_as_duas_corridas_feature_no_formato_expandido(self):
        subs = [
            {"name": "Race", "startDate": "2026-09-26T07:00:00Z"},
            {"name": "Race", "startDate": "2026-09-25T04:15:00Z"},
            {"name": "Race", "startDate": "2026-09-25T10:15:00Z"},
        ]
        sessoes = f2.traduzir_sessoes(subs)
        self.assertEqual(
            [nome for nome, _ in sessoes],
            ["Corrida Sprint", "Corrida Feature 1", "Corrida Feature 2"],
        )

    def test_rejeita_duas_sessoes_no_mesmo_instante(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 25, 10, 0, tzinfo=timezone.utc)
        with self.assertRaisesRegex(ValueError, "começam no mesmo instante"):
            f2.validar_sessoes_sem_conflito(
                "baku", [("Corrida Sprint", instante), ("Corrida Feature", instante)]
            )


if __name__ == "__main__":
    unittest.main()
