package com.example.votacao.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacao.domain.Pauta;
import com.example.votacao.domain.SessaoVotacao;
import com.example.votacao.dto.CreatePautaRequest;
import com.example.votacao.dto.ResultadoDTO;
import com.example.votacao.dto.enums.OpcaoVoto;
import com.example.votacao.dto.enums.SessaoStatus;
import com.example.votacao.exception.ConflictException;
import com.example.votacao.exception.NotFoundException;
import com.example.votacao.repository.PautaRepository;
import com.example.votacao.repository.SessaoVotacaoRepository;
import com.example.votacao.repository.VotoRepository;

@Service
public class PautaService {

    private final PautaRepository repo;
    private final SessaoVotacaoRepository sessaoRepo;
    private final VotoRepository votoRepo;

    public PautaService(PautaRepository repo, SessaoVotacaoRepository sessaoRepo, VotoRepository votoRepo) {
        this.repo = repo;
        this.sessaoRepo = sessaoRepo;
        this.votoRepo = votoRepo;
    }

    public Pauta criar(CreatePautaRequest req) {
        Pauta p = new Pauta();
        p.setTitulo(req.titulo());
        p.setDescricao(req.descricao());
        return repo.save(p);
    }

    public Pauta get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Pauta não encontrada"));
    }

    public List<Pauta> listar() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public SessaoVotacao abrirSessao(UUID pautaId, Integer duracaoMinutos) {
        get(pautaId); // 404 se não existir

        // já existe sessão ABERTA? então 409
        if (sessaoRepo.existsByPautaIdAndStatus(pautaId, SessaoStatus.ABERTA)) {
            throw new ConflictException("Sessão já aberta");
        }

        int dur = (duracaoMinutos == null || duracaoMinutos <= 0) ? 60 : duracaoMinutos;
        // construtor da tua entidade já deve setar status=ABERTA e closesAt = opensAt + dur (em minutos)
        SessaoVotacao s = new SessaoVotacao(pautaId, Instant.now(), dur);
        return sessaoRepo.save(s);
    }



    public ResultadoDTO resultado(UUID pautaId) {
        long sim = votoRepo.countByPautaIdAndOpcao(pautaId, OpcaoVoto.SIM);
        long nao = votoRepo.countByPautaIdAndOpcao(pautaId, OpcaoVoto.NAO);
        long total = sim + nao;

        boolean aberta = sessaoRepo
                .findFirstByPautaIdAndStatusOrderByOpenedAtDesc(pautaId, SessaoStatus.ABERTA)
                .filter(s -> Instant.now().isBefore(s.getClosesAt()))
                .isPresent();

        String status = aberta ? "ABERTA" : "ENCERRADA";
        return new ResultadoDTO(sim, nao, total, status);
    }
}
