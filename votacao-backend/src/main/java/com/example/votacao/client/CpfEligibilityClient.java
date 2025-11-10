package com.example.votacao.client;

public interface CpfEligibilityClient {
    enum Status { ABLE_TO_VOTE, UNABLE_TO_VOTE }
    record EligibilityResult(Status status){}
    EligibilityResult check(String cpf);
}
