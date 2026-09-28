package com.bank.repository;

import com.bank.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKeyAndInitiatedBy(String idempotencyKey, Long initiatedBy);

    @Query("""
            select t from Transfer t
            where t.initiatedBy = :userId
               or t.fromAccountId in :accountIds
               or t.toAccountId in :accountIds
            order by t.createdAt desc
            """)
    List<Transfer> findVisibleToUser(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);
}
