package com.example.votacao.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.example.votacao.dto.enums.OpcaoVoto;

@Entity
@Table(
		  name = "voto",
		  uniqueConstraints = @UniqueConstraint(name = "uk_voto_pauta_cpf", columnNames = {"pauta_id","cpf"})
		)
public class Voto {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID pautaId;

    @Column(nullable = false, length = 11)
    private String cpf; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private OpcaoVoto opcao;

    @Column(nullable = false)
    private Instant createdAt;

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPautaId() { return pautaId; }
    public void setPautaId(UUID pautaId) { this.pautaId = pautaId; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public OpcaoVoto getOpcao() { return opcao; }
    public void setOpcao(OpcaoVoto opcao) { this.opcao = opcao; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    
    
    
}

