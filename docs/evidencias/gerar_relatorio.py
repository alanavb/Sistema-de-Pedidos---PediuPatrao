import glob
import os
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime

relatorios, saida = sys.argv[1], sys.argv[2]

DESCRICAO = {
    "DashboardServiceTest": ("Parte 2", "Cálculo dos indicadores, séries, ranking, situação e filtros (cenário com valores conferidos à mão)"),
    "DashboardControllerTest": ("Parte 2", "API do dashboard: permissões, parâmetros e erros 400"),
    "FiltroDashboardTest": ("Parte 2", "Regras do filtro: período padrão, limites, período anterior, hora"),
    "DashboardIntegracaoTest": ("Parte 2", "Consultas no MongoDB real, incluindo pedidos nos limites do período"),
    "GeradorPedidosDemonstracaoTest": ("Parte 2", "Carga de demonstração coerente com as regras de pedido"),
    "AutorizacaoApiTest": ("Partes 1 e 2", "Autorização por perfil nas rotas e APIs, CSRF e tela do dashboard"),
    "PedidoServiceTest": ("Parte 1", "Regras de pedido: total no servidor, transições, desconto e cancelamento"),
    "PedidoServiceAutorizacaoTest": ("Parte 1", "@PreAuthorize no serviço de pedidos"),
    "UsuarioServiceTest": ("Parte 1", "Proteção do último administrador e autoelevação"),
    "AuditoriaServiceTest": ("Parte 1", "Mascaramento de dados pessoais na auditoria"),
    "TransacaoAuditoriaTest": ("Parte 1", "Rollback da operação quando a auditoria falha (MongoDB em replica set)"),
}
ORDEM = list(DESCRICAO)


def legivel(nome):
    base, _, sufixo = nome.partition("(")
    palavras = re.sub(r"(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])", " ", base).lower()
    caso = re.search(r"\[(\d+)\]$", nome)
    return palavras.capitalize() + (f" (caso {caso.group(1)})" if caso else "")


classes = []
for arquivo in glob.glob(os.path.join(relatorios, "TEST-*.xml")):
    raiz = ET.parse(arquivo).getroot()
    nome = raiz.get("name").split(".")[-1]
    casos = []
    for tc in raiz.findall("testcase"):
        if tc.find("failure") is not None or tc.find("error") is not None:
            situacao = "falhou"
        elif tc.find("skipped") is not None:
            situacao = "ignorado"
        else:
            situacao = "passou"
        casos.append((legivel(tc.get("name")), situacao, float(tc.get("time") or 0)))
    classes.append((nome, casos, float(raiz.get("time") or 0)))

classes.sort(key=lambda c: ORDEM.index(c[0]) if c[0] in ORDEM else 99)
total = sum(len(c[1]) for c in classes)
passou = sum(1 for c in classes for x in c[1] if x[1] == "passou")
falhou = sum(1 for c in classes for x in c[1] if x[1] == "falhou")
ignorado = total - passou - falhou

linhas = [
    "# Relatório de execução dos testes",
    "",
    f"Gerado em {datetime.now():%d/%m/%Y %H:%M} a partir dos relatórios do Maven Surefire (`./mvnw test`).",
    "",
    "Ambiente: Java 21, Spring Boot 3.4.4, MongoDB 9.0 local em replica set (`rs0`).",
    "",
    f"**Resultado: {total} testes, {passou} passaram, {falhou} falharam, {ignorado} ignorados.**",
    "",
    "| Classe | Escopo | O que verifica | Testes | Resultado |",
    "|---|---|---|---:|---|",
]
for nome, casos, _ in classes:
    escopo, desc = DESCRICAO.get(nome, ("-", "-"))
    ok = all(c[1] == "passou" for c in casos)
    linhas.append(f"| `{nome}` | {escopo} | {desc} | {len(casos)} | {'✅ todos passaram' if ok else '❌ ver detalhes'} |")

for nome, casos, tempo in classes:
    escopo, desc = DESCRICAO.get(nome, ("-", "-"))
    linhas += ["", f"## {nome}", "", f"{desc}. Tempo: {tempo:.2f} s.", "", "| Caso de teste | Resultado |", "|---|---|"]
    for caso, situacao, _ in sorted(casos):
        icone = {"passou": "✅", "falhou": "❌", "ignorado": "⏭️"}[situacao]
        linhas.append(f"| {caso} | {icone} {situacao} |")

os.makedirs(os.path.dirname(saida), exist_ok=True)
with open(saida, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(linhas) + "\n")
print(total, passou, falhou, ignorado)
