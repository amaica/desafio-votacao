package com.example.votacao.domain;



import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.*;

import com.example.votacao.dto.enums.SessaoStatus;

@Entity
@Table(
  name = "sessao_votacao",
  uniqueConstraints = @UniqueConstraint(name = "uk_sessao_pauta", columnNames = { "pauta_id" })
)
public class SessaoVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "pauta_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID pautaId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessaoStatus status;

    public SessaoVotacao() {}

    // duracaoEmMinutos
    public SessaoVotacao(UUID pautaId, Instant openedAt, int duracaoEmMinutos) {
        this.pautaId = pautaId;
        this.openedAt = openedAt;
        this.durationSeconds = Math.max(60, duracaoEmMinutos * 60);
        this.closesAt = openedAt.plus(this.durationSeconds, ChronoUnit.SECONDS);
        this.status = SessaoStatus.ABERTA;
    }

    public boolean isAbertaAgora(Clock clock) {
        return this.status == SessaoStatus.ABERTA && Instant.now(clock).isBefore(this.closesAt);
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPautaId() { return pautaId; }
    public void setPautaId(UUID pautaId) { this.pautaId = pautaId; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public Instant getClosesAt() { return closesAt; }
    public void setClosesAt(Instant closesAt) { this.closesAt = closesAt; }

    public SessaoStatus getStatus() { return status; }
    public void setStatus(SessaoStatus status) { this.status = status; }
    
    
    
}

