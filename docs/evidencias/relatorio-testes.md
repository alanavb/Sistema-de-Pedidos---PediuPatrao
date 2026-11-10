# Relatório de execução dos testes

Gerado em 10/11/2026 10:54 a partir dos relatórios do Maven Surefire (`./mvnw test`).

Ambiente: Java 21, Spring Boot 3.4.4, MongoDB 9.0 local em replica set (`rs0`).

**Resultado: 117 testes, 117 passaram, 0 falharam, 0 ignorados.**

| Classe | Escopo | O que verifica | Testes | Resultado |
|---|---|---|---:|---|
| `DashboardServiceTest` | Parte 2 | Cálculo dos indicadores, séries, ranking, situação e filtros (cenário com valores conferidos à mão) | 20 | ✅ todos passaram |
| `DashboardControllerTest` | Parte 2 | API do dashboard: permissões, parâmetros e erros 400 | 15 | ✅ todos passaram |
| `FiltroDashboardTest` | Parte 2 | Regras do filtro: período padrão, limites, período anterior, hora | 8 | ✅ todos passaram |
| `DashboardIntegracaoTest` | Parte 2 | Consultas no MongoDB real, incluindo pedidos nos limites do período | 4 | ✅ todos passaram |
| `GeradorPedidosDemonstracaoTest` | Parte 2 | Carga de demonstração coerente com as regras de pedido | 10 | ✅ todos passaram |
| `AutorizacaoApiTest` | Partes 1 e 2 | Autorização por perfil nas rotas e APIs, CSRF e tela do dashboard | 38 | ✅ todos passaram |
| `PedidoServiceTest` | Parte 1 | Regras de pedido: total no servidor, transições, desconto e cancelamento | 5 | ✅ todos passaram |
| `PedidoServiceAutorizacaoTest` | Parte 1 | @PreAuthorize no serviço de pedidos | 4 | ✅ todos passaram |
| `UsuarioServiceTest` | Parte 1 | Proteção do último administrador e autoelevação | 6 | ✅ todos passaram |
| `AuditoriaServiceTest` | Parte 1 | Mascaramento de dados pessoais na auditoria | 4 | ✅ todos passaram |
| `TransacaoAuditoriaTest` | Parte 1 | Rollback da operação quando a auditoria falha (MongoDB em replica set) | 3 | ✅ todos passaram |

## DashboardServiceTest

Cálculo dos indicadores, séries, ranking, situação e filtros (cenário com valores conferidos à mão). Tempo: 0.48 s.

| Caso de teste | Resultado |
|---|---|
| Consulta ao banco filtra periodo e status | ✅ passou |
| Filtro por hora considera so pedidos que entraram naquela hora | ✅ passou |
| Filtro por produto considera so os itens do produto | ✅ passou |
| Filtro por tipo considera so os itens do tipo | ✅ passou |
| Filtros combinados de tipo e hora | ✅ passou |
| Limite do ranking e validado | ✅ passou |
| Opcoes de filtro listam tipos e periodo disponivel | ✅ passou |
| Periodo sem pedidos retorna zeros | ✅ passou |
| Ranking por faturamento respeita o limite | ✅ passou |
| Ranking por quantidade desempata pelo faturamento | ✅ passou |
| Resumo compara com o periodo anterior | ✅ passou |
| Resumo ignora cancelados e pedidos incompletos | ✅ passou |
| Sem vendas no periodo anterior a variacao fica nula | ✅ passou |
| Situacao conta todos os status e tempo medio de saida | ✅ passou |
| Tipo inexistente e recusado | ✅ passou |
| Vendas por dia preenchem dias sem venda | ✅ passou |
| Vendas por hora tem24 faixas | ✅ passou |
| Vendas por mes | ✅ passou |
| Vendas por semana comecam na segunda | ✅ passou |
| Vendas por tipo com desconto aplicado nos itens | ✅ passou |

## DashboardControllerTest

API do dashboard: permissões, parâmetros e erros 400. Tempo: 0.82 s.

