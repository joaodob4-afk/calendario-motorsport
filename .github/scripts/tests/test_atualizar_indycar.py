"""Testes de regressão offline para o atualizador da IndyCar."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SCRIPT = RAIZ / ".github" / "scripts" / "atualizar_indycar.py"
SPEC = importlib.util.spec_from_file_location("atualizar_indycar", SCRIPT)
indycar = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(indycar)



class AtualizadorIndyCarTest(unittest.TestCase):
    def test_aceita_qualquer_quantidade_positiva_de_etapas_da_fonte(self):
        for quantidade in (1, 2, 9, 18, 25):
            with self.subTest(quantidade=quantidade):
                cartoes = {f"etapa-{n}": {} for n in range(quantidade)}
                self.assertEqual(
                    len(indycar.validar_cartoes_fonte(cartoes)), quantidade
                )


    def test_indice_oficial_e_aceito_quando_duas_leituras_coincidem(self):
        cartoes = {"st-petersburg": {"ano": 2027, "titulo": "St. Petersburg"}}
        from unittest.mock import patch
        with patch.object(indycar, "baixar", return_value="<html></html>"), patch.object(
            indycar, "extrair_cartoes", side_effect=[cartoes, cartoes]
        ):
            self.assertEqual(indycar.obter_cartoes_confirmados(), cartoes)

    def test_indice_oficial_instavel_e_rejeitado(self):
        parcial = {"st-petersburg": {"ano": 2027, "titulo": "St. Petersburg"}}
        completo = {
            **parcial,
            "long-beach": {"ano": 2027, "titulo": "Long Beach"},
        }
        from unittest.mock import patch
        with patch.object(indycar, "baixar", return_value="<html></html>"), patch.object(
            indycar, "extrair_cartoes", side_effect=[parcial, completo]
        ):
            with self.assertRaisesRegex(ValueError, "mudou entre leituras"):
                indycar.obter_cartoes_confirmados()

    def test_rejeita_fonte_sem_etapas_para_nao_apagar_calendario(self):
        with self.assertRaisesRegex(ValueError, "Nenhuma etapa encontrada"):
            indycar.validar_cartoes_fonte({})

    def test_separa_local_e_pais_sem_repetir_pais_internacional(self):
        self.assertEqual(
            indycar.separar_local_e_pais("Barcelona, Spain"),
            ("Barcelona", "Espanha"),
        )

    def test_preserva_cidade_e_estado_dos_eua(self):
        self.assertEqual(
            indycar.separar_local_e_pais("Long Beach, California"),
            ("Long Beach, California", "Estados Unidos"),
        )

    def test_preserva_cidade_e_provincia_do_canada(self):
        self.assertEqual(
            indycar.separar_local_e_pais("Markham, Ontario"),
            ("Markham, Ontario", "Canadá"),
        )

    def test_converte_horario_et_para_brasilia_no_horario_de_verao(self):
        instante = indycar.converter_hora(2026, 5, 1, 10, 0, "AM")
        brasilia = instante.astimezone(indycar.BRASILIA)
        self.assertEqual((brasilia.hour, brasilia.minute), (11, 0))

    def test_converte_meia_noite_e_meio_dia_corretamente(self):
        meia_noite = indycar.converter_hora(2026, 5, 1, 12, 0, "AM")
        meio_dia = indycar.converter_hora(2026, 5, 1, 12, 0, "PM")
        self.assertEqual(meia_noite.hour, 0)
        self.assertEqual(meio_dia.hour, 12)

    def test_traduz_sessoes_conhecidas_sem_descartar_corrida(self):
        self.assertEqual(indycar.traduzir("Practice 1"), "Treino Livre 1")
        self.assertEqual(indycar.traduzir("Qualifications"), "Classificação")
        self.assertEqual(indycar.traduzir("Race 2"), "Corrida 2")
        self.assertEqual(indycar.traduzir("Pre-race"), "Pre-race")

    def test_extrai_sessao_de_html_sintetico_sem_rede(self):
        html = """
        <main>
          <div>Download Weekend Schedule</div>
          <div>Friday, May 1</div>
          <div>10:00 AM ET</div>
          <div>NTT INDYCAR SERIES - Practice 1</div>
          <div>Find Local Radio Station</div>
        </main>
        """
        sessoes = indycar.extrair_sessoes(html, 2026)
        self.assertEqual(len(sessoes), 1)
        nome, instante, dia = sessoes[0]
        self.assertEqual(nome, "Treino Livre 1")
        self.assertEqual(dia, (5, 1))
        self.assertEqual(
            instante.astimezone(indycar.BRASILIA).strftime("%d/%m/%Y %H:%M"),
            "01/05/2026 11:00",
        )

    def test_extrai_programacao_oficial_sem_prefixo_da_serie(self):
        html = """
        <main>
          <div>Download Weekend Schedule</div>
          <div>Friday, Mar 6</div>
          <div>10:00AM ET</div><div>Practice 1</div>
          <div>2:00PM ET</div><div>Qualifications</div>
          <div>4:30PM ET</div><div>High Line &amp; Final Practice</div>
          <div>Saturday, Mar 7</div>
          <div>3:00PM ET</div><div>Race</div>
          <div>Find Local Radio Station</div>
        </main>
        """
        sessoes = indycar.extrair_sessoes(html, 2026)
        self.assertEqual(
            [s[0] for s in sessoes],
            ["Treino Livre 1", "Classificação", "Treino Final (High Line)", "Corrida"],
        )
        self.assertEqual(
            [s[1].astimezone(indycar.BRASILIA).strftime("%d/%m/%Y %H:%M") for s in sessoes],
            ["06/03/2026 12:00", "06/03/2026 16:00", "06/03/2026 18:30", "07/03/2026 17:00"],
        )

    def test_html_sem_bloco_de_programacao_nao_inventa_sessoes(self):
        self.assertEqual(indycar.extrair_sessoes("<html>Sem programação</html>", 2026), [])

    def test_separa_corridas_de_milwaukee_quando_a_pagina_lista_as_duas(self):
        sessoes = [
            ("Treino Livre", indycar.converter_hora(2026, 8, 28, 6, 0, "PM"), (8, 28)),
            ("Classificação", indycar.converter_hora(2026, 8, 29, 11, 0, "AM"), (8, 29)),
            ("Corrida 2", indycar.converter_hora(2026, 8, 30, 1, 0, "PM"), (8, 30)),
            ("Corrida 1", indycar.converter_hora(2026, 8, 30, 6, 0, "PM"), (8, 30)),
        ]
        corrida1 = indycar.filtrar_corrida_da_etapa({"slug": "Milwaukee-Race1"}, sessoes)
        corrida2 = indycar.filtrar_corrida_da_etapa({"slug": "Milwaukee-Race2"}, sessoes)
        esperadas = ["Treino Livre", "Classificação"]
        self.assertEqual([s[0] for s in corrida1], esperadas + ["Corrida 1"])
        self.assertEqual([s[0] for s in corrida2], esperadas + ["Corrida 2"])

    def test_extrai_programacao_do_layout_atual_toggle_schedule(self):
        html = """
        <main>
          <h2>Schedule</h2>
          <button>Toggle Schedule</button>
          <div>Friday, Mar 6</div>
          <div>10:00AM ET</div><div>Practice 1</div>
          <div>2:00PM ET</div><div>Qualifications</div>
          <div>4:30PM ET</div><div>High Line &amp; Final Practice</div>
          <div>Saturday, Mar 7</div>
          <div>3:00PM ET</div><div>Race</div>
          <button>Toggle Event Details</button>
          <div>Event Details</div>
        </main>
        """
        sessoes = indycar.extrair_sessoes(html, 2026)
        self.assertEqual(
            [s[0] for s in sessoes],
            ["Treino Livre 1", "Classificação", "Treino Final (High Line)", "Corrida"],
        )
        self.assertEqual(
            [s[1].astimezone(indycar.BRASILIA).strftime("%d/%m/%Y %H:%M") for s in sessoes],
            ["06/03/2026 12:00", "06/03/2026 16:00", "06/03/2026 18:30", "07/03/2026 17:00"],
        )


    def test_parser_preserva_pre_race_show_fora_da_indianapolis_500(self):
        self.assertEqual(indycar.traduzir("Pre-Race Show"), "Pre-Race Show")
        self.assertEqual(indycar.traduzir("Pre-Race"), "Pre-Race")

    def test_remove_pre_race_apenas_da_110th_indianapolis_500(self):
        sessoes = [("Pre-Race", None, (5, 24)), ("Pre-Race Show", None, (8, 23)), ("Corrida", None, (5, 24))]
        cartao = {"titulo": "110th Running of the Indianapolis 500"}
        filtradas = indycar.filtrar_pre_race_indianapolis(cartao, sessoes)
        self.assertEqual([s[0] for s in filtradas], ["Pre-Race Show", "Corrida"])
        outro_evento = indycar.filtrar_pre_race_indianapolis({"titulo": "Freedom 250 Grand Prix of Washington DC"}, sessoes)
        self.assertEqual([s[0] for s in outro_evento], ["Pre-Race", "Pre-Race Show", "Corrida"])



    def test_confirma_programacao_da_pagina_em_duas_leituras(self):
        from unittest.mock import patch
        html = """
        <main>
          <button>Toggle Schedule</button>
          <div>Friday, Mar 6</div>
          <div>10:00AM ET</div><div>Practice 1</div>
          <div>Find Local Radio Station</div>
        </main>
        """
        cartao = {
            "ano": 2026, "slug": "st-petersburg", "titulo": "St. Petersburg",
            "pista": "St. Petersburg", "local": "St. Petersburg, Florida",
            "data": (3, 6), "hora": (10, 0, "AM"),
        }
        with patch.object(indycar, "baixar", side_effect=[html, html]) as baixar:
            etapa = indycar.obter_etapa_confirmada(cartao)
        self.assertEqual([s["nome"] for s in etapa["sessoes"]], ["Treino Livre 1"])
        self.assertEqual(baixar.call_count, 2)

    def test_rejeita_programacao_da_pagina_instavel(self):
        from unittest.mock import patch
        parcial = """
        <main><button>Toggle Schedule</button>
        <div>Friday, Mar 6</div><div>10:00AM ET</div><div>Practice 1</div>
        <div>Find Local Radio Station</div></main>
        """
        completa = """
        <main><button>Toggle Schedule</button>
        <div>Friday, Mar 6</div><div>10:00AM ET</div><div>Practice 1</div>
        <div>2:00PM ET</div><div>Qualifications</div>
        <div>Find Local Radio Station</div></main>
        """
        cartao = {"ano": 2026, "slug": "st-petersburg"}
        with patch.object(indycar, "baixar", side_effect=[parcial, completa]):
            with self.assertRaisesRegex(ValueError, "leituras consecutivas"):
                indycar.obter_etapa_confirmada(cartao)

if __name__ == "__main__":
    unittest.main()
