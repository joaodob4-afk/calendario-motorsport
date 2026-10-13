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

SCRIPT_FE = RAIZ / ".github" / "scripts" / "atualizar_fe.py"
SPEC_FE = importlib.util.spec_from_file_location("atualizar_fe", SCRIPT_FE)
formula_e = importlib.util.module_from_spec(SPEC_FE)
SPEC_FE.loader.exec_module(formula_e)


class AtualizadorIndyCarTest(unittest.TestCase):
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

    def test_html_sem_bloco_de_programacao_nao_inventa_sessoes(self):
        self.assertEqual(indycar.extrair_sessoes("<html>Sem programação</html>", 2026), [])

    def test_recusa_reduzir_calendario_existente(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar",
                "etapas": [
                    {"etapa": n, "sessoes": [{"nome": "Corrida"}]}
                    for n in range(1, 18)
                ],
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "arquivo atual tem 17"):
                indycar.validar_nao_reduzir_calendario(
                    [{"etapa": n, "sessoes": [{"nome": "Corrida"}]} for n in range(1, 11)], arquivo
                )

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


    def test_recusa_remover_sessao_conhecida_na_mesma_temporada(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [{
                    "etapa": 1, "nome": "Grande Prêmio de teste",
                    "sessoes": [
                        {"nome": "Treino Livre 1", "data": "01/05/2026", "horario": "11:00"},
                        {"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"},
                    ],
                }],
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "sessão 'Treino Livre 1'.*ausente"):
                indycar.validar_nao_reduzir_calendario([{
                    "etapa": 1, "nome": "Grande Prêmio de teste",
                    "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}],
                }], arquivo, temporada_nova=2026)

    def test_recusa_perder_horario_conhecido_na_mesma_temporada(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text(json.dumps({
                "categoria": "IndyCar", "temporada": 2026,
                "etapas": [{
                    "etapa": 1, "nome": "Grande Prêmio de teste",
                    "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "16:00"}],
                }],
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "horário conhecido removido"):
                indycar.validar_nao_reduzir_calendario([{
                    "etapa": 1, "nome": "Grande Prêmio de teste",
                    "sessoes": [{"nome": "Corrida", "data": "03/05/2026", "horario": "A confirmar"}],
                }], arquivo, temporada_nova=2026)

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

    def test_nao_sobrescreve_calendario_anterior_com_estrutura_invalida(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text('{"categoria":"IndyCar","etapas":[]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "estrutura do calendário anterior inválida"):
                indycar.validar_nao_reduzir_calendario(
                    [{"etapa": n} for n in range(1, 11)], arquivo
                )



class AtualizadorFormulaETest(unittest.TestCase):
    def test_recusa_remover_sessao_conhecida_na_mesma_temporada(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "fe_calendar.json"
            arquivo.write_text(json.dumps({
                "temporada": "2026/27",
                "etapas": [{
                    "etapa": 1,
                    "sessoes": [
                        {"nome": "Corrida (Rodada 1)", "data": "18/12/2026", "horario": "A confirmar"},
                        {"nome": "Corrida (Rodada 2)", "data": "19/12/2026", "horario": "A confirmar"},
                    ],
                }],
            }), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "Rodada 2.*ausente"):
                formula_e.preservar_detalhes_conhecidos(
                    [{"etapa": 1, "sessoes": [
                        {"nome": "Corrida (Rodada 1)", "data": "18/12/2026", "horario": "A confirmar"},
                    ]}],
                    arquivo,
                    temporada_nova="2026/27",
                )

    def test_preserva_data_e_horario_conhecidos(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "fe_calendar.json"
            arquivo.write_text(json.dumps({
                "temporada": "2026/27",
                "etapas": [{
                    "etapa": 1,
                    "sessoes": [{
                        "nome": "Corrida (Rodada 1)",
                        "data": "18/12/2026",
                        "horario": "14:00",
                    }],
                }],
            }), encoding="utf-8")

            novas = [{"etapa": 1, "sessoes": [{
                "nome": "Corrida (Rodada 1)",
                "data": "A confirmar",
                "horario": "A confirmar",
            }]}]
            resultado = formula_e.preservar_detalhes_conhecidos(
                novas, arquivo, temporada_nova="2026/27"
            )
            self.assertEqual(resultado[0]["sessoes"][0]["data"], "18/12/2026")
            self.assertEqual(resultado[0]["sessoes"][0]["horario"], "14:00")

    def test_temporada_nova_nao_reutiliza_sessoes_antigas(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "fe_calendar.json"
            arquivo.write_text(json.dumps({
                "temporada": "2026/27",
                "etapas": [{
                    "etapa": 1,
                    "sessoes": [{"nome": "Corrida (Rodada 1)", "data": "18/12/2026", "horario": "14:00"}],
                }],
            }), encoding="utf-8")

            novas = [{"etapa": 1, "sessoes": [
                {"nome": "Corrida (Rodada 1)", "data": "20/12/2027", "horario": "A confirmar"},
            ]}]
            resultado = formula_e.preservar_detalhes_conhecidos(
                novas, arquivo, temporada_nova="2027/28"
            )
            self.assertEqual(resultado[0]["sessoes"][0]["data"], "20/12/2027")
            self.assertEqual(resultado[0]["sessoes"][0]["horario"], "A confirmar")


if __name__ == "__main__":
    unittest.main()
