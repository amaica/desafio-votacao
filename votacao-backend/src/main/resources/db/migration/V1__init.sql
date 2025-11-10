CREATE TABLE IF NOT EXISTS pauta (
  id BINARY(16) PRIMARY KEY,
  titulo VARCHAR(200) NOT NULL,
  descricao TEXT,
  created_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS sessao_votacao (
  id BINARY(16) PRIMARY KEY,
  pauta_id BINARY(16) NOT NULL UNIQUE,
  opened_at DATETIME(6) NOT NULL,
  duration_seconds INT NOT NULL,
  closes_at DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  CONSTRAINT fk_sessao_pauta FOREIGN KEY (pauta_id) REFERENCES pauta(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS voto (
  id BINARY(16) PRIMARY KEY,
  pauta_id BINARY(16) NOT NULL,
  cpf VARCHAR(11) NOT NULL,
  opcao VARCHAR(3) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES pauta(id) ON DELETE CASCADE
);

ALTER TABLE voto
  ADD CONSTRAINT uk_voto_pauta_cpf UNIQUE (pauta_id, cpf);

CREATE INDEX idx_voto_pauta ON voto(pauta_id);
CREATE INDEX idx_sessao_pauta_status ON sessao_votacao(pauta_id, status);
CREATE INDEX idx_sessao_pauta_opened ON sessao_votacao(pauta_id, opened_at);

