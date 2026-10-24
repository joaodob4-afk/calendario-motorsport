"""Testes offline do atualizador de MotoGP, Moto2 e Moto3 e dos JSONs embutidos no app."""
import importlib.util
import json
import re
import unittest
from datetime import datetime, timedelta
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SCRIPT = RAIZ / ".github" / "scripts" / "atualizar_moto.py"
SPEC = importlib.util.spec_from_file_location("atualizar_moto", SCRIPT)
moto = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(moto)

ASSETS = RAIZ / "app" / "src" / "main" / "assets"


def sessao(classe, curto, tipo, inicio, nome="x", tipo_evento="SESSION"):
    return {
        "shortname": curto,
        "name": nome,
        "type": tipo_evento,
        "kind": tipo,
        "date_start": inicio,
        "category": {"acronym": classe},
    }


def gp(sequencia, sigla, inicio, fim, status="FINISHED", broadcasts=None):
    return {
        "kind": "GP",
        "sequence": sequencia,
        "shortname": sigla,
        "date_start": inicio + "T00:00:00+00:00",
        "date_end": fim + "T00:00:00+00:00",
        "status": status,
        "name": f"Nome oficial do GP {sigla}",
        "circuit": {
            "name": f"Circuito {sigla}",
            "city": f"Cidade {sigla}",
            "country": "País da API",
        },
        "broadcasts": broadcasts or [],
    }


