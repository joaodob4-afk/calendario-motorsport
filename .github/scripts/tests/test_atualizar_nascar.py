import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from atualizar_nascar import extrair_temporada, converter_instante, atualizar


def sessao(serie, race_id, corrida, pista, nome, tipo, hora):
    return {
        "series_id": serie,
        "race_id": race_id,
        "race_name": corrida,
        "track_name": pista,
        "event_name": nome,
        "run_type": tipo,
        "start_time_utc": hora,
    }


class AtualizarNascarTest(unittest.TestCase):
    def setUp(self):
        self.cup = [
            sessao(1, 1, "Daytona 500", "Daytona International Speedway",
                   "Practice 1", 1, "2026-02-11T15:00:00Z"),
            sessao(1, 1, "Daytona 500", "Daytona International Speedway",
                   "Qualifying", 2, "2026-02-12T18:00:00Z"),
            sessao(1, 1, "Daytona 500", "Daytona International Speedway",
                   "Race", 3, "2026-02-15T18:30:00Z"),
            # Mesmo fim de semana, mas pertencem às outras séries.
            sessao(2, 2, "United Rentals 300", "Daytona International Speedway",
                   "Race", 3, "2026-02-14T17:00:00Z"),
            sessao(3, 3, "Fresh From Florida 250", "Daytona International Speedway",
                   "Race", 3, "2026-02-13T19:00:00Z"),
        ]
        self.truck_charlotte = [
            sessao(3, 30, "North Carolina Education Lottery 200",
                   "Charlotte Motor Speedway", "Practice", 1, "2026-05-22T20:30:00Z"),
            sessao(3, 30, "North Carolina Education Lottery 200",
                   "Charlotte Motor Speedway", "Qualifying", 2, "2026-05-22T21:35:00Z"),
            # Corridas de outras séries não podem entrar na etapa Truck.
            sessao(1, 31, "Coca-Cola 600", "Charlotte Motor Speedway",
                   "Race", 3, "2026-05-24T22:00:00Z"),
            sessao(2, 32, "Charlotte O'Reilly Race", "Charlotte Motor Speedway",
                   "Race", 3, "2026-05-23T21:00:00Z"),
            sessao(3, 30, "North Carolina Education Lottery 200",
                   "Charlotte Motor Speedway", "Race", 3, "2026-05-24T14:00:00Z"),
        ]

    def test_filtra_serie_e_preserva_nome_oficial_do_evento(self):
        dados = extrair_temporada(2026, 1, self.cup)
        self.assertEqual(len(dados["etapas"]), 1)
        etapa = dados["etapas"][0]
        self.assertEqual(etapa["nomeEvento"], "Daytona 500")
        self.assertEqual([s["nome"] for s in etapa["sessoes"]],
                         ["Practice 1", "Qualifying", "Corrida"])
        self.assertEqual(etapa["inicio"], "11/02/2026")
        self.assertEqual(etapa["fim"], "15/02/2026")

    def test_charlotte_truck_nao_recebe_corridas_das_outras_series(self):
        dados = extrair_temporada(2026, 3, self.truck_charlotte)
        self.assertEqual(len(dados["etapas"]), 1)
        etapa = dados["etapas"][0]
        corridas = [s for s in etapa["sessoes"] if s["nome"] == "Corrida"]
        self.assertEqual(len(corridas), 1)
        self.assertEqual(corridas[0]["data"], "24/05/2026")
        self.assertEqual(etapa["nomeEvento"], "North Carolina Education Lottery 200")

    def test_pista_fora_dos_eua_tem_pais_correto(self):
        canada = [
            sessao(1, 4, "Canadian race", "Circuit Gilles Villeneuve",
                   "Race", 3, "2026-08-01T18:00:00Z")
        ]
        mexico = [
            sessao(1, 5, "Mexico race", "Autódromo Hermanos Rodríguez",
                   "Race", 3, "2026-06-01T18:00:00Z")
        ]
        self.assertEqual(extrair_temporada(2026, 1, canada)["etapas"][0]["pais"], "Canadá")
        self.assertEqual(extrair_temporada(2026, 1, mexico)["etapas"][0]["pais"], "México")

    def test_horario_utc_e_convertido_para_brasilia(self):
        self.assertEqual(converter_instante("2026-02-15T18:30:00Z").strftime("%H:%M"), "15:30")

    def test_resposta_vazia_nao_gera_calendario_invalido(self):
        with self.assertRaises(ValueError):
            extrair_temporada(2026, 1, [])

    def test_atualizador_consulta_cada_serie_separadamente(self):
        respostas = {
            "https://feed.nascar.com/api/weekendschedule?series_id=1&race_season=2026&v=2": self.cup[:3],
            "https://feed.nascar.com/api/weekendschedule?series_id=2&race_season=2026&v=2": [
                self.cup[3]
            ],
            "https://feed.nascar.com/api/weekendschedule?series_id=3&race_season=2026&v=2": [
                self.cup[4]
            ],
        }
        chamadas = []

        def baixar(url):
            chamadas.append(url)
            return respostas[url]

        with tempfile.TemporaryDirectory() as pasta:
            resultado = atualizar(2026, Path(pasta), baixar)
            self.assertEqual(len(chamadas), 3)
            self.assertEqual(len(resultado), 3)
            self.assertTrue((Path(pasta) / "nascar_cup_calendar.json").is_file())
            self.assertTrue((Path(pasta) / "nascar_xfinity_calendar.json").is_file())
            self.assertTrue((Path(pasta) / "nascar_truck_calendar.json").is_file())


if __name__ == "__main__":
    unittest.main()
