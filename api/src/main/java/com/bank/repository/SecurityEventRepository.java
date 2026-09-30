package com.bank.repository;

import com.bank.domain.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findTop20BySubjectUserIdOrderByCreatedAtDesc(Long subjectUserId);

    @Query("""
            select e from SecurityEvent e
            where e.subjectUserId = :userId
              and e.createdAt >= :since
            order by e.createdAt asc
            """)
    List<SecurityEvent> findBySubjectSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("""
            select distinct e.ipAddress from SecurityEvent e
            where e.subjectUserId = :userId
              and e.eventType = :eventType
              and e.createdAt < :before
              and e.ipAddress is not null
            """)
    List<String> findDistinctIpsBefore(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("before") Instant before
    );
}