class AtualizadorMotoTest(unittest.TestCase):
    def eventos_tailandia(self):
        return [
            gp(
                1,
                "THA",
                "2026-02-27",
                "2026-03-01",
                broadcasts=[
                    sessao("MGP", "FP1", "PRACTICE", "2026-02-27T10:45:00+0700", "Free Practice Nr. 1"),
                    sessao("MGP", "SPR", "RACE", "2026-02-28T15:00:00+0700", "Tissot Sprint"),
                    sessao("MGP", "RAC", "RACE", "2026-03-01T15:00:00+0700", "Grand Prix"),
                    sessao("MGP", "SHOW", "PRESS", "2026-02-26T17:20:00+0700", "GearUp", "MEDIA"),
                    sessao("MT2", "RAC2", "RACE", "2026-03-01T13:15:00+0700", "Race "),
                    sessao("MT3", "RAC", "RACE", "2026-03-01T12:00:00+0700", "Race"),
                    sessao("BWC", "RAC", "RACE", "2026-03-01T10:00:00+0700", "Bagger"),
                ],
            ),
            {"kind": "TEST", "sequence": 1, "shortname": "SEP", "broadcasts": []},
        ]

    def test_traduz_nomes_e_converte_para_brasilia(self):
        dados = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)
        sessoes = dados["etapas"][0]["sessoes"]

        self.assertEqual(
            [s["nome"] for s in sessoes], ["Treino Livre 1", "Corrida Sprint", "Corrida"]
        )
        # 10:45 em Bangcoc (UTC+7) são 00:45 em Brasília (UTC-3), no mesmo dia.
        self.assertEqual(sessoes[0]["data"], "27/02/2026")
        self.assertEqual(sessoes[0]["horario"], "00:45")
        self.assertEqual(sessoes[2]["horario"], "05:00")

    def test_ignora_midia_testes_e_outras_classes(self):
        dados = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)

        self.assertEqual(len(dados["etapas"]), 1)  # só o evento GP
        nomes = [s["nome"] for s in dados["etapas"][0]["sessoes"]]
        self.assertNotIn("GearUp", nomes)

    def test_cada_classe_recebe_apenas_as_suas_sessoes(self):
        moto2 = moto.montar(self.eventos_tailandia(), "MT2", "Moto2", 2026)
        moto3 = moto.montar(self.eventos_tailandia(), "MT3", "Moto3", 2026)

        self.assertEqual([s["nome"] for s in moto2["etapas"][0]["sessoes"]], ["Corrida"])
        self.assertEqual(moto2["etapas"][0]["sessoes"][0]["horario"], "03:15")
        self.assertEqual(moto3["etapas"][0]["sessoes"][0]["horario"], "02:00")
        self.assertEqual(moto2["categoria"], "Moto2")

    def test_periodo_vem_das_datas_locais_das_sessoes(self):
        dados = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)
        etapa = dados["etapas"][0]

        self.assertEqual((etapa["inicio"], etapa["fim"]), ("27/02/2026", "01/03/2026"))
        self.assertEqual(etapa["nomeEvento"], "Nome oficial do GP THA")
        self.assertEqual(etapa["circuito"], "Circuito THA")
        self.assertEqual(etapa["cidade"], "Cidade THA")
        self.assertEqual(etapa["pais"], "Tailândia")

    def test_etapa_sem_sessoes_usa_datas_do_evento(self):
        eventos = [gp(1, "VAL", "2026-11-27", "2026-11-29", status="NOT-STARTED")]
        etapa = moto.montar(eventos, "MGP", "MotoGP", 2026)["etapas"][0]

        self.assertEqual((etapa["inicio"], etapa["fim"]), ("27/11/2026", "29/11/2026"))
        self.assertEqual(etapa["sessoes"], [])

    def test_etapas_saem_numeradas_em_ordem_de_data(self):
        eventos = [
            gp(2, "BRA", "2026-03-20", "2026-03-22"),
            gp(1, "THA", "2026-02-27", "2026-03-01"),
        ]
        etapas = moto.montar(eventos, "MGP", "MotoGP", 2026)["etapas"]

        self.assertEqual([e["etapa"] for e in etapas], [1, 2])
        self.assertEqual([e["nomeEvento"] for e in etapas], ["Nome oficial do GP THA", "Nome oficial do GP BRA"])
        self.assertEqual([e["circuito"] for e in etapas], ["Circuito THA", "Circuito BRA"])

    def test_cidade_igual_ao_circuito_nao_e_repetida_como_localidade(self):
        evento = gp(1, "XYZ", "2027-03-05", "2027-03-07")
        evento["circuit"]["name"] = "Circuito XYZ"
        evento["circuit"]["city"] = "Circuito XYZ"

        etapa = moto.montar([evento], "MGP", "MotoGP", 2027)["etapas"][0]

        self.assertEqual(etapa["circuito"], "Circuito XYZ")
        self.assertEqual(etapa["cidade"], "")

    def test_etapa_desconhecida_usa_dados_da_api(self):
        evento = gp(1, "XYZ", "2027-03-05", "2027-03-07")
        evento["name"] = ""
        evento["circuit"] = {"city": "Cidade Nova", "country": "Novolândia"}

        etapa = moto.montar([evento], "MGP", "MotoGP", 2027)["etapas"][0]

        self.assertEqual(etapa["nomeEvento"], "Cidade Nova")
        self.assertEqual(etapa["circuito"], "Cidade Nova")
        self.assertEqual(etapa["cidade"], "")
        self.assertEqual(etapa["pais"], "Novolândia")

    def test_recusa_calendario_vazio(self):
        vazio = {"temporada": 2026, "categoria": "MotoGP", "etapas": []}
        with self.assertRaises(ValueError):
            moto.validar(vazio, None)

    def test_recusa_resposta_com_muito_menos_sessoes_que_o_arquivo_atual(self):
        atual = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)
        sem_sessoes = moto.montar(
            [gp(1, "THA", "2026-02-27", "2026-03-01")], "MGP", "MotoGP", 2026
        )

        with self.assertRaises(ValueError):
            moto.validar(sem_sessoes, atual)

    def test_aceita_atualizacao_normal(self):
        atual = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)
        moto.validar(atual, atual)

    def test_temporada_nova_nao_e_comparada_com_a_antiga(self):
        antiga = moto.montar(self.eventos_tailandia(), "MGP", "MotoGP", 2026)
        nova = moto.montar([gp(1, "THA", "2027-02-27", "2027-03-01")], "MGP", "MotoGP", 2027)

        moto.validar(nova, antiga)

    def test_escolhe_temporada_atual_enquanto_ha_corridas_por_fazer(self):
        dados = {2026: [gp(1, "THA", "2026-02-27", "2026-03-01", status="NOT-STARTED")]}
        ano, _ = moto.escolher_temporada(2026, obter=lambda a: dados[a])

        self.assertEqual(ano, 2026)

    def test_passa_para_proxima_temporada_quando_a_atual_acabou(self):
        dados = {
            2026: [gp(1, "VAL", "2026-11-27", "2026-11-29", status="FINISHED")],
            2027: [gp(1, "THA", "2027-02-26", "2027-02-28", status="NOT-STARTED")],
        }
        ano, eventos = moto.escolher_temporada(2026, obter=lambda a: dados[a])

        self.assertEqual(ano, 2027)
        self.assertEqual(eventos[0]["shortname"], "THA")

    def test_fica_na_atual_se_a_proxima_ainda_nao_foi_publicada(self):
        dados = {2026: [gp(1, "VAL", "2026-11-27", "2026-11-29", status="FINISHED")], 2027: []}
        ano, _ = moto.escolher_temporada(2026, obter=lambda a: dados[a])

        self.assertEqual(ano, 2026)

    def test_erro_se_a_api_nao_trouxer_grandes_premios(self):
        with self.assertRaises(ValueError):
            moto.escolher_temporada(2026, obter=lambda a: [{"kind": "TEST"}])


