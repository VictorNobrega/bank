# Bank API

API REST para transferência de valores entre contas bancárias, com consistência transacional, prevenção de deadlocks e notificações assíncronas.

---

## Pré-requisitos

- Java 25
- Maven 3.9+
- Docker e Docker Compose

---

## Como executar

### Opção 1 — Docker Compose (recomendado)

Sobe o banco de dados PostgreSQL e a aplicação juntos:

```bash
docker compose up --build
```

A API ficará disponível em `http://localhost:8080/api/v1`.

### Opção 2 — Apenas o banco via Docker, aplicação local

```bash
# Sobe apenas o PostgreSQL
docker compose up postgres -d

# Executa a aplicação
./mvnw spring-boot:run
```

### Variáveis de ambiente

| Variável       | Padrão      | Descrição                        |
|----------------|-------------|----------------------------------|
| `DB_HOST`      | `localhost` | Host do banco de dados           |
| `DB_PORT`      | `5432`      | Porta do PostgreSQL              |
| `DB_NAME`      | `bankdb`    | Nome do banco                    |
| `DB_USER`      | `bank`      | Usuário do banco                 |
| `DB_PASSWORD`  | `bank`      | Senha do banco                   |

### Dados pré-carregados

Ao iniciar, a aplicação cria automaticamente quatro contas:

| Nome          | Saldo inicial |
|---------------|---------------|
| Alice Souza   | R$ 5.000,00   |
| Bruno Lima    | R$ 3.000,00   |
| Carla Mendes  | R$ 10.000,00  |
| Diego Rocha   | R$ 1.500,00   |

---

## Swagger

Com a aplicação rodando, acesse a documentação interativa:

**`http://localhost:8080/api/v1/swagger-ui.html`**

---

## Endpoints principais

| Método | Endpoint                        | Descrição                              |
|--------|---------------------------------|----------------------------------------|
| POST   | `/api/v1/accounts`              | Criar conta                            |
| GET    | `/api/v1/accounts`              | Listar contas (paginado, ordem por nome) |
| GET    | `/api/v1/accounts/{id}`         | Buscar conta por ID                    |
| GET    | `/api/v1/accounts/{id}/transaction` | Extrato da conta (paginado)        |
| POST   | `/api/v1/transaction`           | Realizar transferência                 |
| GET    | `/api/v1/transaction/{id}`      | Buscar transferência por ID            |

---

## Decisões de design e arquitetura

### Prevenção de deadlock em transferências concorrentes

Ao transferir entre duas contas, ambas precisam ser bloqueadas com `SELECT FOR UPDATE` (lock pessimista). Se dois threads tentarem transferir entre A↔B simultaneamente na ordem inversa, ocorre deadlock.

A solução adotada é adquirir os locks sempre na **mesma ordem determinística** — comparando os UUIDs das contas e bloqueando sempre o menor UUID primeiro. Isso garante que, independentemente da direção da transferência, os locks são adquiridos na mesma sequência global, eliminando o deadlock.

### Persistência de transações com falha

Quando uma transferência falha (saldo insuficiente, conta inexistente, etc.), um registro com status `FAILED` é persistido para rastreabilidade. Como a transação principal já foi revertida pelo Spring, essa persistência usa `TransactionTemplate` com `PROPAGATION_REQUIRES_NEW` — abre uma nova transação independente que é commitada mesmo com o rollback da principal.

### Notificações assíncronas e tolerantes a falha

O envio de notificação ocorre **após** a transferência ser confirmada, em thread separada (`@Async`). Um pool dedicado de threads (`notification-*`) isola esse processamento do fluxo principal. Falhas na notificação são capturadas e logadas sem propagar a exceção — a transferência já está commitada e não pode ser desfeita por uma falha de notificação.

### Separação de responsabilidades no serviço de transferência

O método `transfer` foi estruturado como um **orquestrador** de etapas privadas com responsabilidades únicas:

- `validateDistinctAccounts` — guarda contra transferência para a mesma conta
- `resolveAccountsWithDeadlockSafeLocking` — aquisição de locks em ordem determinística
- `applyFundsTransfer` — validação de saldo e mutação das entidades
- `persistCompletedTransaction` — persistência do registro e log
- `notifyTransferParticipants` — disparo da notificação

### Tratamento de erros padronizado (RFC 9457)

Todos os erros são retornados no formato `ProblemDetail` (RFC 9457), com `status`, `detail` e, em erros de validação, um campo `errors` com os campos inválidos e suas mensagens.

### Respostas paginadas

Listagens de contas e extratos retornam `PaginatedResponse<T>` com metadados de paginação (`totalElements`, `totalPages`, `pageNumber`, `size`). O tamanho máximo de página é 100 para evitar respostas excessivamente grandes.
