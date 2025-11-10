package com.example.votacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacao.domain.SessaoVotacao;
import com.example.votacao.dto.CreatePautaRequest;
import com.example.votacao.dto.ResultadoDTO;
import com.example.votacao.dto.VotoRequest;
import com.example.votacao.dto.enums.OpcaoVoto;
import com.example.votacao.exception.ConflictException;
import com.example.votacao.exception.UnprocessableException;
import com.example.votacao.repository.SessaoVotacaoRepository;
import com.example.votacao.service.PautaService;
import com.example.votacao.service.VotacaoService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BasicFlowTest {

    @Autowired PautaService pautaService;
    @Autowired VotacaoService votacaoService;
    @Autowired SessaoVotacaoRepository sessaoRepo;

    @Test
    @DisplayName("Default de duração da sessão deve ser >= 60s quando não informado")
    void defaultDurationIsAtLeast60Seconds() {
        var pauta = pautaService.criar(new CreatePautaRequest("Pauta Duração", "Sem duração explícita"));

        // abrir sem duração -> usa default (60)
        SessaoVotacao s = pautaService.abrirSessao(pauta.getId(), null);

        // Valida que closesAt > openedAt em pelo menos ~60 segundos (tolerância para arredondamentos)
        var diff = Duration.between(s.getOpenedAt(), s.getClosesAt());
        assertThat(diff.getSeconds()).isGreaterThanOrEqualTo(60);
    }

    @Test
    @DisplayName("CPF vota 1x por pauta e pode votar em outra pauta")
    void oneVotePerPautaSameCpfCanVoteInOtherPauta() {
        var pautaA = pautaService.criar(new CreatePautaRequest("Pauta A", "Teste A"));
        var pautaB = pautaService.criar(new CreatePautaRequest("Pauta B", "Teste B"));

        pautaService.abrirSessao(pautaA.getId(), 60);
        pautaService.abrirSessao(pautaB.getId(), 60);

        var req = new VotoRequest("11122233344", OpcaoVoto.SIM);

        // 1º voto na pauta A -> OK
        votacaoService.votar(pautaA.getId(), req);

        // 2º voto na pauta A com mesmo CPF -> 409
        assertThatThrownBy(() -> votacaoService.votar(pautaA.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Associado já votou nesta pauta");

        // Voto na pauta B com o mesmo CPF -> OK
        votacaoService.votar(pautaB.getId(), req);
    }

    @Test
    @DisplayName("Sessão precisa estar aberta no momento do voto")
    void mustBeOpenToVote() {
        var pauta = pautaService.criar(new CreatePautaRequest("Pauta Sessão Encerrada", "Teste"));
   
        SessaoVotacao s = pautaService.abrirSessao(pauta.getId(), 1); // 1 minuto
        // Simula encerramento rápido
        s.setClosesAt(s.getOpenedAt()); // fecha imediatamente
        sessaoRepo.save(s);

        var req = new VotoRequest("55566677788", OpcaoVoto.NAO);

        assertThatThrownBy(() -> votacaoService.votar(pauta.getId(), req))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Sessão encerrada");
    }

    @Test
    @DisplayName("Resultado contabiliza SIM/NAO e total corretamente")
    void resultadoCountsCorrectly() {
        var pauta = pautaService.criar(new CreatePautaRequest("Pauta Resultado", "Teste Resultado"));
        pautaService.abrirSessao(pauta.getId(), 60);

        votacaoService.votar(pauta.getId(), new VotoRequest("11122233344", OpcaoVoto.SIM));
        votacaoService.votar(pauta.getId(), new VotoRequest("99988877766", OpcaoVoto.NAO));

        ResultadoDTO r = pautaService.resultado(pauta.getId());
        assertThat(r.sim()).isEqualTo(1L);
        assertThat(r.nao()).isEqualTo(1L);
        assertThat(r.total()).isEqualTo(2L);
        assertThat(r.status()).isIn("ABERTA", "ENCERRADA"); 
    }
}
