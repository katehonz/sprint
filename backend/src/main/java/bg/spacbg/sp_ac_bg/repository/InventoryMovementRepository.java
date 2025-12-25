package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Integer> {

    List<InventoryMovementEntity> findByCompanyIdOrderByMovementDateDescCreatedAtDesc(Integer companyId);

    List<InventoryMovementEntity> findByAccountIdOrderByMovementDateDescCreatedAtDesc(Integer accountId);

    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND (:accountId IS NULL OR m.account.id = :accountId) " +
           "AND (:fromDate IS NULL OR m.movementDate >= :fromDate) " +
           "AND (:toDate IS NULL OR m.movementDate <= :toDate) " +
           "AND (:movementType IS NULL OR m.movementType = :movementType) " +
           "ORDER BY m.movementDate DESC, m.createdAt DESC")
    List<InventoryMovementEntity> findByFilter(
            Integer companyId,
            Integer accountId,
            LocalDate fromDate,
            LocalDate toDate,
            String movementType);

    List<InventoryMovementEntity> findByJournalEntry_Id(Integer journalEntryId);

    void deleteByJournalEntry_Id(Integer journalEntryId);

    // ========== Методи за количествена оборотна ведомост ==========

    /**
     * Движения преди определена дата (за начално салдо)
     */
    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND m.movementDate < :fromDate " +
           "ORDER BY m.account.id ASC, m.movementDate ASC, m.id ASC")
    List<InventoryMovementEntity> findMovementsBeforeDate(Integer companyId, LocalDate fromDate);

    /**
     * Движения в период (за обороти)
     */
    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND m.movementDate >= :fromDate AND m.movementDate <= :toDate " +
           "ORDER BY m.account.id ASC, m.movementDate ASC, m.id ASC")
    List<InventoryMovementEntity> findMovementsInPeriod(Integer companyId, LocalDate fromDate, LocalDate toDate);

    /**
     * Движения до определена дата (за изчисляване на СПЦ към дата)
     */
    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND m.account.id = :accountId AND m.movementDate <= :asOfDate " +
           "ORDER BY m.movementDate ASC, m.id ASC")
    List<InventoryMovementEntity> findMovementsUpToDate(Integer companyId, Integer accountId, LocalDate asOfDate);

    // ========== Методи за корекции на СПЦ ==========

    /**
     * CREDIT движения след определена дата за конкретна сметка
     */
    @Query("SELECT m FROM InventoryMovementEntity m WHERE m.company.id = :companyId " +
           "AND m.account.id = :accountId AND m.movementType = 'CREDIT' " +
           "AND m.movementDate > :newEntryDate " +
           "ORDER BY m.movementDate ASC, m.id ASC")
    List<InventoryMovementEntity> findCreditMovementsAfterDate(Integer companyId, Integer accountId, LocalDate newEntryDate);

    /**
     * Проверка дали има движение за entry line
     */
    Optional<InventoryMovementEntity> findByEntryLine_Id(Integer entryLineId);

    /**
     * Уникални accountId с движения
     */
    @Query("SELECT DISTINCT m.account.id FROM InventoryMovementEntity m WHERE m.company.id = :companyId")
    List<Integer> findDistinctAccountIdsByCompanyId(Integer companyId);
}
