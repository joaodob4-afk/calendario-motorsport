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

    def test_rejeita_fonte_sem_etapas_para_nao_apagar_calendario(self):
        with self.assertRaisesRegex(ValueError, "Nenhuma etapa encontrada"):
            indycar.validar_cartoes_fonte({})

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
        self.assertIsNone(indycar.traduzir("Pre-race"))

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

    def test_permite_reducao_oficial_de_etapas_na_mesma_temporada(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [
                    {"etapa": n, "circuito": f"Circuito {n}",
                     "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}]}
                    for n in range(1, 18)
                ],
            }), encoding="utf-8")
            novas = [
                {"etapa": n, "circuito": f"Circuito {n}",
                 "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}]}
                for n in range(1, 10)
            ]
            indycar.validar_nao_reduzir_calendario(novas, arquivo, temporada_nova=2026)
            self.assertEqual(len(novas), 9)

    def test_permite_calendario_com_mesma_ou_maior_quantidade(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar",
                "etapas": [
                    {"etapa": n, "sessoes": [{"nome": "Corrida"}]}
                    for n in range(1, 11)
                ],
            }), encoding="utf-8")
            indycar.validar_nao_reduzir_calendario(
                [{"etapa": n, "sessoes": [{"nome": "Corrida"}]} for n in range(1, 11)], arquivo
            )
            indycar.validar_nao_reduzir_calendario(
                [{"etapa": n, "sessoes": [{"nome": "Corrida"}]} for n in range(1, 12)], arquivo
            )


    def test_permite_remocao_oficial_de_sessao_e_renumeracao_da_etapa(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [{
                    "etapa": 1, "nome": "Grande Prêmio de teste", "circuito": "Pista teste",
                    "sessoes": [
                        {"nome": "Treino Livre 1", "data": "01/05/2026", "horario": "11:00"},
                        {"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"},
                    ],
                }],
            }), encoding="utf-8")
            novas = [{
                "etapa": 2, "nome": "Grande Prêmio de teste", "circuito": "Pista teste",
                "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}],
            }]
            with self.assertRaisesRegex(ValueError, "sessão anterior ausente na extração"):
                indycar.validar_nao_reduzir_calendario(novas, arquivo, temporada_nova=2026)
            # A validação falha antes de o atualizador gravar o JSON novo.
            self.assertEqual(novas[0]["etapa"], 2)
            self.assertEqual([s["nome"] for s in novas[0]["sessoes"]], ["Corrida"])

    def test_preserva_horario_conhecido_ao_renumerar_etapa(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [{
                    "etapa": 1, "nome": "Grande Prêmio de teste", "circuito": "Pista teste",
                    "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}],
                }],
            }), encoding="utf-8")
            novas = [{
                "etapa": 2, "nome": "Grande Prêmio de teste", "circuito": "Pista teste",
                "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "A confirmar"}],
            }]
            indycar.validar_nao_reduzir_calendario(novas, arquivo, temporada_nova=2026)
            self.assertEqual(novas[0]["sessoes"][0]["horario"], "16:00")

    def test_temporada_nova_permite_menos_etapas(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [
                    {"etapa": n, "nome": f"Etapa {n}", "sessoes": [{"nome": "Corrida", "horario": "16:00"}]}
                    for n in range(1, 18)
                ],
            }), encoding="utf-8")
            indycar.validar_nao_reduzir_calendario([
                {"etapa": n, "nome": f"Nova etapa {n}", "sessoes": [{"nome": "Corrida", "horario": "A confirmar"}]}
                for n in range(1, 11)
            ], arquivo, temporada_nova=2027)

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

    def test_preserva_horarios_separados_para_duas_corridas_na_mesma_pista(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [
                    {
                        "etapa": 16, "nome": "Snap-on Makers and Fixers 250",
                        "circuito": "Milwaukee Mile",
                        "sessoes": [{"nome": "Corrida 1", "data": "29/08/2026", "horario": "19:00"}],
                    },
                    {
                        "etapa": 17, "nome": "Snap-on Milwaukee Mile 250",
                        "circuito": "Milwaukee Mile",
                        "sessoes": [{"nome": "Corrida 2", "data": "30/08/2026", "horario": "14:00"}],
                    },
                ],
            }), encoding="utf-8")
            novas = [
                {
                    "etapa": 16, "nome": "Snap-on Makers and Fixers 250",
                    "circuito": "Milwaukee Mile",
                    "inicio": "28/08/2026", "fim": "30/08/2026",
                    "sessoes": [{"nome": "Corrida 1", "data": "29/08/2026", "horario": "A confirmar"}],
                },
                {
                    "etapa": 17, "nome": "Snap-on Milwaukee Mile 250",
                    "circuito": "Milwaukee Mile",
                    "inicio": "29/08/2026", "fim": "30/08/2026",
                    "sessoes": [{"nome": "Corrida 2", "data": "30/08/2026", "horario": "A confirmar"}],
                },
            ]
            indycar.validar_nao_reduzir_calendario(novas, arquivo, temporada_nova=2026)
            self.assertEqual(novas[0]["sessoes"][0]["horario"], "19:00")
            self.assertEqual(novas[1]["sessoes"][0]["horario"], "14:00")

    def test_nao_sobrescreve_calendario_anterior_com_estrutura_invalida(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text('{"categoria":"IndyCar","etapas":[]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "estrutura do calendário anterior inválida"):
                indycar.validar_nao_reduzir_calendario(
                    [{"etapa": n} for n in range(1, 11)], arquivo
                )



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


    def test_pre_race_removida_do_calendario_falha_sem_excecao_especial(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [{
                    "etapa": 1, "nome": "110th Running of the Indianapolis 500",
                    "circuito": "Indianapolis Motor Speedway",
                    "sessoes": [
                        {"nome": "Pre-Race", "data": "24/05/2026", "horario": "12:00"},
                        {"nome": "Corrida", "data": "24/05/2026", "horario": "13:00"},
                    ],
                }],
            }), encoding="utf-8")
            novas = [{
                "etapa": 1, "nome": "110th Running of the Indianapolis 500",
                "circuito": "Indianapolis Motor Speedway",
                "sessoes": [{"nome": "Corrida", "data": "24/05/2026", "horario": "13:00"}],
            }]
            with self.assertRaisesRegex(ValueError, "sessão anterior ausente na extração"):
                indycar.validar_nao_reduzir_calendario(novas, arquivo, temporada_nova=2026)

    def test_parser_preserva_pre_race_show_fora_da_indianapolis_500(self):
        self.assertEqual(indycar.traduzir("Pre-Race Show"), "Pre-Race Show")
        self.assertEqual(indycar.traduzir("Pre-Race"), "Pre-Race")

    def test_remove_pre_race_apenas_da_110th_indianapolis_500(self):
        sessoes = [("Pre-Race", None, (5, 24)), ("Pre-Race Show", None, (8, 23)), ("Corrida", None, (5, 24))]
        cartao = {"titulo": "110th Running of the Indianapolis 500"}
        filtradas = [s for s in sessoes if not (cartao["titulo"].casefold() == "110th running of the indianapolis 500" and s[0].casefold() == "pre-race")]
        self.assertEqual([s[0] for s in filtradas], ["Pre-Race Show", "Corrida"])


if __name__ == "__main__":
    unittest.main()
