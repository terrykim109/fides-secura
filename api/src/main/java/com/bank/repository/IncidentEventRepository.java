package com.bank.repository;

import com.bank.domain.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, IncidentEvent.Pk> {

    List<IncidentEvent> findByIncidentId(Long incidentId);
}
