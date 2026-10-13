"""Testes de regressão offline para a tradução de sessões da F3."""
import importlib.util
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SCRIPT = RAIZ / ".github" / "scripts" / "atualizar_f3.py"
SPEC = importlib.util.spec_from_file_location("atualizar_f3", SCRIPT)
f3 = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(f3)


class AtualizadorF3SessoesTest(unittest.TestCase):
    def test_ordenacao_cronologica_e_classificacao_em_grupos(self):
        subs = [
            {"name": "Qualifying Group B", "startDate": "2026-06-05T09:29:00Z"},
            {"name": "Sprint Race", "startDate": "2026-06-06T08:45:00Z"},
            {"name": "Practice", "startDate": "2026-06-04T11:25:00Z"},
            {"name": "Qualifying Group A", "startDate": "2026-06-05T09:05:00Z"},
            {"name": "Feature Race", "startDate": "2026-06-07T05:45:00Z"},
        ]
        sessoes = f3.traduzir_sessoes(subs)
        self.assertEqual(
            [nome for nome, _ in sessoes],
            [
                "Treino Livre",
                "Classificação Grupo A",
                "Classificação Grupo B",
                "Corrida Sprint",
                "Corrida Feature",
            ],
        )
        self.assertEqual(sessoes, sorted(sessoes, key=lambda item: item[1]))

    def test_preserva_as_duas_corridas_feature_do_fim_de_semana_expandido(self):
        subs = [
            {"name": "Feature Race 2", "startDate": "2026-09-13T07:45:00Z"},
            {"name": "Feature Race 1", "startDate": "2026-09-12T16:00:00Z"},
        ]
        sessoes = f3.traduzir_sessoes(subs)
        self.assertEqual(
            [nome for nome, _ in sessoes],
            ["Corrida Feature 1", "Corrida Feature 2"],
        )

    def test_rejeita_duas_sessoes_no_mesmo_horario_exibido(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 11, 12, 0, tzinfo=timezone.utc)
        with self.assertRaisesRegex(ValueError, "mesmo horário de Brasília"):
            f3.validar_horarios_exibidos(
                "madrid", [("Classificação 1", instante), ("Classificação 2", instante)]
            )


if __name__ == "__main__":
    unittest.main()
