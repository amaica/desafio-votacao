package com.example.votacao;



import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.votacao.client.CpfEligibilityClient;
import com.example.votacao.dto.VotoRequest;
import com.example.votacao.dto.enums.OpcaoVoto;
import com.example.votacao.exception.UnprocessableException;
import com.example.votacao.repository.PautaRepository;
import com.example.votacao.repository.SessaoVotacaoRepository;
import com.example.votacao.repository.VotoRepository;
import com.example.votacao.service.VotacaoService;

class VotacaoServiceCpfTest {

    VotoRepository votos = mock(VotoRepository.class);
    PautaRepository pautas = mock(PautaRepository.class);
    SessaoVotacaoRepository sessoes = mock(SessaoVotacaoRepository.class);
    CpfEligibilityClient cpfClient = mock(CpfEligibilityClient.class);
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

    VotacaoService service;

    @BeforeEach
    void setUp() {
        service = new VotacaoService(votos, pautas, sessoes, cpfClient, clock);
    }

    @Test
    void rejeitaCpfInvalido() {
        var pautaId = UUID.randomUUID();
        var req = new VotoRequest("123", OpcaoVoto.SIM);

        assertThatThrownBy(() -> service.votar(pautaId, req))
            .isInstanceOf(UnprocessableException.class)
            .hasMessageContaining("CPF inválido");
    }
}
