"""Testes offline de regressão para descoberta de calendário e sessões da F2."""
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

    def test_parser_aceita_slug_da_temporada_atual(self):
        temporada = f2.TEMPORADA_ATUAL
        html = f'<a href="/en/racing/{temporada}/melbourne">Melbourne</a>'
        self.assertEqual(f2.listar_slugs(html, temporada), ["melbourne"])

    def test_descobre_temporada_atual_pela_url_final_da_rota_estavel(self):
        from unittest.mock import patch
        temporada_atual = f2.TEMPORADA_ATUAL
        html_atual = f'<a href="/en/racing/{temporada_atual}/melbourne">Melbourne</a>'
        with patch.object(
            f2,
            "baixar_com_url",
            side_effect=[
                (html_atual, f"https://www.fiaformula2.com/en/racing/{temporada_atual}"),
            ],
        ):
            temporada, slugs = f2.descobrir_calendario()
        self.assertEqual(temporada, temporada_atual)
        self.assertEqual(slugs, ["melbourne"])

    def test_aceita_etapa_com_duas_sessoes_publicadas(self):
        html = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-03-06T00:00:00Z"},{"name":"Race","startDate":"2026-03-08T00:25:00Z"}]}</script>'
        etapa = f2.montar_etapa("sakhir", html)
        self.assertEqual([s["nome"] for s in etapa["sessoes"]], ["Treino Livre", "Corrida Feature"])

    def test_avisa_sem_recusar_duas_sessoes_no_mesmo_instante(self):
        from datetime import datetime, timezone
        instante = datetime(2026, 9, 25, 10, 0, tzinfo=timezone.utc)
        avisos = f2.avisos_de_horario(
            "baku", [("Corrida Sprint", instante), ("Corrida Feature", instante)]
        )
        self.assertEqual(len(avisos), 1)
        self.assertIn("mesmo horário de Brasília", avisos[0])

    def test_sessoes_no_mesmo_horario_nao_impedem_a_atualizacao(self):
        html = '<script type="application/ld+json">{"subEvent":[{"name":"Practice","startDate":"2026-03-06T00:00:00Z"},{"name":"Qualifying","startDate":"2026-03-06T00:00:00Z"}]}</script>'
        etapa = f2.montar_etapa("sakhir", html)
        self.assertEqual(len(etapa["sessoes"]), 2)

    def test_sessao_desconhecida_mantem_o_nome_original(self):
        subs = [
            {"name": "Practice", "startDate": "2026-03-06T00:00:00Z"},
            {"name": "Pit Lane Walk", "startDate": "2026-03-06T05:00:00Z"},
        ]
        self.assertEqual(
            [nome for nome, _ in f2.traduzir_sessoes(subs)],
            ["Treino Livre", "Pit Lane Walk"],
        )

    def test_etapa_sem_sessoes_continua_sendo_recusada(self):
        with self.assertRaisesRegex(ValueError, "nenhuma sessão encontrada"):
            f2.montar_etapa("sakhir", "<html></html>")

    def test_circuito_nao_mapeado_continua_sendo_recusado(self):
        with self.assertRaisesRegex(ValueError, "Circuito não mapeado"):
            f2.montar_etapa("circuito-inexistente", "<html></html>")

    def test_extrai_subevent_de_lista_jsonld(self):
        html = (
            '<script type="application/ld+json">'
            '[{"@type":"WebPage"},{"@type":"Event","subEvent":'
            '[{"name":"Practice","startDate":"2026-03-06T00:00:00Z"}]}]'
            '</script>'
        )
        self.assertEqual(f2.sub_eventos(html), [
            {"name": "Practice", "startDate": "2026-03-06T00:00:00Z"}
        ])

    def test_extrai_subevent_de_grafo_jsonld_aninhado(self):
        html = (
            '<script type="application/ld+json">'
            '{"@context":"https://schema.org","@graph":['
            '{"@type":"WebPage","name":"Calendário"},'
            '{"@type":"Event","subEvent":['
            '{"name":"Qualifying","startDate":"2026-03-06T03:55:00Z"}]}]}'
            '</script>'
        )
        self.assertEqual(f2.sub_eventos(html), [
            {"name": "Qualifying", "startDate": "2026-03-06T03:55:00Z"}
        ])


if __name__ == "__main__":
    unittest.main()
