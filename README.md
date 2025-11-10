# Desafio Votação — Spring Boot + React

Sistema simples de votação em pautas: criar pauta, abrir sessão (com tempo), receber votos (SIM/NAO), contabilizar resultado.  
Regras:
- **1 voto por pauta por CPF** (pode votar em outras pautas).
- Sessão deve estar **ABERTA** e não expirada para aceitar votos.
- Persistência em banco relacional.

## Stack
- **Backend**: Java 17, Spring Boot, Spring Data JPA, Flyway, springdoc-openapi.
- **Frontend**: React + Vite + PrimeReact (responsivo).
- **DB**: MySQL (Docker) — pode usar H2 em testes.

---

## Como rodar (rápido)

### Opção A — Docker Compose (API, DB e Front)
```bash
docker compose up --build