| Caso de teste | Resultado |
|---|---|
| Admin e gerente consultam o dashboard (caso 1) | ✅ passou |
| Admin e gerente consultam o dashboard (caso 2) | ✅ passou |
| Agrupamento e criterio tem valor padrao | ✅ passou |
| Agrupamento invalido retorna400 | ✅ passou |
| Atendente nao consulta o dashboard | ✅ passou |
| Dashboard esomente leitura | ✅ passou |
| Data mal formatada retorna400 | ✅ passou |
| Filtros sao repassados ao servico | ✅ passou |
| Hora e produto sao repassados ao servico | ✅ passou |
| Hora fora do dia retorna400 | ✅ passou |
| Limite nao numerico retorna400 | ✅ passou |
| Periodo invertido retorna400 | ✅ passou |
| Regra do servico vira erro400 | ✅ passou |
| Sem login redireciona para o login | ✅ passou |
| Status inexistente retorna400 | ✅ passou |

## FiltroDashboardTest

Regras do filtro: período padrão, limites, período anterior, hora. Tempo: 0.01 s.

| Caso de teste | Resultado |
|---|---|
| Data inicial depois da final e invalida | ✅ passou |
| Hora precisa estar entre0e23 | ✅ passou |
| Intervalo inclui o dia final inteiro | ✅ passou |
| Periodo anterior mantem hora e produto | ✅ passou |
| Periodo anterior tem o mesmo tamanho | ✅ passou |
| Periodo maximo de366 dias | ✅ passou |
| Sem datas usa os ultimos30 dias | ✅ passou |
| So data inicial vai ate hoje | ✅ passou |

## DashboardIntegracaoTest

Consultas no MongoDB real, incluindo pedidos nos limites do período. Tempo: 2.14 s.

| Caso de teste | Resultado |
|---|---|
| Considera so os pedidos do periodo incluindo as bordas | ✅ passou |
| Filtro de status e aplicado no banco | ✅ passou |
| Opcoes trazem o periodo com pedidos | ✅ passou |
| Ranking usa os tipos cadastrados | ✅ passou |

## GeradorPedidosDemonstracaoTest

Carga de demonstração coerente com as regras de pedido. Tempo: 0.23 s.

| Caso de teste | Resultado |
|---|---|
| Fim de semana tem mais movimento que segunda | ✅ passou |
| Gera volume compativel com uma pizzaria | ✅ passou |
| Mesma semente gera a mesma carga | ✅ passou |
| Nenhum pedido no futuro ou fora do periodo | ✅ passou |
| Pedidos antigos estao concluidos ou cancelados | ✅ passou |
| Saida e cancelamento sao coerentes com o status | ✅ passou |
| Sem produtos ou clientes nao gera | ✅ passou |
| Sempre ha pedidos em andamento na ultima hora | ✅ passou |
| Tem cancelados e descontos suficientes para os indicadores | ✅ passou |
| Valores seguem as regras do pedido service | ✅ passou |

## AutorizacaoApiTest

Autorização por perfil nas rotas e APIs, CSRF e tela do dashboard. Tempo: 6.86 s.

