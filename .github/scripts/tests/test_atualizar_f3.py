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

    def test_descobre_temporada_atual(self):
        temporada_atual = f3.TEMPORADAS_PREFERIDAS[0]
        html_atual = f'<a href="/en/racing/{temporada_atual}/melbourne">Melbourne</a><a href="/en/racing/{temporada_atual}/monza">Monza</a>'
        with patch.object(f3, "baixar", return_value=html_atual):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, temporada_atual)
        self.assertEqual(slugs, ["melbourne", "monza"])

    def test_descobre_temporada_atual_pela_rota_estavel_se_url_do_ano_falhar(self):
        temporada_atual = f3.TEMPORADAS_PREFERIDAS[0]
        html_atual = f'<a href="/en/racing/{temporada_atual}/melbourne">Melbourne</a><a href="/en/racing/{temporada_atual}/monza">Monza</a>'
        with patch.object(
            f3, "baixar",
            side_effect=[RuntimeError("HTTP 500"), html_atual, html_atual],
        ):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, temporada_atual)
        self.assertEqual(slugs, ["melbourne", "monza"])

    def test_usa_rota_estavel_quando_pagina_do_ano_nao_tem_etapas(self):
        temporada_atual = f3.TEMPORADAS_PREFERIDAS[0]
        html_atual = f'<a href="/en/racing/{temporada_atual}/melbourne">Melbourne</a>'
        with patch.object(
            f3, "baixar",
            side_effect=["página sem etapas", html_atual, html_atual],
        ):
            temporada, slugs = f3.descobrir_calendario()
        self.assertEqual(temporada, temporada_atual)
        self.assertEqual(slugs, ["melbourne"])


    def test_descarta_indice_instavel_e_tenta_rota_alternativa(self):
        temporada = f3.TEMPORADAS_PREFERIDAS[0]
        parcial = f'<a href="/en/racing/{temporada}/melbourne">Melbourne</a>'
        completo = parcial + f'<a href="/en/racing/{temporada}/monza">Monza</a>'
        with patch.object(
            f3,
            "baixar",
            side_effect=[parcial, completo, completo, completo],
        ):
            ano, slugs = f3.descobrir_calendario()
        self.assertEqual(ano, temporada)
        self.assertEqual(slugs, ["melbourne", "monza"])


    def test_indice_atual_instavel_nao_faz_fallback_para_temporada_anterior(self):
        temporada = f3.TEMPORADAS_PREFERIDAS[0]
        parcial = f'<a href="/en/racing/{temporada}/melbourne">Melbourne</a>'
        completo = parcial + f'<a href="/en/racing/{temporada}/monza">Monza</a>'
        with patch.object(
            f3,
            "baixar",
            side_effect=[parcial, completo, parcial, completo],
        ) as baixar:
            ano, slugs = f3.descobrir_calendario()
        self.assertIsNone(ano)
        self.assertEqual(slugs, [])
        self.assertEqual(baixar.call_count, 4)

    def test_aceita_etapa_com_duas_sessoes_publicadas(self):
        html = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-06-04T11:25:00Z"},{"name":"Feature Race","startDate":"2026-06-07T05:45:00Z"}]}</script>'
        etapa = f3.montar_etapa("sakhir", html)
        self.assertEqual([s["nome"] for s in etapa["sessoes"]], ["Treino Livre", "Corrida Feature"])

    def test_avisa_sem_recusar_duas_sessoes_no_mesmo_horario_exibido(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 11, 12, 0, tzinfo=timezone.utc)
        avisos = f3.avisos_de_horario(
            "madrid", [("Classificação 1", instante), ("Classificação 2", instante)]
        )
        self.assertEqual(len(avisos), 1)
        self.assertIn("mesmo horário de Brasília", avisos[0])

    def test_sessao_desconhecida_mantem_o_nome_original(self):
        subs = [
            {"name": "Practice", "startDate": "2026-06-04T11:25:00Z"},
            {"name": "Autograph Session", "startDate": "2026-06-04T15:00:00Z"},
        ]
        self.assertEqual(
            [nome for nome, _ in f3.traduzir_sessoes(subs)],
            ["Treino Livre", "Autograph Session"],
        )

    def test_etapa_sem_sessoes_continua_sendo_recusada(self):
        with self.assertRaisesRegex(ValueError, "nenhuma sessão encontrada"):
            f3.montar_etapa("sakhir", "<html></html>")

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



    def test_confirma_pagina_individual_em_duas_leituras(self):
        html = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-06-04T11:25:00Z"},{"name":"Feature Race","startDate":"2026-06-07T05:45:00Z"}]}</script>'
        with patch.object(f3, "baixar", side_effect=[html, html]) as baixar:
            etapa = f3.obter_etapa_confirmada("sakhir", 2026)
        self.assertEqual(len(etapa["sessoes"]), 2)
        self.assertEqual(baixar.call_count, 2)

    def test_rejeita_pagina_individual_instavel(self):
        parcial = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-06-04T11:25:00Z"}]}</script>'
        completa = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-06-04T11:25:00Z"},{"name":"Feature Race","startDate":"2026-06-07T05:45:00Z"}]}</script>'
        with patch.object(f3, "baixar", side_effect=[parcial, completa]):
            with self.assertRaisesRegex(ValueError, "leituras consecutivas"):
                f3.obter_etapa_confirmada("sakhir", 2026)


    def test_fim_de_semana_expandido_de_madrid_preserva_duas_classificacoes_e_duas_features(self):
        subs = [
            {"name": "Qualifying 1", "startDate": "2026-09-11T10:00:00Z"},
            {"name": "Qualifying 2", "startDate": "2026-09-11T10:30:00Z"},
            {"name": "Race", "startDate": "2026-09-12T09:00:00Z"},
            {"name": "Race", "startDate": "2026-09-12T14:00:00Z"},
            {"name": "Race", "startDate": "2026-09-13T08:00:00Z"},
        ]
        sessoes = f3.traduzir_sessoes(subs)
        self.assertEqual(
            [nome for nome, _ in sessoes],
            [
                "Classificação 1",
                "Classificação 2",
                "Corrida Sprint",
                "Corrida Feature 1",
                "Corrida Feature 2",
            ],
        )
        self.assertEqual(sessoes, sorted(sessoes, key=lambda item: item[1]))


if __name__ == "__main__":
    unittest.main()
