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

    def test_parser_aceita_slug_da_temporada_informada(self):
        html = (
            '<a href="/en/racing/2027/sakhir">Sakhir</a>'
            '<a href="/en/racing/2026/melbourne">Melbourne</a>'
        )
        self.assertEqual(f2.listar_slugs(html, 2027), ["sakhir"])
        self.assertEqual(f2.listar_slugs(html, 2026), ["melbourne"])

    def test_descobre_2027_quando_calendario_oficial_esta_publicado(self):
        from unittest.mock import patch
        html_2027 = (
            '<a href="/en/racing/2027/sakhir">Sakhir</a>'
            '<a href="/en/racing/2027/istanbul">Istanbul</a>'
        )
        with patch.object(f2, "ordem_temporadas", return_value=(2027, 2026)), patch.object(
            f2, "baixar", return_value=html_2027
        ):
            temporada, slugs = f2.descobrir_calendario()
        self.assertEqual(temporada, 2027)
        self.assertEqual(slugs, ["istanbul", "sakhir"])

    def test_descobre_temporada_pela_url_final_da_rota_estavel(self):
        from unittest.mock import patch
        html_2026 = '<a href="/en/racing/2026/melbourne">Melbourne</a>'
        html_2027 = '<a href="/en/racing/2027/sakhir">Sakhir</a>'
        with patch.object(f2, "ordem_temporadas", return_value=(2027,)), patch.object(
            f2,
            "baixar_com_url",
            side_effect=[
                (html_2026, "https://www.fiaformula2.com/en/racing/2026"),
                (html_2027, "https://www.fiaformula2.com/en/racing/2027"),
            ],
        ):
            temporada, slugs = f2.descobrir_calendario()
        self.assertEqual(temporada, 2027)
        self.assertEqual(slugs, ["sakhir"])

    def test_prioriza_2026_ate_o_fim_da_temporada(self):
        from datetime import date
        self.assertEqual(f2.ordem_temporadas(date(2026, 10, 14)), (2026, 2027))

    def test_prioriza_2027_depois_do_fim_da_temporada(self):
        from datetime import date
        self.assertEqual(f2.ordem_temporadas(date(2026, 12, 7)), (2027, 2026))
    def test_mantem_2026_se_calendario_2027_ainda_nao_tiver_etapas(self):
        from unittest.mock import patch
        html_2026 = '<a href="/en/racing/2026/melbourne">Melbourne</a>'
        with patch.object(f2, "ordem_temporadas", return_value=(2027, 2026)), patch.object(
            f2, "baixar", side_effect=["página sem etapas", html_2026]
        ):
            temporada, slugs = f2.descobrir_calendario()
        self.assertEqual(temporada, 2026)
        self.assertEqual(slugs, ["melbourne"])

    def test_mapeia_circuitos_novos_confirmados_para_2027(self):
        self.assertEqual(f2.CIRCUITOS["sakhir"][0], "Sakhir")
        self.assertEqual(f2.CIRCUITOS["jeddah"][0], "Jeddah")
        self.assertEqual(f2.CIRCUITOS["istanbul"][0], "Istanbul Park")
        self.assertEqual(f2.CIRCUITOS["mexico-city"][0], "Cidade do México")

    def test_rejeita_duas_sessoes_no_mesmo_instante(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 25, 10, 0, tzinfo=timezone.utc)
        with self.assertRaisesRegex(ValueError, "começam no mesmo instante"):
            f2.validar_sessoes_sem_conflito(
                "baku", [("Corrida Sprint", instante), ("Corrida Feature", instante)]
            )


if __name__ == "__main__":
    unittest.main()
