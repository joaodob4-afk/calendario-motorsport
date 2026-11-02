import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from atualizar_nascar import extrair_temporada, converter_instante, atualizar


def sessao(nome, tipo, hora):
    return {"event_name": nome, "run_type": tipo, "start_time_utc": hora}


class AtualizarNascarTest(unittest.TestCase):
    def setUp(self):
        self.cup = [
            {
                "race_id": 1,
                "race_name": "Daytona 500",
                "track_name": "Daytona International Speedway",
                "schedule": [
                    sessao("Haulers Enter", 0, "2026-02-10T10:00:00"),
                    sessao("Practice 1", 1, "2026-02-11T15:00:00"),
                    sessao("Qualifying", 2, "2026-02-12T18:00:00"),
                    sessao("Race", 3, "2026-02-15T18:30:00"),
                ],
            }
        ]
        self.shared = {
            "series_2": [{
                "race_id": 2, "race_name": "Xfinity 300",
                "track_name": "Daytona International Speedway",
                "schedule": [sessao("Race", 3, "2026-02-14T17:00:00")],
            }],
            "series_3": [{
                "race_id": 3, "race_name": "Truck 250",
                "track_name": "Daytona International Speedway",
                "schedule": [sessao("Practice 1", 1, "2026-02-13T22:00:00"),
                             sessao("Race", 3, "2026-02-14T19:00:00")],
            }],
        }

    def test_cup_ignora_eventos_administrativos_e_preserva_sessoes(self):
        dados = extrair_temporada(2026, 1, self.cup)
        etapa = dados["etapas"][0]
        self.assertEqual(etapa["circuito"], "Daytona International Speedway")
        self.assertEqual([s["nome"] for s in etapa["sessoes"]],
                         ["Practice 1", "Qualifying", "Corrida"])
        self.assertEqual(etapa["inicio"], "11/02/2026")
        self.assertEqual(etapa["fim"], "15/02/2026")

    def test_xfinity_e_truck_sao_lidos_das_chaves_corretas(self):
        xfinity = extrair_temporada(2026, 2, self.shared)
        truck = extrair_temporada(2026, 3, self.shared)
        self.assertEqual(xfinity["categoria"], "NASCAR Xfinity Series")
        self.assertEqual(xfinity["etapas"][0]["sessoes"][0]["nome"], "Corrida")
        self.assertEqual(truck["categoria"], "NASCAR Craftsman Truck Series")
        self.assertEqual(len(truck["etapas"][0]["sessoes"]), 2)

    def test_pista_fora_dos_eua_tem_pais_correto(self):
        canada = [{
            "race_id": 4, "race_name": "Race", "track_name": "Circuit Gilles Villeneuve",
            "schedule": [sessao("Race", 3, "2026-08-01T18:00:00")],
        }]
        mexico = [{
            "race_id": 5, "race_name": "Race", "track_name": "Autódromo Hermanos Rodríguez",
            "schedule": [sessao("Race", 3, "2026-06-01T18:00:00")],
        }]
        self.assertEqual(extrair_temporada(2026, 1, canada)["etapas"][0]["pais"], "Canadá")
        self.assertEqual(extrair_temporada(2026, 1, mexico)["etapas"][0]["pais"], "México")

    def test_horario_sem_fuso_e_interpretado_como_utc(self):
        self.assertEqual(converter_instante("2026-02-15T18:30:00").strftime("%H:%M"), "15:30")

    def test_resposta_vazia_nao_gera_calendario_invalido(self):
        with self.assertRaises(ValueError):
            extrair_temporada(2026, 1, [])

    def test_atualizador_baixa_apenas_duas_respostas_e_gera_tres_jsons(self):
        respostas = {
            "https://cf.nascar.com/cacher/2026/1/race_list_basic.json": self.cup,
            "https://cf.nascar.com/cacher/2026/race_list_basic.json": self.shared,
        }
        chamadas = []
        def baixar(url):
            chamadas.append(url)
            return respostas[url]
        with tempfile.TemporaryDirectory() as pasta:
            resultado = atualizar(2026, Path(pasta), baixar)
            self.assertEqual(len(chamadas), 2)
            self.assertEqual(len(resultado), 3)
            self.assertTrue((Path(pasta) / "nascar_cup_calendar.json").is_file())
            self.assertTrue((Path(pasta) / "nascar_xfinity_calendar.json").is_file())
            self.assertTrue((Path(pasta) / "nascar_truck_calendar.json").is_file())


if __name__ == "__main__":
    unittest.main()
