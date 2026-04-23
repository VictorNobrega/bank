# Bank API

API REST para transferência de valores entre contas bancárias, com consistência transacional, prevenção de deadlocks e notificações assíncronas.

---

## Pré-requisitos

- Java 25
- Maven 3.9+
- Docker e Docker Compose (inclui PostgreSQL e RabbitMQ)

---

## Como executar

### Opção 1 — Docker Compose (recomendado)

Sobe o banco de dados PostgreSQL o broker e a aplicação juntos:

```bash
docker compose up --build
```

A API ficará disponível em `http://localhost:8080/api/v1`.

### Opção 2 — Apenas o banco e o broker via Docker, aplicação local

```bash
# Sobe PostgreSQL e RabbitMQ
docker compose up postgres rabbitmq -d

# Executa a aplicação
./mvnw spring-boot:run
```

### Variáveis de ambiente

| Variável            | Padrão      | Descrição                        |
|---------------------|-------------|----------------------------------|
| `DB_HOST`           | `localhost` | Host do banco de dados           |
| `DB_PORT`           | `5432`      | Porta do PostgreSQL              |
| `DB_NAME`           | `bankdb`    | Nome do banco                    |
| `DB_USER`           | `bank`      | Usuário do banco                 |
| `DB_PASSWORD`       | `bank`      | Senha do banco                   |
| `RABBITMQ_HOST`     | `localhost` | Host do RabbitMQ                 |
| `RABBITMQ_PORT`     | `5672`      | Porta AMQP do RabbitMQ           |
| `RABBITMQ_USER`     | `guest`     | Usuário do RabbitMQ              |
| `RABBITMQ_PASSWORD` | `guest`     | Senha do RabbitMQ                |

### RabbitMQ Management

Com o Docker Compose rodando, o painel de administração do RabbitMQ fica disponível em:

**`http://localhost:15672`** (usuário: `guest` / senha: `guest`)

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
| GET    | `/api/v1/transaction`           | Listar transferências (paginado)       |
| GET    | `/api/v1/transaction/{id}`      | Buscar transferência por ID            |

---

## Decisões de design e arquitetura

### Prevenção de deadlock em transferências concorrentes

Ao transferir entre duas contas, ambas precisam ser bloqueadas com `SELECT FOR UPDATE` (lock pessimista). Se dois threads tentarem transferir entre A↔B simultaneamente na ordem inversa, ocorre deadlock.

A solução adotada é adquirir os locks sempre na **mesma ordem determinística, baseada no UUID das contas**. Assim, independentemente de quem é origem ou destino, toda transferência segue a mesma sequência de aquisição de locks, evitando ordens opostas e eliminando a condição clássica de deadlock.

### Persistência de transações com falha

Quando uma transferência falha **após as duas contas terem sido resolvidas** (por exemplo, saldo insuficiente), um registro com status `FAILED` é persistido para rastreabilidade. Se a falha ocorrer antes disso, como em cenário de conta inexistente, a aplicação registra o erro em log, mas não persiste a transação falha porque não há ambas as referências necessárias. Como a transação principal já foi revertida pelo Spring, essa persistência usa `TransactionTemplate` com `PROPAGATION_REQUIRES_NEW` — abre uma nova transação independente que é commitada mesmo com o rollback da principal.

### Notificações via RabbitMQ

Após a confirmação de uma transferência, o `NotificationService` (producer) publica um evento na exchange `bank.notifications` com routing key `notification`. Um `NotificationConsumer` dedicado, escutando a fila `bank.notification.queue`, consome esse evento e realiza a chamada HTTP para o serviço externo de notificação.

Essa separação desacopla o fluxo de transferência da notificação: falhas HTTP no consumer não afetam a transação já commitada. Quando a chamada externa falha, o consumer registra o erro e propaga a exceção para que a mensagem seja roteada para a DLQ configurada no RabbitMQ.

### Separação de responsabilidades no serviço de transferência

O método `transfer` foi estruturado como um **orquestrador** de etapas privadas com responsabilidades únicas:

- `validateDistinctAccounts` — guarda contra transferência para a mesma conta
- `findAccountsWithOrderedLocks` — aquisição de locks em ordem determinística, preservando origem e destino no retorno
- `applyFundsTransfer` — validação de saldo e mutação das entidades
- `persistCompletedTransaction` — persistência do registro e log
- `notifyTransferParticipants` — disparo da notificação

### Tratamento de erros padronizado (RFC 9457)

Todos os erros são retornados no formato `ProblemDetail` (RFC 9457), com `status`, `detail` e, em erros de validação, um campo `errors` com os campos inválidos e suas mensagens.

### Respostas paginadas

Listagens de contas, extratos e transferências retornam `PaginatedResponse<T>` com metadados de paginação (`totalElements`, `totalPages`, `numberOfElements`, `pageNumber`, `size`). O tamanho máximo de página é 100 para evitar respostas excessivamente grandes.