class CalendariosMotoEmbutidosTest(unittest.TestCase):
    ARQUIVOS = {
        "motogp_calendar.json": "MotoGP",
        "moto2_calendar.json": "Moto2",
        "moto3_calendar.json": "Moto3",
    }
    # O arquivo é atualizado todo dia com os dados da API; uma sessão nova com outro nome não
    # pode quebrar o build, então o teste só exige um nome limpo.

    def carregar(self, nome):
        caminho = ASSETS / nome
        self.assertTrue(caminho.is_file(), f"{nome} não encontrado em {ASSETS}")
        return json.loads(caminho.read_text(encoding="utf-8"))

    def test_estrutura_e_categoria(self):
        for arquivo, categoria in self.ARQUIVOS.items():
            with self.subTest(arquivo=arquivo):
                dados = self.carregar(arquivo)
                self.assertEqual(dados["categoria"], categoria)
                self.assertTrue(str(dados["temporada"]).strip())
                self.assertTrue(dados["etapas"])

    def test_numeracao_sequencial_e_datas_validas(self):
        for arquivo in self.ARQUIVOS:
            with self.subTest(arquivo=arquivo):
                etapas = self.carregar(arquivo)["etapas"]
                self.assertEqual([e["etapa"] for e in etapas], list(range(1, len(etapas) + 1)))
                anterior = None
                for e in etapas:
                    inicio = datetime.strptime(e["inicio"], "%d/%m/%Y")
                    fim = datetime.strptime(e["fim"], "%d/%m/%Y")
                    self.assertLessEqual(inicio, fim)
                    if anterior:
                        self.assertGreaterEqual(inicio, anterior)
                    anterior = inicio
                    self.assertTrue(e["circuito"].strip())
                    self.assertTrue(e["pais"].strip())

    def test_sessoes_validas_e_perto_do_periodo(self):
        for arquivo in self.ARQUIVOS:
            for e in self.carregar(arquivo)["etapas"]:
                inicio = datetime.strptime(e["inicio"], "%d/%m/%Y")
                fim = datetime.strptime(e["fim"], "%d/%m/%Y")
                for s in e["sessoes"]:
                    with self.subTest(arquivo=arquivo, etapa=e["etapa"], sessao=s["nome"]):
                        self.assertTrue(s["nome"].strip())
                        self.assertEqual(s["nome"], s["nome"].strip())
                        self.assertRegex(s["horario"], re.compile(r"^\d{2}:\d{2}$"))
                        dia = datetime.strptime(s["data"], "%d/%m/%Y")
                        self.assertTrue(inicio - timedelta(days=2) <= dia <= fim + timedelta(days=2))


if __name__ == "__main__":
    unittest.main()
