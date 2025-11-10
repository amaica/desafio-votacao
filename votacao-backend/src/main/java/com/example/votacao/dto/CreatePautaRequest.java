package com.example.votacao.dto;
import jakarta.validation.constraints.NotBlank;
public record CreatePautaRequest(@NotBlank String titulo, String descricao) {}
