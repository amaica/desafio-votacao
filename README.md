---

# 🗳️ Desafio Votação – Spring Boot + React

Sistema de **votação cooperativa**: criar pautas, abrir sessões temporizadas e registrar votos **SIM/NÃO**.
Backend **Spring Boot (Java 17)**, frontend **React + PrimeReact** e banco **MySQL + Flyway**, orquestrados por **Docker Compose**.

👉 Repositório: [https://github.com/amaica/desafio-votacao](https://github.com/amaica/desafio-votacao)

---

## 🚀 Como rodar (Docker Compose – recomendado)

Clone o repositório:

```bash
git clone https://github.com/amaica/desafio-votacao.git
cd desafio-votacao
```

Suba tudo (banco + backend + frontend):

```bash
docker compose up --build
```

Acesse:

* **Frontend:** [http://localhost:5173](http://localhost:5173)
* **API (Swagger):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **MySQL:** localhost:3306 (user **root**, pass **root**)

Parar:

```bash
docker compose down
```

Limpar volumes (apaga o banco):

```bash
docker compose down -v
```

---

## 💻 Execução local (sem Docker)

**Pré-requisitos:** Java 17+, Node 18+, MySQL 8+

**Backend**

```bash
./mvnw spring-boot:run
```

**Frontend**

```bash
cd votacao-frontend
npm install
npm run dev
```

---

## 🧩 Fluxo de uso

1. **Criar pauta** → “Nova Pauta” (título e descrição)
2. **Abrir sessão** → “Abrir Sessão” (padrão **60s**; pode usar `?duracao=90`)
3. **Votar** → “Votar”, informe **CPF (11 dígitos)** e **SIM**/**NÃO**

   * mesmo CPF pode votar em outras pautas, **mas 1 voto por pauta**
4. **Resultado** → totais de **SIM/NÃO** e **status da sessão**

---

## ✅ Regras e validações

* **1 voto por pauta + CPF** (constraint `UNIQUE(pauta_id, cpf)`)
* **CPF com 11 dígitos numéricos**
* Voto só é aceito com **sessão ABERTA** (não expirada)
* **Sessão expira automaticamente** após `duration_seconds`
* **Resultados** expõem totais agregados por opção
* **Tratamento global** de erros (mensagens limpas e status corretos)
* **Swagger UI** documenta todos os endpoints
* **Flyway** versiona o schema e garante consistência

---

## 🔥 API — Endpoints principais

| Método | Endpoint                                | Descrição                    |
| -----: | --------------------------------------- | ---------------------------- |
| `POST` | `/api/v1/pautas`                        | Cria pauta                   |
|  `GET` | `/api/v1/pautas`                        | Lista pautas                 |
| `POST` | `/api/v1/pautas/{id}/sessao?duracao=60` | Abre sessão                  |
| `POST` | `/api/v1/pautas/{id}/votar`             | Registra voto `{cpf, opcao}` |
|  `GET` | `/api/v1/pautas/{id}/resultado`         | Resultado (SIM/NÃO + status) |

**Exemplo (curl)**

```bash
curl -X POST http://localhost:8080/api/v1/pautas/{ID}/votar \
  -H "Content-Type: application/json" \
  -d '{"cpf":"11122233344","opcao":"SIM"}'
```

---

## 🧾 Postman

Pasta **`/postman/`**:

* `desafio-votacao.postman_collection.json`
* `desafio-votacao-docker.postman_environment.json`  ← usa `{{baseUrl}} = http://votacao-backend:8080/api/v1`

**Ordem sugerida**: Criar Pauta → Abrir Sessão → Votar → Resultado

---

## 🧪 Testes

Execute:

```bash
./mvnw test
```

Cenários incluídos e/ou recomendados:

* criação de pauta
* abrir sessão (custom `?duracao=`) e sessão expirada
* **voto duplicado** (mesmo CPF/pauta) → **409/Conflict**
* mesmo CPF em **múltiplas pautas** → **OK**
* **resultado** com contagem correta (SIM/NÃO)
* códigos de status esperados (200/201/204/409/422)

> **H2 em memória** para testes, com migrações Flyway aplicadas.

---

## 🧱 Stack técnica

* **Backend:** Java 17 • Spring Boot 3 • Spring Data JPA • Flyway • springdoc-openapi
* **Frontend:** React 18 • Vite • PrimeReact • PrimeFlex
* **Banco:** MySQL 8 (Docker) • H2 (testes)
* **Qualidade:** JUnit 5 • Mockito
* **Infra:** Docker Compose

---

## 🗺️ Estrutura do projeto

```
desafio-votacao/
├─ votacao-backend/
│  ├─ src/main/java/.../controller/
│  ├─ src/main/java/.../service/
│  ├─ src/main/java/.../repository/
│  ├─ src/main/java/.../domain/
│  ├─ src/main/java/.../dto/
│  ├─ src/main/java/.../exception/
│  └─ src/main/resources/db/migration/      # Flyway (pauta, sessao_votacao, voto)
├─ votacao-frontend/
│  └─ src/
│     ├─ components/
│     └─ App.tsx
├─ postman/
│  ├─ desafio-votacao.postman_collection.json
│  └─ desafio-votacao-docker.postman_environment.json
├─ docker-compose.yml
├─ README.md
└─ Handoff.md
```

---

## ⚙️ Configurações úteis

### Variáveis padrão

```
SPRING_PROFILES_ACTIVE=dev
MYSQL_USER=root
MYSQL_PASSWORD=root
CPF_CHECK_ENABLED=false
```

### Logs (sem Lombok)

`votacao-backend/src/main/resources/application.yml`

```yaml
logging:
  level:
    root: INFO
    # ajuste para seu pacote base (ex.: com.amaica.votacao)
    com: DEBUG
    org.hibernate.SQL: WARN
```

*(Opcional: `logback-spring.xml` com pattern de console.)*

---

## 🧠 Decisões de arquitetura (por quê?)

* **Simplicidade**: REST claro `controller → service → repository → domain` sem over-engineering.
* **Regra de voto único**: garantida **no banco** (`UNIQUE(pauta_id, cpf)`) + validação de serviço.
* **Sessões temporizadas**: controle por `closes_at` checado nos fluxos (sem cron/job), determinístico.
* **Migrações**: **Flyway** para reproduzir schema em qualquer ambiente (Docker/local/CI).
* **DX**: **Swagger UI** para inspeção e testes rápidos da API.
* **Frontend**: **PrimeReact/PrimeFlex** para responsividade rápida e componentes sólidos.

---

## 🧰 Qualidade & Commits

* **Back**: testes com **JUnit 5**; recomendável Spotless/Checkstyle (opcional).
* **Front**: `eslint`/`prettier` (opcional).
* **Commits** (sugestão de padrão):

  * `feat:` nova funcionalidade
  * `fix:` correção de bug
  * `test:` testes
  * `docs:` documentação (README/Handoff)
  * `chore:` manutenção (build, deps)
  * `refactor:` refatoração sem mudar comportamento

Exemplos:

* `feat: abrir sessão com duração customizável (?duracao=)`
* `fix: impedir voto quando sessão expirada`
* `test: cobre cenário de voto duplicado`
* `docs: adiciona Postman e instruções Docker`

---
