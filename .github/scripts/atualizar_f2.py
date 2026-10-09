import re
import urllib.request
from html.parser import HTMLParser

URL = "https://www.fiaformula2.com/en/racing/2026"


class LinksParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() != "a":
            return

        for nome, valor in attrs:
            if nome.lower() == "href" and valor:
                if re.search(
                    r"/en/racing/2026/[^/?#]+",
                    valor
                ):
                    self.links.append(valor)


def main():
    print("Consultando páginas oficiais da F2...")

    requisicao = urllib.request.Request(
        URL,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept-Language": "en-US,en;q=0.9",
        },
    )

    with urllib.request.urlopen(
        requisicao, timeout=30
    ) as resposta:
        html = resposta.read().decode(
            "utf-8", errors="replace"
        )

    parser = LinksParser()
    parser.feed(html)

    links = sorted(set(parser.links))

    print("Links de etapas encontrados:", len(links))

    for link in links:
        if link.startswith("/"):
            link = "https://www.fiaformula2.com" + link
        print(link)

    if not links:
        print("Nenhum link de etapa encontrado.")
        print("O calendário não foi alterado.")

    print("Teste concluído. Nenhum arquivo foi alterado.")


if __name__ == "__main__":
    main()