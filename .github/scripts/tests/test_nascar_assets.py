import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "app" / "src" / "main" / "assets"

CALENDARIOS = {
    "nascar_cup_calendar.json": "NASCAR Cup Series",
    "nascar_xfinity_calendar.json": "NASCAR Xfinity Series",
    "nascar_truck_calendar.json": "NASCAR Craftsman Truck Series",
}


class NascarAssetsTest(unittest.TestCase):
    def test_calendarios_nascar_incluem_etapas_e_sessoes(self):
        for arquivo, categoria in CALENDARIOS.items():
            with self.subTest(categoria=categoria):
                caminho = ASSETS / arquivo
                self.assertTrue(caminho.is_file(), f"Arquivo ausente: {arquivo}")
                dados = json.loads(caminho.read_text(encoding="utf-8"))
                self.assertEqual(dados.get("categoria"), categoria)
                self.assertEqual(dados.get("temporada"), 2026)
                etapas = dados.get("etapas", [])
                self.assertGreater(len(etapas), 0, f"{categoria} sem etapas")
                self.assertTrue(
                    all(etapa.get("sessoes") for etapa in etapas),
                    f"{categoria} tem etapa sem sessão"
                )


if __name__ == "__main__":
    unittest.main()
