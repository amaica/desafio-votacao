package com.example.votacao.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.votacao.domain.Pauta;
import com.example.votacao.dto.CreatePautaRequest;
import com.example.votacao.dto.ResultadoDTO;
import com.example.votacao.dto.VotoRequest;
import com.example.votacao.service.PautaService;
import com.example.votacao.service.VotacaoService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@CrossOrigin("*")
@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaService pautaService;
    private final VotacaoService votacaoService;

    public PautaController(PautaService pautaService, VotacaoService votacaoService) {
        this.pautaService = pautaService;
        this.votacaoService = votacaoService;
    }

    @GetMapping
    public List<Pauta> listar() { return pautaService.listar(); }

    @PostMapping
    public Pauta criar(@RequestBody CreatePautaRequest req) { return pautaService.criar(req); }

    @PostMapping("/{id}/sessao")
    public ResponseEntity<Void> abrirSessao(
            @PathVariable UUID id,
            @RequestParam(name = "duracao", required = false, defaultValue = "60") Integer duracaoMin) {
        pautaService.abrirSessao(id, duracaoMin);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/votar")
    public ResponseEntity<Void> votar(@PathVariable UUID id, @RequestBody VotoRequest req) {
        votacaoService.votar(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/resultado")
    public ResultadoDTO resultado(@PathVariable UUID id) {
        return pautaService.resultado(id);
    }
}
