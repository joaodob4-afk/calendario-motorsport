import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from atualizar_nascar import extrair_temporada, converter_instante, atualizar


def corrida(nome, pista, sessoes):
    return {"race_name": nome, "track_name": pista, "schedule": sessoes}


def sessao(nome, tipo, hora):
    return {"event_name": nome, "run_type": tipo, "start_time_utc": hora}


class AtualizarNascarTest(unittest.TestCase):
    def setUp(self):
        self.cup = [
            corrida("Daytona 500", "Daytona International Speedway", [
                sessao("Practice 1", 1, "2026-02-11T15:00:00Z"),
                sessao("Qualifying", 2, "2026-02-12T18:00:00Z"),
                sessao("Race", 3, "2026-02-15T18:30:00Z"),
            ]),
        ]
        self.xfinity = [
            corrida("United Rentals 300", "Daytona International Speedway", [
                sessao("Practice", 1, "2026-02-13T15:00:00Z"),
                sessao("Race", 3, "2026-02-14T17:00:00Z"),
            ]),
        ]
        self.truck = [
            corrida("Fresh From Florida 250", "Daytona International Speedway", [
                sessao("Practice", 1, "2026-02-12T15:00:00Z"),
                sessao("Race", 3, "2026-02-13T19:00:00Z"),
            ]),
            corrida("North Carolina Education Lottery 200", "Charlotte Motor Speedway", [
                sessao("Practice", 1, "2026-05-22T20:30:00Z"),
                sessao("Qualifying", 2, "2026-05-22T21:35:00Z"),
                sessao("Race", 3, "2026-05-24T14:00:00Z"),
            ]),
        ]

    def test_preserva_nome_oficial_e_sessoes_da_corrida(self):
        dados = extrair_temporada(2026, 1, self.cup)
        etapa = dados["etapas"][0]
        self.assertEqual(etapa["nomeEvento"], "Daytona 500")
        self.assertEqual([s["nome"] for s in etapa["sessoes"]],
                         ["Practice 1", "Qualifying", "Corrida"])
        self.assertEqual(etapa["inicio"], "11/02/2026")
        self.assertEqual(etapa["fim"], "15/02/2026")

    def test_feed_de_truck_nao_inclui_corridas_de_outras_series(self):
        dados = extrair_temporada(2026, 3, self.truck)
        self.assertEqual(len(dados["etapas"]), 2)
        charlotte = dados["etapas"][1]
        self.assertEqual(charlotte["nomeEvento"], "North Carolina Education Lottery 200")
        self.assertEqual([s["data"] for s in charlotte["sessoes"] if s["nome"] == "Corrida"],
                         ["24/05/2026"])

    def test_pista_fora_dos_eua_tem_pais_correto(self):
        canada = [corrida("Canadian race", "Circuit Gilles Villeneuve", [
            sessao("Race", 3, "2026-08-01T18:00:00Z")])]
        mexico = [corrida("Mexico race", "Autódromo Hermanos Rodríguez", [
            sessao("Race", 3, "2026-06-01T18:00:00Z")])]
        self.assertEqual(extrair_temporada(2026, 1, canada)["etapas"][0]["pais"], "Canadá")
        self.assertEqual(extrair_temporada(2026, 1, mexico)["etapas"][0]["pais"], "México")

    def test_horario_utc_e_convertido_para_brasilia(self):
        self.assertEqual(converter_instante("2026-02-15T18:30:00Z").strftime("%H:%M"), "15:30")

    def test_resposta_vazia_nao_gera_calendario_invalido(self):
        with self.assertRaises(ValueError):
            extrair_temporada(2026, 1, [])

    def test_atualizador_baixa_feed_individual_de_cada_serie(self):
        respostas = {
            "https://cf.nascar.com/cacher/2026/1/race_list_basic.json": self.cup,
            "https://cf.nascar.com/cacher/2026/2/race_list_basic.json": self.xfinity,
            "https://cf.nascar.com/cacher/2026/3/race_list_basic.json": self.truck,
        }
        chamadas = []

        def baixar(url):
            chamadas.append(url)
            return respostas[url]

        with tempfile.TemporaryDirectory() as pasta:
            resultado = atualizar(2026, Path(pasta), baixar)
            self.assertEqual(chamadas, list(respostas))
            self.assertEqual(len(resultado), 3)
            for arquivo in ("nascar_cup_calendar.json", "nascar_xfinity_calendar.json",
                            "nascar_truck_calendar.json"):
                self.assertTrue((Path(pasta) / arquivo).is_file())


if __name__ == "__main__":
    unittest.main()
