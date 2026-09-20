package com.bank.repository;

import com.bank.domain.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    List<SecurityEvent> findTop20BySubjectUserIdOrderByCreatedAtDesc(Long subjectUserId);
}
