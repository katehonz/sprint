package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.SaltEdgeTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaltEdgeTransactionRepository extends JpaRepository<SaltEdgeTransactionEntity, Integer> {

    Optional<SaltEdgeTransactionEntity> findBySaltEdgeTransactionId(String saltEdgeTransactionId);

    List<SaltEdgeTransactionEntity> findBySaltEdgeAccountId(String saltEdgeAccountId);

    List<SaltEdgeTransactionEntity> findByBankProfileId(Integer bankProfileId);

    List<SaltEdgeTransactionEntity> findByBankProfileIdAndIsProcessedFalse(Integer bankProfileId);

    Page<SaltEdgeTransactionEntity> findByBankProfileIdOrderByMadeOnDesc(Integer bankProfileId, Pageable pageable);

    List<SaltEdgeTransactionEntity> findBySaltEdgeAccountIdAndMadeOnBetween(
            String saltEdgeAccountId, LocalDate from, LocalDate to);

    @Query("SELECT t FROM SaltEdgeTransactionEntity t WHERE t.bankProfile.id = :bankProfileId " +
           "AND t.madeOn BETWEEN :from AND :to ORDER BY t.madeOn DESC")
    List<SaltEdgeTransactionEntity> findByBankProfileAndDateRange(
            @Param("bankProfileId") Integer bankProfileId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COUNT(t) FROM SaltEdgeTransactionEntity t WHERE t.bankProfile.id = :bankProfileId AND t.isProcessed = false")
    long countUnprocessedByBankProfile(@Param("bankProfileId") Integer bankProfileId);

    boolean existsBySaltEdgeTransactionId(String saltEdgeTransactionId);
}
