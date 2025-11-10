package com.example.votacao.dto;
import com.example.votacao.dto.enums.OpcaoVoto;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VotoRequest(
    @JsonProperty("cpf")
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(
        regexp = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
        message = "CPF deve ter 11 dígitos"
    )
    String cpf,

    @JsonProperty("opcao")
    @NotNull(message = "Opção é obrigatória")
    OpcaoVoto opcao
) {}
