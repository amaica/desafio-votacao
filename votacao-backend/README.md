# Votação – Backend (Maven Project)

Projeto Maven com Spring Boot, Flyway, Testcontainers, WireMock e **Maven Wrapper**.
Rodar:
```bash
./mvnw spring-boot:run
# ou
./mvnw test
```

Para banco local:
```bash
docker compose up -d
./mvnw spring-boot:run
```

Endpoints v1:
- `POST /api/v1/pautas`
- `POST /api/v1/pautas/{pautaId}/sessoes`
- `POST /api/v1/pautas/{pautaId}/votos`
- `GET  /api/v1/pautas/{pautaId}/resultado`