| Caso de teste | Resultado |
|---|---|
| Admin e gerente abrem o dashboard (caso 1) | ✅ passou |
| Admin e gerente abrem o dashboard (caso 2) | ✅ passou |
| Admin e gerente consultam auditoria (caso 1) | ✅ passou |
| Admin e gerente consultam auditoria (caso 2) | ✅ passou |
| Admin gerencia usuarios | ✅ passou |
| Admin nao consulta clientes | ✅ passou |
| Admin nao registra pedido | ✅ passou |
| Atendente altera cliente | ✅ passou |
| Atendente nao abre o dashboard nem ve o item no menu | ✅ passou |
| Atendente nao ve auditoria | ✅ passou |
| Atendente registra pedido | ✅ passou |
| Auditoria nao pode ser alterada nem excluida (caso 1) | ✅ passou |
| Auditoria nao pode ser alterada nem excluida (caso 2) | ✅ passou |
| Auditoria nao pode ser alterada nem excluida (caso 3) | ✅ passou |
| Cancelamento negado para quem nao e gerente (caso 1) | ✅ passou |
| Cancelamento negado para quem nao e gerente (caso 2) | ✅ passou |
| Desconto negado para quem nao e gerente (caso 1) | ✅ passou |
| Desconto negado para quem nao e gerente (caso 2) | ✅ passou |
| Gerente aplica desconto | ✅ passou |
| Gerente cancela pedido | ✅ passou |
| Gerente nao altera cliente | ✅ passou |
| Mudanca de status so pelo gerente (caso 1) | ✅ passou |
| Mudanca de status so pelo gerente (caso 2) | ✅ passou |
| Nao admin nao altera perfil (caso 1) | ✅ passou |
| Nao admin nao altera perfil (caso 2) | ✅ passou |
| Nao admin nao cria usuario admin (caso 1) | ✅ passou |
| Nao admin nao cria usuario admin (caso 2) | ✅ passou |
| Perfil desconhecido nao acessa nada | ✅ passou |
| Requisicao sem token csrf e recusada | ✅ passou |
| Sem login redireciona para o login | ✅ passou |
| Somente admin lista usuarios (caso 1) | ✅ passou |
| Somente admin lista usuarios (caso 2) | ✅ passou |
| Tela de usuarios so para admin (caso 1) | ✅ passou |
| Tela de usuarios so para admin (caso 2) | ✅ passou |
| Todos os perfis consultam pedidos (caso 1) | ✅ passou |
| Todos os perfis consultam pedidos (caso 2) | ✅ passou |
| Todos os perfis consultam pedidos (caso 3) | ✅ passou |
| Token csrf invalido e recusado | ✅ passou |

## PedidoServiceTest

Regras de pedido: total no servidor, transições, desconto e cancelamento. Tempo: 0.04 s.

| Caso de teste | Resultado |
|---|---|
| Cancelamento exige justificativa e trava o pedido | ✅ passou |
| Desconto limitado a20 por cento e antes da saida | ✅ passou |
| Fluxo valido registra saida | ✅ passou |
| Total vem dos precos do servidor | ✅ passou |
| Transicao invalida e bloqueada | ✅ passou |

## PedidoServiceAutorizacaoTest

@PreAuthorize no serviço de pedidos. Tempo: 0.21 s.

| Caso de teste | Resultado |
|---|---|
| Admin nao aplica desconto | ✅ passou |
| Admin nao cancela | ✅ passou |
| Atendente nao cancela | ✅ passou |
| Gerente passa pela autorizacao | ✅ passou |

## UsuarioServiceTest

Proteção do último administrador e autoelevação. Tempo: 0.12 s.

| Caso de teste | Resultado |
|---|---|
| Exclui admin quando ha outro | ✅ passou |
| Nao altera o proprio perfil | ✅ passou |
| Nao exclui o proprio usuario | ✅ passou |
| Nao exclui ultimo admin | ✅ passou |
| Nao rebaixa ultimo admin | ✅ passou |
| Rebaixa admin quando ha outro | ✅ passou |

## AuditoriaServiceTest

Mascaramento de dados pessoais na auditoria. Tempo: 0.17 s.

| Caso de teste | Resultado |
|---|---|
| Grava dados pessoais mascarados | ✅ passou |
| Mascaras | ✅ passou |
| Sem mudanca nao grava nada | ✅ passou |
| Troca que fica igual mascarada ainda e registrada | ✅ passou |

## TransacaoAuditoriaTest

Rollback da operação quando a auditoria falha (MongoDB em replica set). Tempo: 0.88 s.

| Caso de teste | Resultado |
|---|---|
| Alteracao de cliente e desfeita | ✅ passou |
| Cadastro de cliente e desfeito | ✅ passou |
| Mudanca de status do pedido e desfeita | ✅ passou |
