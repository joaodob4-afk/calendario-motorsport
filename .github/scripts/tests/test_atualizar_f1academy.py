"""Testes offline do atualizador da F1 Academy (páginas simuladas)."""
import importlib.util
import json
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
SCRIPT = RAIZ / ".github" / "scripts" / "atualizar_f1academy.py"
SPEC = importlib.util.spec_from_file_location("atualizar_f1academy", SCRIPT)
f1a = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(f1a)


def pagina(titulo, sessoes, pais=None):
    page_data = {"SessionResults": sessoes}
    if pais:
        page_data["CountryName"] = pais
    dados = {"props": {"pageProps": {"pageData": page_data}}}
    return (
        f"<html><head><title>{titulo} - F1 ACADEMY™ Racing Series</title></head><body>"
        f'<script id="__NEXT_DATA__" type="application/json">{json.dumps(dados)}</script>'
        "</body></html>"
    )


def sessao(nome, inicio, nao_confirmada=False):
    return {"SessionName": nome, "SessionStartTime": inicio, "Unconfirmed": nao_confirmada}


XANGAI = pagina("Round 1: Shanghai 13-15 March 2026", [
    sessao("Feature Race", "2026-03-15T10:40:00+08:00"),
    sessao("Free Practice", "2026-03-13T09:10:00+08:00"),
    sessao("Reverse Grid Race", "2026-03-14T13:45:00+08:00"),
    sessao("Qualifying", "2026-03-13T14:10:00+08:00"),
])
VEGAS = pagina("Round 6: Las Vegas 19-21 November 2026", [
    sessao("Free Practice", "2026-11-19T01:00:00-07:00", True),
    sessao("Qualifying", "2026-11-19T02:00:00-07:00", True),
    sessao("Reverse Grid Race", "2026-11-20T01:00:00-07:00", True),
    sessao("Feature Race", "2026-11-21T01:00:00-07:00", True),
])


class AtualizadorF1AcademyTest(unittest.TestCase):
    def test_ids_das_etapas(self):
        html = '<a href="/Racing-Series/Results?raceid=24">x</a><a href="?raceid=22">y</a><a href="?raceid=22">z</a>'
        self.assertEqual(f1a.ids_das_etapas(html), [22, 24])

    def test_etapa_confirmada_vai_para_brasilia_em_ordem(self):
        etapa = f1a.montar_etapa(XANGAI)
        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Xangai", "China"))
        self.assertEqual((etapa["inicio"], etapa["fim"]), ("13/03/2026", "15/03/2026"))
        self.assertEqual(
            [(s["nome"], s["data"], s["horario"]) for s in etapa["sessoes"]],
            [
                ("Treino Livre", "12/03/2026", "22:10"),
                ("Classificação", "13/03/2026", "03:10"),
                ("Corrida Grid Invertido", "14/03/2026", "02:45"),
                ("Corrida Feature", "14/03/2026", "23:40"),
            ],
        )

    def test_sessao_nao_confirmada_fica_a_confirmar_com_o_dia_local(self):
        etapa = f1a.montar_etapa(VEGAS)
        self.assertEqual(
            [(s["data"], s["horario"]) for s in etapa["sessoes"]],
            [("19/11/2026", "A confirmar"), ("19/11/2026", "A confirmar"),
             ("20/11/2026", "A confirmar"), ("21/11/2026", "A confirmar")],
        )
        self.assertEqual((etapa["inicio"], etapa["fim"]), ("19/11/2026", "21/11/2026"))

    def test_calendario_ordenado_e_numerado(self):
        cal = f1a.montar_calendario([f1a.montar_etapa(VEGAS), f1a.montar_etapa(XANGAI)])
        self.assertEqual(cal["temporada"], 2026)
        self.assertEqual(cal["categoria"], "F1 Academy")
        self.assertEqual([(e["etapa"], e["circuito"]) for e in cal["etapas"]],
                         [(1, "Xangai"), (2, "Las Vegas")])
        self.assertEqual(set(cal["etapas"][0]), {"etapa", "circuito", "pais", "inicio", "fim", "sessoes"})

    def test_cidade_fora_da_tabela_usa_o_que_o_site_informa(self):
        sessoes = [sessao("Free Practice", "2026-05-01T10:00:00+00:00")]
        etapa = f1a.montar_etapa(pagina("Round 2: Imola 1-3 May 2026", sessoes, "Italy"))
        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Imola", "Itália"))
        etapa = f1a.montar_etapa(pagina("Round 2: Atlantis 1-3 May 2026", sessoes, "Atlantis Land"))
        self.assertEqual((etapa["circuito"], etapa["pais"]), ("Atlantis", "Atlantis Land"))

    def test_cidade_fora_da_tabela_sem_pais_recusa(self):
        html = pagina("Round 2: Atlantis 1-3 May 2026", [sessao("Free Practice", "2026-05-01T10:00:00+00:00")])
        with self.assertRaises(ValueError):
            f1a.montar_etapa(html)

    def test_pagina_sem_sessoes_ou_sem_dados_recusa(self):
        with self.assertRaises(ValueError):
            f1a.montar_etapa(pagina("Round 1: Shanghai 13-15 March 2026", []))
        with self.assertRaises(ValueError):
            f1a.montar_etapa("<html><head><title>Round 1: Shanghai 13-15 March 2026</title></head></html>")

    def test_temporadas_misturadas_recusam(self):
        antiga = pagina("Round 7: Las Vegas 20-22 November 2025", [sessao("Free Practice", "2025-11-20T18:00:00-08:00")])
        with self.assertRaises(ValueError):
            f1a.montar_calendario([f1a.montar_etapa(antiga), f1a.montar_etapa(XANGAI)])

    def test_etapa_de_tres_corridas_com_classificacao_em_dois_grupos(self):
        html = pagina("Round 5: Austin 22-25 October 2026", [
            sessao("Free Practice", "2026-10-22T12:25:00-05:00"),
            sessao("Qualifying 1", "2026-10-22T16:30:00-05:00"),
            sessao("Qualifying 2", "2026-10-22T16:30:00-05:00"),
            sessao("Opening Race", "2026-10-23T17:50:00-05:00"),
            sessao("Reverse Grid Race", "2026-10-24T18:00:00-05:00"),
            sessao("Feature Race", "2026-10-25T12:00:00-05:00"),
        ])
        etapa = f1a.montar_etapa(html)
        self.assertEqual(
            [s["nome"] for s in etapa["sessoes"]],
            ["Treino Livre", "Classificação", "Corrida de Abertura",
             "Corrida Grid Invertido", "Corrida Feature"],
        )
        self.assertEqual((etapa["inicio"], etapa["fim"]), ("22/10/2026", "25/10/2026"))

    def test_nomes_de_2025(self):
        self.assertEqual(f1a.nome_da_sessao({"SessionName": "Race 1"}), "Corrida Grid Invertido")
        self.assertEqual(f1a.nome_da_sessao({"SessionName": "Race 2"}), "Corrida Feature")


if __name__ == "__main__":
    unittest.main()