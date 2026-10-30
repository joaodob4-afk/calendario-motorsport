import importlib.util
import json
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest.mock import MagicMock, patch

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("utilitarios", ROOT / "utilitarios.py")
u = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(u)


def resposta(corpo: bytes):
    r = MagicMock()
    r.read.return_value = corpo
    r.__enter__.return_value = r
    return r


class BaixarTest(unittest.TestCase):
    def test_texto_com_uma_tentativa_nao_espera_e_repassa_o_erro(self):
        erro = urllib.error.HTTPError("http://x", 404, "nao existe", {}, None)
        with patch.object(u.urllib.request, "urlopen", side_effect=erro) as abrir, \
                patch.object(u.time, "sleep") as dormir:
            with self.assertRaises(urllib.error.HTTPError):
                u.baixar_texto("http://x")
        self.assertEqual(abrir.call_count, 1)
        dormir.assert_not_called()

    def test_tenta_de_novo_e_espera_entre_tentativas(self):
        with patch.object(
            u.urllib.request, "urlopen", side_effect=[OSError("fora"), resposta("olá".encode())]
        ) as abrir, patch.object(u.time, "sleep") as dormir:
            self.assertEqual(u.baixar_texto("http://x", tentativas=3), "olá")
        self.assertEqual(abrir.call_count, 2)
        dormir.assert_called_once_with(2)

    def test_depois_da_ultima_tentativa_nao_dorme_e_levanta(self):
        with patch.object(u.urllib.request, "urlopen", side_effect=OSError("fora")), \
                patch.object(u.time, "sleep") as dormir:
            with self.assertRaises(OSError):
                u.baixar_texto("http://x", tentativas=3)
        self.assertEqual(dormir.call_count, 2)

    def test_json_invalido_conta_como_falha_e_tenta_de_novo(self):
        with patch.object(
            u.urllib.request, "urlopen", side_effect=[resposta(b"<html>"), resposta(b'{"a": 1}')]
        ), patch.object(u.time, "sleep"):
            self.assertEqual(u.baixar_json("http://x"), {"a": 1})

    def test_cabecalhos_personalizados_sao_usados(self):
        with patch.object(u.urllib.request, "urlopen", return_value=resposta(b"ok")), \
                patch.object(u.urllib.request, "Request") as pedido:
            u.baixar_texto("http://x", cabecalhos={"User-Agent": "teste"})
        self.assertEqual(pedido.call_args.kwargs["headers"], {"User-Agent": "teste"})


class ArquivosTest(unittest.TestCase):
    def test_grava_so_quando_muda(self):
        with tempfile.TemporaryDirectory() as pasta:
            caminho = Path(pasta) / "cal.json"
            dados = {"etapas": [{"sessoes": [1, 2]}], "nome": "Fórmula"}
            self.assertTrue(u.gravar_se_mudou(caminho, dados))
            self.assertFalse(u.gravar_se_mudou(caminho, dados))
            self.assertTrue(u.gravar_se_mudou(caminho, {"etapas": []}))
            self.assertEqual(json.loads(caminho.read_text(encoding="utf-8")), {"etapas": []})

    def test_formato_igual_ao_dos_scripts_antigos(self):
        dados = {"nome": "Fórmula", "x": [1]}
        self.assertEqual(
            u.serializar(dados), json.dumps(dados, ensure_ascii=False, indent=2) + "\n"
        )

    def test_ler_existente_devolve_none_se_ausente_ou_ilegivel(self):
        with tempfile.TemporaryDirectory() as pasta:
            self.assertIsNone(u.ler_existente(Path(pasta) / "nao-existe.json"))
            ruim = Path(pasta) / "ruim.json"
            ruim.write_text("{", encoding="utf-8")
            self.assertIsNone(u.ler_existente(ruim))

    def test_gravacao_atomica_preserva_arquivo_anterior_se_replace_falhar(self):
        with tempfile.TemporaryDirectory() as pasta:
            caminho = Path(pasta) / "cal.json"
            original = u.serializar({"etapas": ["antigas"]})
            caminho.write_text(original, encoding="utf-8")

            with patch.object(u.os, "replace", side_effect=OSError("falha simulada")):
                with self.assertRaisesRegex(OSError, "falha simulada"):
                    u.gravar_se_mudou(caminho, {"etapas": ["novas"]})

            self.assertEqual(caminho.read_text(encoding="utf-8"), original)
            self.assertEqual(list(Path(pasta).iterdir()), [caminho])

    def test_gravacao_atomica_deixa_json_valido(self):
        with tempfile.TemporaryDirectory() as pasta:
            caminho = Path(pasta) / "cal.json"
            self.assertTrue(u.gravar_se_mudou(caminho, {"etapas": [1]}))
            self.assertEqual(json.loads(caminho.read_text(encoding="utf-8")), {"etapas": [1]})
            self.assertEqual(list(Path(pasta).iterdir()), [caminho])

    def test_total_de_sessoes(self):
        dados = {"etapas": [{"sessoes": [1, 2]}, {"sessoes": [3]}, {}]}
        self.assertEqual(u.total_de_sessoes(dados), 3)


class DatasTest(unittest.TestCase):
    def test_parse_utc_e_formatar_data(self):
        self.assertEqual(u.parse_utc("2026-03-06T10:30:00Z").utcoffset().total_seconds(), 0)
        from datetime import date
        self.assertEqual(u.formatar_data(date(2026, 3, 6)), "06/03/2026")


if __name__ == "__main__":
    unittest.main()
