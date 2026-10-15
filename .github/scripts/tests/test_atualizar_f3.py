"""Testes de regressão offline para a tradução de sessões da F3."""
import importlib.util
import unittest
from unittest.mock import patch
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

    def test_parser_aceita_slug_de_temporada_informada(self):
        html = '<a href="/en/racing/2027/jeddah">Jeddah</a><a href="/en/racing/2026/melbourne">Melbourne</a>'
        self.assertEqual(f3.listar_slugs(html, 2027), ["jeddah"])
        self.assertEqual(f3.listar_slugs(html, 2026), ["melbourne"])

    def test_descobre_temporada_atual_2026(self):
        html_2026 = '<a href="/en/racing/2026/melbourne">Melbourne</a><a href="/en/racing/2026/monza">Monza</a>'
        with patch.object(f3, "baixar", return_value=html_2026):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, 2026)
        self.assertEqual(slugs, ["melbourne", "monza"])

    def test_descobre_temporada_atual_pela_rota_estavel_se_url_do_ano_falhar(self):
        html_2026 = '<a href="/en/racing/2026/melbourne">Melbourne</a><a href="/en/racing/2026/monza">Monza</a>'
        with patch.object(
            f3, "baixar",
            side_effect=[RuntimeError("HTTP 500"), html_2026],
        ):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, 2026)
        self.assertEqual(slugs, ["melbourne", "monza"])

    def test_nao_busca_temporada_futura(self):
        html_2027 = '<a href="/en/racing/2027/jeddah">Jeddah</a>'
        with patch.object(f3, "baixar", return_value=html_2027):
            temporada, slugs = f3.descobrir_calendario()
        self.assertIsNone(temporada)
        self.assertEqual(slugs, [])

    def test_usa_rota_estavel_quando_pagina_de_2026_nao_tem_etapas(self):
        html_2026 = '<a href="/en/racing/2026/melbourne">Melbourne</a>'
        with patch.object(
            f3, "baixar",
            side_effect=["página sem etapas", html_2026],
        ):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, 2026)
        self.assertEqual(slugs, ["melbourne"])

    def test_mapeia_novos_circuitos_confirmados_para_2027(self):
        self.assertEqual(f3.CIRCUITOS["jeddah"][0], "Jeddah")
        self.assertEqual(f3.CIRCUITOS["montreal"][0], "Montreal")
        self.assertEqual(f3.CIRCUITOS["istanbul"][0], "Istanbul Park")

    def test_aceita_etapa_com_duas_sessoes_publicadas(self):
        html = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-06-04T11:25:00Z"},{"name":"Feature Race","startDate":"2026-06-07T05:45:00Z"}]}</script>'
        etapa = f3.montar_etapa("sakhir", html)
        self.assertEqual([s["nome"] for s in etapa["sessoes"]], ["Treino Livre", "Corrida Feature"])

    def test_rejeita_duas_sessoes_no_mesmo_horario_exibido(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 11, 12, 0, tzinfo=timezone.utc)
        with self.assertRaisesRegex(ValueError, "mesmo horário de Brasília"):
            f3.validar_horarios_exibidos(
                "madrid", [("Classificação 1", instante), ("Classificação 2", instante)]
            )


    def test_extrai_sessoes_de_jsonld_em_lista(self):
        html = '''<script type="application/ld+json">[
          {"@type":"WebPage","name":"Página"},
          {"@type":"Event","subEvent":[
            {"name":"Practice","startDate":"2026-06-04T11:25:00Z"},
            {"name":"Feature Race","startDate":"2026-06-07T05:45:00Z"}
          ]}
        ]</script>'''
        self.assertEqual(
            f3.sub_eventos(html),
            [
                {"name": "Practice", "startDate": "2026-06-04T11:25:00Z"},
                {"name": "Feature Race", "startDate": "2026-06-07T05:45:00Z"},
            ],
        )

    def test_extrai_sessoes_de_jsonld_em_graph(self):
        html = '''<script type="application/ld+json">{"@graph":[
          {"@type":"WebSite","name":"F3"},
          {"@type":"Event","subEvent":[
            {"name":"Practice","startDate":"2026-06-04T11:25:00Z"}
          ]}
        ]}</script>'''
        self.assertEqual(
            f3.sub_eventos(html),
            [{"name": "Practice", "startDate": "2026-06-04T11:25:00Z"}],
        )

    def test_ignora_jsonld_invalido_e_continua_procurando(self):
        html = '''<script type="application/ld+json">{inválido}</script>
        <script type="application/ld+json">{"subEvent":[
          {"name":"Practice","startDate":"2026-06-04T11:25:00Z"}
        ]}</script>'''
        self.assertEqual(len(f3.sub_eventos(html)), 1)


if __name__ == "__main__":
    unittest.main()
