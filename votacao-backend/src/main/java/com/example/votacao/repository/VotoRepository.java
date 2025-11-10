package com.example.votacao.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.votacao.domain.Voto;
import com.example.votacao.dto.enums.OpcaoVoto;

public interface VotoRepository extends JpaRepository<Voto, UUID> {

    
    boolean existsByPautaIdAndCpf(UUID pautaId, String cpf);  
    long countByPautaIdAndOpcao(UUID pautaId, com.example.votacao.dto.enums.OpcaoVoto opcao);

    @Query("SELECT v.opcao, COUNT(v) FROM Voto v WHERE v.pautaId = :pautaId GROUP BY v.opcao")
    List<Object[]> countByOpcao(UUID pautaId);
}
