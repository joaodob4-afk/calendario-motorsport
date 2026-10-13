import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def carregar_modulo(nome):
    spec = importlib.util.spec_from_file_location(nome, ROOT / f"atualizar_{nome}.py")
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


f2 = carregar_modulo("f2")
f3 = carregar_modulo("f3")


class PreservacaoF2F3Test(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.arquivo = Path(self.temp.name) / "calendario.json"
        self.anterior = {
            "temporada": 2026,
            "etapas": [{
                "etapa": 1,
                "inicio": "01/05/2026",
                "fim": "03/05/2026",
                "sessoes": [{
                    "nome": "Corrida Feature",
                    "data": "03/05/2026",
                    "horario": "10:25",
                }],
            }],
        }
        self.arquivo.write_text(json.dumps(self.anterior), encoding="utf-8")
        self.novas = [{
            "etapa": 1,
            "inicio": "",
            "fim": "A confirmar",
            "sessoes": [{
                "nome": "Corrida Feature",
                "data": "A confirmar",
                "horario": "TBD",
            }],
        }]

    def test_f2_preserva_datas_e_horario_conhecidos(self):
        resultado = f2.preservar_sessoes_conhecidas(
            self.novas, self.arquivo, temporada_nova=2026
        )
        self.assertEqual(resultado[0]["inicio"], "01/05/2026")
        self.assertEqual(resultado[0]["fim"], "03/05/2026")
        self.assertEqual(resultado[0]["sessoes"][0]["data"], "03/05/2026")
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "10:25")

    def test_f3_preserva_datas_e_horario_conhecidos(self):
        resultado = f3.preservar_sessoes_conhecidas(
            self.novas, self.arquivo, temporada_nova=2026
        )
        self.assertEqual(resultado[0]["sessoes"][0]["data"], "03/05/2026")
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "10:25")

    def test_f2_rejeita_sessao_conhecida_ausente(self):
        self.novas[0]["sessoes"] = []
        with self.assertRaisesRegex(ValueError, "sessão.*ausente"):
            f2.preservar_sessoes_conhecidas(
                self.novas, self.arquivo, temporada_nova=2026
            )

    def test_f3_rejeita_etapa_conhecida_ausente(self):
        with self.assertRaisesRegex(ValueError, "etapa 1 ausente"):
            f3.preservar_sessoes_conhecidas(
                [], self.arquivo, temporada_nova=2026
            )

    def test_temporada_nova_nao_reutiliza_dados_antigos(self):
        resultado = f2.preservar_sessoes_conhecidas(
            self.novas, self.arquivo, temporada_nova=2027
        )
        self.assertEqual(resultado[0]["sessoes"][0]["horario"], "TBD")
        self.assertEqual(resultado[0]["inicio"], "")


if __name__ == "__main__":
    unittest.main()
