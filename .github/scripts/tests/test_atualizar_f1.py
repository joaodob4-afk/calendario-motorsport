"""Testes offline do atualizador da F1 (sem rede)."""
import copy
import importlib.util
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location(
    "atualizar_f1", RAIZ / ".github" / "scripts" / "atualizar_f1.py"
)
f1 = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(f1)


def evento(numero, circuito, dia, extras=()):
    agenda = [
        {"code": "FP1", "timestamp": f"2026-03-{dia - 2:02d}T01:30:00+00:00"},
        {"code": "Q", "timestamp": f"2026-03-{dia - 1:02d}T05:00:00Z"},
        {"code": "R", "timestamp": f"2026-03-{dia:02d}T04:00:00Z"},
        *extras,
    ]
    return {
        "round": {"number": numero, "name": f"GP {circuito}"},
        "circuit": {"id": circuito, "name": circuito, "locality": "Cidade", "country": "Pais"},
        "schedule": agenda,
    }


def resposta(*eventos, ano=2026):
    return {"data": {"year": ano, "events": list(eventos)}}


class NormalizarTest(unittest.TestCase):
    def test_converte_para_formato_ergast_do_app(self):
        dados = f1.normalizar(resposta(evento(1, "albert_park", 15)))
        tabela = dados["MRData"]["RaceTable"]
        self.assertEqual(tabela["season"], "2026")
        corrida = tabela["Races"][0]
        self.assertEqual(corrida["round"], "1")
        self.assertEqual(corrida["Circuit"]["circuitId"], "albert_park")
        self.assertEqual(corrida["date"], "2026-03-15")
        self.assertEqual(corrida["time"], "04:00:00Z")
        self.assertEqual(corrida["FirstPractice"], {"date": "2026-03-13", "time": "01:30:00Z"})
        self.assertEqual(corrida["Qualifying"]["date"], "2026-03-14")

    def test_converte_fuso_para_utc(self):
        ev = evento(1, "x", 15)
        ev["schedule"][2]["timestamp"] = "2026-03-15T23:30:00-03:00"
        corrida = f1.normalizar(resposta(ev))["MRData"]["RaceTable"]["Races"][0]
        self.assertEqual((corrida["date"], corrida["time"]), ("2026-03-16", "02:30:00Z"))

    def test_sprint_nos_dois_codigos_e_sessao_invalida_ignorada(self):
        extras = [
            {"code": "SQ", "timestamp": "2026-03-13T05:30:00Z"},
            {"code": "SR", "timestamp": "2026-03-14T01:00:00Z"},
            {"code": "FP2", "timestamp": None},
            {"code": "FP3", "timestamp": "lixo"},
        ]
        corrida = f1.normalizar(resposta(evento(1, "x", 15, extras)))["MRData"]["RaceTable"]["Races"][0]
        self.assertIn("SprintQualifying", corrida)
        self.assertIn("Sprint", corrida)
        self.assertNotIn("SecondPractice", corrida)
        self.assertNotIn("ThirdPractice", corrida)

    def test_sem_corrida_usa_primeira_sessao(self):
        sem_r = evento(1, "x", 15)
        sem_r["schedule"] = [sem_r["schedule"][0]]
        corridas = f1.normalizar(resposta(sem_r))["MRData"]["RaceTable"]["Races"]
        self.assertEqual(len(corridas), 1)
        self.assertEqual(corridas[0]["date"], "2026-03-13")
        self.assertNotIn("time", corridas[0])

    def test_evento_sem_circuito_e_rejeitado(self):
        ruim = {"round": {"number": 1, "name": "x"}, "schedule": []}
        with self.assertRaisesRegex(ValueError, "sem rodada ou circuito"):
            f1.normalizar(resposta(ruim))

    def test_evento_sem_data_e_rejeitado(self):
        vazio = evento(1, "x", 20)
        vazio["schedule"] = []
        with self.assertRaisesRegex(ValueError, "sem data de corrida"):
            f1.normalizar(resposta(vazio))

    def test_assinatura_detecta_sessao_ausente_entre_leituras(self):
        primeira = resposta(evento(1, "x", 15))
        segunda = copy.deepcopy(primeira)
        segunda["data"]["events"][0]["schedule"].pop(0)
        self.assertNotEqual(f1.assinatura_resposta(primeira), f1.assinatura_resposta(segunda))

    def test_assinatura_usa_fallbacks_da_normalizacao(self):
        ev = evento(1, "x", 15)
        ev["round"]["number"] = None
        ev["circuit"].pop("id")
        assinatura = f1.assinatura_resposta(resposta(ev))
        corrida = f1.normalizar(resposta(ev))["MRData"]["RaceTable"]["Races"][0]
        self.assertEqual(assinatura[0][0], corrida["round"])
        self.assertEqual(assinatura[0][1], corrida["Circuit"]["circuitId"])

    def test_assinatura_rejeita_numero_de_rodada_duplicado(self):
        primeira = evento(1, "x", 15)
        segunda = evento(1, "y", 20)
        with self.assertRaisesRegex(ValueError, "número de rodada duplicado: 1"):
            f1.assinatura_resposta(resposta(primeira, segunda))


class ValidarTest(unittest.TestCase):
    def calendario(self, n):
        return f1.normalizar(resposta(*[evento(i + 1, f"c{i}", 3 + i) for i in range(n)]))

    def test_rejeita_vazio_e_datas_repetidas(self):
        with self.assertRaises(ValueError):
            f1.validar(f1.normalizar(resposta()))
        repetido = f1.normalizar(resposta(evento(1, "a", 15), evento(2, "b", 15)))
        with self.assertRaises(ValueError):
            f1.validar(repetido)

    def test_aceita_primeiro_calendario(self):
        f1.validar(self.calendario(5))

    def test_aceita_reducao_confirmada_na_mesma_temporada(self):
        antigo = self.calendario(20)
        novo = self.calendario(3)
        f1.validar(novo)

    def test_validacao_nao_impede_remocao_oficial_de_sessoes(self):
        antigo = self.calendario(5)
        novo = copy.deepcopy(antigo)
        for corrida in f1._corridas(novo):
            corrida.pop("FirstPractice", None)
            corrida.pop("Qualifying", None)
        f1.validar(novo)

    def test_outra_temporada_pode_ter_menos_corridas(self):
        novo = f1.normalizar(resposta(*[evento(i + 1, f"c{i}", 3 + i) for i in range(5)], ano=2027))
        f1.validar(novo)


if __name__ == "__main__":
    unittest.main()
