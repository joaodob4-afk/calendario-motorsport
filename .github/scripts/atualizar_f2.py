import json
import re
import urllib.request
from html.parser import HTMLParser

URL = "https://www.fiaformula2.com/en/racing/2026"


class JSONLDParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.dentro = False
        self.conteudo = []
        self.blocos = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() == "script":
            atributos = dict(attrs)
            if atributos.get("type") == "application/ld+json":
                self.dentro = True
                self.conteudo = []

    def handle_data(self, data):
        if self.dentro:
            self.conteudo.append(data)

    def handle_endtag(self, tag):
        if tag.lower() == "script" and self.dentro:
            texto = "".join(self.conteudo).strip()
            if texto:
                self.blocos.append(texto)
            self.dentro = False


def main():
    print("Consultando o calendário oficial da F2...")

    requisicao = urllib.request.Request(
        URL,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept-Language": "en-US,en;q=0.9",
        },
    )

    with urllib.request.urlopen(requisicao, timeout=30) as resposta:
        html = resposta.read().decode(
            "utf-8", errors="replace"
        )

    if len(html) < 1000:
        raise RuntimeError("A página retornou conteúdo insuficiente.")

    parser = JSONLDParser()
    parser.feed(html)

    print("Página acessada com sucesso.")
    print("Tamanho recebido:", len(html), "caracteres")
    print("Blocos JSON-LD encontrados:", len(parser.blocos))

    for i, bloco in enumerate(parser.blocos, start=1):
        try:
            dados = json.loads(bloco)
            print(f"Bloco estruturado {i}:")
            print(json.dumps(dados, ensure_ascii=False)[:1500])
        except json.JSONDecodeError:
            print(f"Bloco {i}: formato JSON inválido")

    palavras = [
        "Practice",
        "Qualifying",
        "Sprint Race",
        "Feature Race",
        "schedule",
    ]

    for palavra in palavras:
        encontrados = list(
            re.finditer(re.escape(palavra), html, re.IGNORECASE)
        )
        print(f"{palavra}: {len(encontrados)} ocorrência(s)")

    print("Teste concluído. Nenhum arquivo foi alterado.")


if __name__ == "__main__":
    main()