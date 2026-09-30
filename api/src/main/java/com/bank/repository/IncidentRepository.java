package com.bank.repository;

import com.bank.domain.Incident;
import com.bank.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findAllByOrderByCreatedAtDesc();

    Optional<Incident> findByTransferId(Long transferId);

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);
}
