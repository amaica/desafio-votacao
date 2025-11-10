package com.example.votacao.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.votacao.client.CpfEligibilityClient;
import com.example.votacao.domain.SessaoVotacao;
import com.example.votacao.domain.Voto;
import com.example.votacao.dto.ResultadoResponse;
import com.example.votacao.dto.VotoRequest;
import com.example.votacao.dto.enums.OpcaoVoto;

import com.example.votacao.exception.ConflictException;
import com.example.votacao.exception.ForbiddenException;
import com.example.votacao.exception.NotFoundException;
import com.example.votacao.exception.UnprocessableException;
import com.example.votacao.repository.PautaRepository;
import com.example.votacao.repository.SessaoVotacaoRepository;
import com.example.votacao.repository.VotoRepository;

import java.time.Clock;

@Service
public class VotacaoService {

	private final VotoRepository votos;
	private final PautaRepository pautas;
	private final SessaoVotacaoRepository sessoes;
	private final CpfEligibilityClient cpfClient;
	private final Clock clock;

	public VotacaoService(VotoRepository votos, PautaRepository pautas, SessaoVotacaoRepository sessoes,
			CpfEligibilityClient cpfClient, Clock clock) {
		this.votos = votos;
		this.pautas = pautas;
		this.sessoes = sessoes;
		this.cpfClient = cpfClient;
		this.clock = clock;
	}

	public void votar(UUID pautaId, VotoRequest req) {

	    final String cpf = (req.cpf() == null ? "" : req.cpf().replaceAll("\\D", ""));
	    if (cpf.length() != 11) {
	        throw new UnprocessableException("CPF inválido (informe 11 dígitos)");
	    }

	    // 404 se pauta não existir
	    pautas.findById(pautaId).orElseThrow(() -> new NotFoundException("Pauta não encontrada"));

	    // carrega/cria sessão (uma por pauta)
	    SessaoVotacao s = sessoes.findByPautaId(pautaId)
	        .orElseGet(() -> sessoes.save(new SessaoVotacao(pautaId, Instant.now(clock), 60)));

	    // SANIDADE: a sessão precisa pertencer ao mesmo pautaId do path
	    if (!pautaId.equals(s.getPautaId())) {
	        // se isso acontecer, tem bug de dados — não grava e avisa claro
	        throw new UnprocessableException("Sessão não pertence à pauta informada");
	    }

	    if (!isAbertaAgora(s)) {
	        throw new UnprocessableException("Sessão encerrada");
	    }

	    // elegibilidade desativada em dev (bypass)
	    // ...

	    final com.example.votacao.dto.enums.OpcaoVoto opcao = req.opcao();
	    if (opcao == null) {
	        throw new UnprocessableException("Opção inválida (use SIM ou NAO)");
	    }

	    // PRÉ-CHECAGEM: evita bater no UNIQUE e ainda nos dá diagnóstico limpo
	    if (votos.existsByPautaIdAndCpf(pautaId, cpf)) {
	        throw new ConflictException("Associado já votou nesta pauta");
	    }

	    Voto v = new Voto();
	    v.setPautaId(pautaId);   // <- SEMPRE usa o id do PATH
	    v.setCpf(cpf);
	    v.setOpcao(opcao);
	    v.setCreatedAt(Instant.now(clock));

	    try {
	        votos.save(v);
	    } catch (org.springframework.dao.DataIntegrityViolationException e) {
	        throw new ConflictException("Associado já votou nesta pauta");
	    }
	}

	private boolean isAbertaAgora(SessaoVotacao s) {
	    if (s == null || s.getClosesAt() == null) return false;
	    final String status = String.valueOf(s.getStatus());
	    final boolean statusAberta = status != null && "ABERTA".equalsIgnoreCase(status.trim());
	    return statusAberta && Instant.now(clock).isBefore(s.getClosesAt());
	}


	public ResultadoResponse resultado(UUID pautaId) {
		pautas.findById(pautaId).orElseThrow(() -> new NotFoundException("Pauta não encontrada"));
		SessaoVotacao s = sessoes.findByPautaId(pautaId)
				.orElseThrow(() -> new NotFoundException("Sessão não encontrada"));

		List<Object[]> rows = votos.countByOpcao(pautaId);
		long sim = 0, nao = 0;
		for (Object[] r : rows) {
			String opcao = r[0].toString();
			long count = ((Number) r[1]).longValue();
			if ("SIM".equalsIgnoreCase(opcao))
				sim = count;
			else if ("NAO".equalsIgnoreCase(opcao))
				nao = count;
		}

		String status = isAbertaAgora(s) ? "ABERTA" : "ENCERRADA";
		return new ResultadoResponse(sim, nao, sim + nao, status);
	}
}
