package com.example.votacao.client;

import com.example.votacao.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.Duration;

@Component
public class WebClientCpfEligibilityClient implements CpfEligibilityClient {

    private final WebClient webClient;

    public WebClientCpfEligibilityClient(@Value("${cpf.base-url}") String baseUrl){
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public EligibilityResult check(String cpf) {
        try {
            var resp = webClient.get()
                .uri("/users/{cpf}", cpf)
                .retrieve()
                .bodyToMono(EligibilityResult.class)
                .timeout(Duration.ofSeconds(1))
                .block();
            if (resp == null) throw new RuntimeException("Empty response");
            return resp;
        } catch (WebClientResponseException e){
            if (e.getStatusCode() == HttpStatus.NOT_FOUND){
                throw new NotFoundException("CPF inválido");
            }
            throw e;
        }
    }
}
