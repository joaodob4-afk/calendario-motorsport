import json
import os
import urllib.request
from pathlib import Path

URL_CALENDARIO = "https://www.fiaformula2.com/en/racing/2026"

ARQUIVO_JSON = Path("app/src/main/assets/f2_calendar.json")


def consultar_site():
    requisicao = urllib.request.Request(
        URL_CALENDARIO,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept-Language": "en-US,en;q=0.9",
        },
    )

    with urllib.request.urlopen(requisicao, timeout=30) as resposta:
        html = resposta.read().decode("utf-8", errors="replace")

    return html


def main():
    print("Consultando calendário oficial da F2...")

    html = consultar_site()

    if "2026" not in html or len(html) < 1000:
        raise RuntimeError("Não foi possível validar a página oficial.")

    print("Página oficial acessada.")
    print("Nenhuma alteração será feita sem dados estruturados confirmados.")
    print("O calendário existente foi preservado.")


if __name__ == "__main__":
    main()