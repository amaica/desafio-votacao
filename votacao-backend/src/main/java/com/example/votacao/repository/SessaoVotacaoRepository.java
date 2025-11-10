package com.example.votacao.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.votacao.domain.SessaoVotacao;
import com.example.votacao.dto.enums.SessaoStatus;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, UUID> {
	boolean existsByPautaIdAndStatus(UUID pautaId, SessaoStatus status);

	Optional<SessaoVotacao> findFirstByPautaIdAndStatusOrderByOpenedAtDesc(UUID pautaId, SessaoStatus status);

	Optional<SessaoVotacao> findByPautaId(UUID pautaId);
}
