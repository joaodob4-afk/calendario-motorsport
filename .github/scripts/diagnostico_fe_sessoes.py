"""Diagnóstico das sessões da Fórmula E em todas as rodadas da temporada atual.

Descobre as rodadas no JSON-LD do calendário oficial, usa o nome original da
cidade para montar a URL de resultados e salva cada página recebida como artefato.
O diagnóstico é somente de leitura: não altera o calendário do aplicativo.
"""
import re
import unicodedata
from pathlib import Path
from urllib.parse import urlencode

from atualizar_fe import URL as URL_CALENDARIO
from atualizar_fe import extrair_rodadas, lista_do_calendario
from utilitarios import baixar_texto

BASE_RESULTADOS = "https://www.fiaformulae.com/en/results-and-standings"
TEMPORADA = 13


def slug_cidade(nome):
    """Converte o nome original da rodada no slug usado pelo site."""
    normalizado = unicodedata.normalize("NFKD", nome)
    ascii_nome = normalizado.encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "-", ascii_nome.lower()).strip("-")


def descobrir_urls(html, temporada=TEMPORADA):
    """Gera uma URL por rodada usando exclusivamente o índice oficial."""
    dados = lista_do_calendario(html)
    rodadas = extrair_rodadas(dados)
    slugs_por_rodada = {}

    for elemento in dados["itemListElement"]:
        item = elemento.get("item") if isinstance(elemento, dict) else None
        nome = item.get("name") if isinstance(item, dict) else None
        match = re.match(r"R(\d+)\s+(.+?)\s+E-Prix", nome or "")
        if not match:
            raise ValueError(f"Nome de rodada inesperado no diagnóstico: {nome!r}")
        numero = int(match.group(1))
        if numero in slugs_por_rodada:
            raise ValueError(f"Rodada duplicada no diagnóstico: R{numero}")
        slugs_por_rodada[numero] = slug_cidade(match.group(2))

    numeros = {r["rodada"] for r in rodadas}
    if numeros != set(slugs_por_rodada):
        raise ValueError("Não foi possível associar todas as rodadas aos seus slugs")

    return [
        (
            rodada["rodada"],
            rodada["cidade"],
            f"{BASE_RESULTADOS}?{urlencode({'season': temporada, 'round': str(rodada['rodada']) + '-' + slugs_por_rodada[rodada['rodada']]})}",
        )
        for rodada in rodadas
    ]


def analisar(url, numero, cidade):
    print("=" * 72)
    print(f"Rodada R{numero}: {cidade}")
    print("Página:", url)
    try:
        html = baixar_texto(url)
    except Exception as erro:  # noqa: BLE001
        print("  ERRO ao baixar:", erro)
        return False

    print("  tamanho:", len(html))
    if not html:
        print("  Página vazia.")
        return False

    arquivo = Path(f"fe_pagina_{numero:02d}.html")
    arquivo.write_text(html, encoding="utf-8")
    print("  HTML salvo:", arquivo)
    print("  ocorrências de sessões:", html.lower().count("session"))
    for termo in ("Free Practice", "Practice", "Qualifying", "E-Prix Unleashed", "startTime"):
        print(f"  {termo}: {html.count(termo)} ocorrência(s)")
    return True


def main():
    print("Consultando o calendário oficial da Fórmula E...")
    try:
        html = baixar_texto(URL_CALENDARIO)
        urls = descobrir_urls(html)
    except Exception as erro:  # noqa: BLE001
        print("ERRO ao descobrir rodadas:", erro)
        print("Nenhuma página foi processada.")
        return 1

    print(f"Rodadas descobertas automaticamente: {len(urls)}")
    sucessos = 0
    for numero, cidade, url in urls:
        if analisar(url, numero, cidade):
            sucessos += 1

    print("=" * 72)
    print(f"Páginas baixadas: {sucessos}/{len(urls)}")
    print("Os arquivos HTML são anexados pelo workflow como artefato.")
    return 0 if sucessos == len(urls) else 1


if __name__ == "__main__":
    raise SystemExit(main())
