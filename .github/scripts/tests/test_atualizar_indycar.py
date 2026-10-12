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
            with self.assertRaisesRegex(ValueError, "17 etapas"):
                indycar.validar_nao_reduzir_calendario(
                    [{"etapa": n} for n in range(1, 11)], arquivo
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
                [{"etapa": n} for n in range(1, 11)], arquivo
            )
            indycar.validar_nao_reduzir_calendario(
                [{"etapa": n} for n in range(1, 12)], arquivo
            )

    def test_nao_sobrescreve_calendario_anterior_com_estrutura_invalida(self):
        with tempfile.TemporaryDirectory() as pasta:
            arquivo = Path(pasta) / "indycar_calendar.json"
            arquivo.write_text('{"categoria":"IndyCar","etapas":[]}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "estrutura do calendário anterior inválida"):
                indycar.validar_nao_reduzir_calendario(
                    [{"etapa": n} for n in range(1, 11)], arquivo
                )


if __name__ == "__main__":
    unittest.main()
