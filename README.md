# SIstema PediuPatrão  — Controle de acesso, pedidos e auditoria

Sistema de pedidos da pizzaria (Java 21, Spring Boot 3.4, Spring Security, MongoDB, Thymeleaf), evoluído para a Atividade Prática M1 – Parte 2.

## Como executar

Requisitos: JDK 21, Maven (ou `mvnw`) e um MongoDB. Por padrão a aplicação usa `mongodb://127.0.0.1:27017/pizzaria`; para outro banco (ex.: Atlas) defina a variável de ambiente `MONGODB_URI`.

```bash
./mvnw spring-boot:run     # sobe em http://localhost:8080
./mvnw test                # testes das regras de pedidos
```

### MongoDB local em replica set

Cada operação e o seu registro de auditoria são gravados na mesma transação, e o MongoDB só aceita transações em replica set. O Atlas já funciona assim; no MongoDB local é preciso ativar um replica set de um nó:

1. No `mongod.cfg`, troque `#replication:` por:
   ```yaml
   replication:
     replSetName: rs0
   ```
2. Reinicie o serviço `MongoDB`.
3. No `mongosh`, rode uma única vez: `rs.initiate({_id: "rs0", members: [{_id: 0, host: "127.0.0.1:27017"}]})`

Os testes de `TransacaoAuditoriaTest` simulam falha na gravação da auditoria e conferem que a operação foi desfeita; sem replica set eles são ignorados.

Na primeira execução são criados (se não existirem) três usuários de demonstração. 

| Usuário | Senha | Perfil |

| admin | teste123 | ADMIN |
| gerente | gerente123 | GERENTE |
| atendente | atendente123 | ATENDENTE |

## Requisitos implementados

### RF01 — Perfis e autorização
- Perfis `ADMIN`, `GERENTE`, `ATENDENTE` (`entity/Perfil`), gravados em `Usuario.role` e carregados como `ROLE_*` em `UsuarioDetailsService` (antes todos eram `USER`).
- Autorização no backend em `SecurityConfig` (regras por URL/método HTTP, `anyRequest` exige um dos três perfis) e `@PreAuthorize("hasRole('GERENTE')")` nos serviços de desconto e cancelamento (defesa em profundidade). Esconder botões (`sec:authorize`) é só conveniência visual.
- Acesso negado devolve 403 e fica no log (`Acesso negado: usuario=... uri=...`).
- CSRF reativado: formulários Thymeleaf recebem o token automaticamente e o `layout.html` injeta o cabeçalho em todas as chamadas `fetch` não-GET.
- Usuários: `/usuarios/**` e `/api/usuarios/**` só para ADMIN (a rota `/api/usuarios` antes era pública). O perfil é validado contra o enum, ninguém altera o **próprio** perfil nem exclui a si mesmo (impede autoelevação), username é único e a senha nunca é devolvida pela API (`@JsonProperty(WRITE_ONLY)`).

| Operação | ADMIN | GERENTE | ATENDENTE |

| Gerenciar usuários/perfis | ✔ | | |
| Consultar pedidos | ✔ | ✔ | ✔ |
| Registrar pedido | | ✔ | ✔ |
| Avançar status do pedido | | ✔ | |
| Desconto / cancelamento | | ✔ | |
| Consultar clientes | | ✔ | ✔ |
| Cadastrar/atualizar clientes | | | ✔ |
| Excluir cliente | | ✔ | |
| Consultar auditoria | ✔ | ✔ | |

Decisões onde o enunciado era omisso: avançar o status foi atribuído ao GERENTE ("gerenciar pedidos"); a exclusão de clientes (já existente) ficou com o GERENTE e é auditada; produtos: leitura para todos, escrita para ADMIN e GERENTE.

### RF02 — Pedido
`Pedido` guarda itens (`ItemPedido`: produto, quantidade, preço unitário, subtotal), subtotal, desconto %, valor total, responsável e data/hora de entrada. A API recebe apenas `clienteId` e `{produtoId, quantidade}` (`PedidoRequest`); preços vêm do cadastro de produtos e o total é calculado em `PedidoService` (BigDecimal, 2 casas). Valida cliente, produto ativo e quantidade > 0.

### RF03 — Fluxo de status
`RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA | RETIRADO → FINALIZADO`, definido em `StatusPedido.proximos()`. Transições inválidas retornam 409; a saída grava data/hora e responsável; `CANCELADO` não pode ser usado em `PUT /status` e nenhum pedido cancelado muda de estado.

### RF04 — Cancelamento e desconto (somente GERENTE)
- `POST /api/pedidos/{id}/cancelamento?justificativa=...`: justificativa obrigatória, só antes da saída; preserva os dados e grava quem/quando/por quê.
- `POST /api/pedidos/{id}/desconto?percentual=...`: >0 e ≤20%, só antes da saída; recalcula o total no servidor (o novo desconto substitui o anterior).

### RF05 — Auditoria
- Coleção `auditoria` (`AuditoriaEvento`): usuário, perfil, data/hora, operação, entidade + id, lista de `{campo, valorAnterior, valorNovo}` e justificativa.
- Gerada em `AuditoriaService.registrar` a partir do diff entre estado anterior e posterior. Auditados: criação de pedido, mudança de status, desconto, cancelamento, criação/alteração/exclusão de cliente (inclui logradouro, telefone etc.) e criação/alteração/exclusão de usuário (só username e perfil — **senhas nunca entram na auditoria**).
- Consulta em `/auditoria` e `GET /api/auditoria` (ADMIN e GERENTE). `AuditoriaRepository` estende apenas `Repository` (sem update/delete) e não existe endpoint de escrita; outros métodos HTTP em `/api/auditoria` são negados.
- Dados pessoais (telefone/endereço) aparecem na auditoria porque são o objeto do registro, mas só para ADMIN e GERENTE.

## Principais arquivos alterados/criados
`config/SecurityConfig`, `config/DataInitializer`, `service/{PedidoService,ClienteService,UsuarioService,UsuarioDetailsService,AuditoriaService}`, `entity/{Pedido,ItemPedido,StatusPedido,Perfil,AuditoriaEvento,Alteracao}`, `controller/{PedidoController,AuditoriaController,ApiExceptionHandler,UsuarioController,HomeController}`, templates `layout`, `pedidos`, `auditoria`, `usuarios/*`, `fragments/sidebar`, e o teste `PedidoServiceTest`.


