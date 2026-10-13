import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("fe", ROOT / "atualizar_fe.py")
fe = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(fe)


class PreservacaoFormulaETest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.arquivo = Path(self.temp.name) / "fe_calendar.json"
        self.anterior = {
            "temporada": "2026/27",
            "categoria": "Formula E",
            "etapas": [
                {
                    "etapa": 1,
                    "circuito": "São Paulo",
                    "pais": "Brasil",
                    "sessoes": [
                        {"nome": "Corrida (Rodada 6)", "data": "13/03/2027", "horario": "14:05"}
                    ],
                },
                {
                    "etapa": 2,
                    "circuito": "Berlim",
                    "pais": "Alemanha",
                    "sessoes": [
                        {"nome": "Corrida (Rodada 10)", "data": "08/05/2027", "horario": "13:00"}
                    ],
                },
            ],
        }
        self.arquivo.write_text(json.dumps(self.anterior), encoding="utf-8")

    def test_preserva_horario_conhecido_do_mesmo_evento(self):
        novas = [{
            "etapa": 3,
            "circuito": "São Paulo",
            "pais": "Brasil",
            "sessoes": [
                {"nome": "Corrida (Rodada 7)", "data": "13/03/2027", "horario": "A confirmar"}
            ],
        }]
        resultado = fe.preservar_detalhes_conhecidos(novas, self.arquivo)
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "14:05")

    def test_aceita_remocao_oficial_de_sessao(self):
        novas = [{
            "etapa": 1,
            "circuito": "São Paulo",
            "pais": "Brasil",
            "sessoes": [],
        }]
        resultado = fe.preservar_detalhes_conhecidos(novas, self.arquivo)
        self.assertEqual(resultado[0]["sessoes"], [])

    def test_aceita_remocao_oficial_de_etapa(self):
        resultado = fe.preservar_detalhes_conhecidos([], self.arquivo)
        self.assertEqual(resultado, [])

    def test_nao_transfere_horario_para_evento_diferente_apos_renumeracao(self):
        novas = [{
            "etapa": 1,
            "circuito": "Austin",
            "pais": "Estados Unidos",
            "sessoes": [
                {"nome": "Corrida (Rodada 6)", "data": "13/03/2027", "horario": "A confirmar"}
            ],
        }]
        resultado = fe.preservar_detalhes_conhecidos(novas, self.arquivo)
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "A confirmar")

    def test_nao_substitui_data_oficial_publicada(self):
        novas = [{
            "etapa": 1,
            "circuito": "São Paulo",
            "pais": "Brasil",
            "inicio": "14/03/2027",
            "fim": "14/03/2027",
            "sessoes": [
                {"nome": "Corrida (Rodada 6)", "data": "14/03/2027", "horario": "A confirmar"}
            ],
        }]
        resultado = fe.preservar_detalhes_conhecidos(novas, self.arquivo)
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "A confirmar")

    def test_temporada_nova_nao_reutiliza_horarios_antigos(self):
        novas = [{
            "etapa": 1,
            "circuito": "São Paulo",
            "pais": "Brasil",
            "sessoes": [
                {"nome": "Corrida (Rodada 6)", "data": "13/03/2028", "horario": "A confirmar"}
            ],
        }]
        resultado = fe.preservar_detalhes_conhecidos(
            novas, self.arquivo, temporada_nova="2027/28"
        )
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "A confirmar")


if __name__ == "__main__":
    unittest.main()
