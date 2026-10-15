"""Valida os JSONs de calendário embutidos no app (app/src/main/assets).

Os atualizadores diários gravam esses arquivos. Se um deles gerar algo fora do padrão que
o app espera, estes testes falham no build, antes de o problema chegar ao celular.
"""
import json
import re
import unittest
from datetime import datetime, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
ASSETS = RAIZ / "app" / "src" / "main" / "assets"
ARQUIVOS = ["f2_calendar.json", "f3_calendar.json", "fe_calendar.json", "indycar_calendar.json"]
CAMPOS_ETAPA = {"etapa", "circuito", "pais", "inicio", "fim", "sessoes"}
HORARIO = re.compile(r"^\d{2}:\d{2}$")


def data(texto):
    return datetime.strptime(texto, "%d/%m/%Y").date()


class CalendariosEmbutidosTest(unittest.TestCase):
    def carregar(self, nome):
        caminho = ASSETS / nome
        self.assertTrue(caminho.is_file(), f"{nome} não encontrado em {ASSETS}")
        return json.loads(caminho.read_text(encoding="utf-8"))

    def test_estrutura_basica(self):
        for nome in ARQUIVOS:
            with self.subTest(arquivo=nome):
                dados = self.carregar(nome)
                self.assertTrue(str(dados.get("temporada", "")).strip(), "temporada vazia")
                self.assertTrue(str(dados.get("categoria", "")).strip(), "categoria vazia")
                self.assertTrue(dados.get("etapas"), "lista de etapas vazia")

    def test_campos_obrigatorios_em_cada_etapa(self):
        for nome in ARQUIVOS:
            for etapa in self.carregar(nome)["etapas"]:
                with self.subTest(arquivo=nome, etapa=etapa.get("etapa")):
                    faltando = CAMPOS_ETAPA - set(etapa)
                    self.assertFalse(faltando, f"campos ausentes: {sorted(faltando)}")
                    self.assertTrue(str(etapa["circuito"]).strip(), "circuito vazio")
                    self.assertTrue(str(etapa["pais"]).strip(), "país vazio")

    def test_numeracao_das_etapas_e_sequencial(self):
        for nome in ARQUIVOS:
            with self.subTest(arquivo=nome):
                numeros = [e["etapa"] for e in self.carregar(nome)["etapas"]]
                self.assertEqual(numeros, list(range(1, len(numeros) + 1)))

    def test_datas_validas_e_em_ordem(self):
        for nome in ARQUIVOS:
            with self.subTest(arquivo=nome):
                anterior = None
                for etapa in self.carregar(nome)["etapas"]:
                    inicio, fim = data(etapa["inicio"]), data(etapa["fim"])
                    self.assertLessEqual(inicio, fim, f"etapa {etapa['etapa']}: fim antes do início")
                    if anterior:
                        self.assertGreaterEqual(inicio, anterior, f"etapa {etapa['etapa']} fora de ordem")
                    anterior = inicio

    def test_sem_etapas_duplicadas(self):
        for nome in ARQUIVOS:
            with self.subTest(arquivo=nome):
                chaves = [(e["circuito"], e["inicio"]) for e in self.carregar(nome)["etapas"]]
                self.assertEqual(len(chaves), len(set(chaves)))

    def test_sessoes_com_nome_data_e_horario_validos(self):
        for nome in ARQUIVOS:
            for etapa in self.carregar(nome)["etapas"]:
                inicio, fim = data(etapa["inicio"]), data(etapa["fim"])
                for sessao in etapa["sessoes"]:
                    with self.subTest(arquivo=nome, etapa=etapa["etapa"], sessao=sessao.get("nome")):
                        self.assertTrue(str(sessao.get("nome", "")).strip(), "nome da sessão vazio")
                        dia = data(sessao["data"])
                        self.assertTrue(
                            inicio - timedelta(days=2) <= dia <= fim + timedelta(days=2),
                            f"sessão em {sessao['data']} longe do período {etapa['inicio']}–{etapa['fim']}",
                        )
                        horario = sessao.get("horario", "")
                        self.assertTrue(
                            HORARIO.match(horario) or horario == "A confirmar",
                            f"horário inválido: {horario!r}",
                        )


if __name__ == "__main__":
    unittest.main()
