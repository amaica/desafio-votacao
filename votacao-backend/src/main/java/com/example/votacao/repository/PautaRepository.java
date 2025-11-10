package com.example.votacao.repository;
import com.example.votacao.domain.Pauta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PautaRepository extends JpaRepository<Pauta, UUID> {}
