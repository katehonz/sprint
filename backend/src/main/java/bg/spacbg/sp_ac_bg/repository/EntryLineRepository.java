package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.EntryLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntryLineRepository extends JpaRepository<EntryLineEntity, Integer> {

    List<EntryLineEntity> findByJournalEntry_Id(Integer journalEntryId);

    List<EntryLineEntity> findByJournalEntry_IdOrderByLineOrder(Integer journalEntryId);

    List<EntryLineEntity> findByAccount_Id(Integer accountId);

    void deleteByJournalEntry_Id(Integer journalEntryId);

    @Query("SELECT el FROM EntryLineEntity el " +
           "JOIN el.journalEntry je " +
           "WHERE el.account.id = :accountId " +
           "AND je.accountingDate BETWEEN :fromDate AND :toDate " +
           "AND je.isPosted = true")
    List<EntryLineEntity> findByAccountIdAndDateRange(
            @Param("accountId") Integer accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(el.debitAmount), 0) FROM EntryLineEntity el " +
           "JOIN el.journalEntry je " +
           "WHERE el.account.id = :accountId AND je.isPosted = true")
    BigDecimal sumDebitByAccountId(@Param("accountId") Integer accountId);

    @Query("SELECT COALESCE(SUM(el.creditAmount), 0) FROM EntryLineEntity el " +
           "JOIN el.journalEntry je " +
           "WHERE el.account.id = :accountId AND je.isPosted = true")
    BigDecimal sumCreditByAccountId(@Param("accountId") Integer accountId);

    @Query("SELECT COALESCE(SUM(el.debitAmount), 0) FROM EntryLineEntity el " +
           "JOIN el.journalEntry je " +
           "WHERE el.account.id = :accountId " +
           "AND je.accountingDate BETWEEN :fromDate AND :toDate " +
           "AND je.isPosted = true")
    BigDecimal sumDebitByAccountIdAndDateRange(
            @Param("accountId") Integer accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(el.creditAmount), 0) FROM EntryLineEntity el " +
           "JOIN el.journalEntry je " +
           "WHERE el.account.id = :accountId " +
           "AND je.accountingDate BETWEEN :fromDate AND :toDate " +
           "AND je.isPosted = true")
    BigDecimal sumCreditByAccountIdAndDateRange(
            @Param("accountId") Integer accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // Report queries
    @Query("SELECT el FROM EntryLineEntity el " +
           "JOIN FETCH el.journalEntry je " +
           "JOIN FETCH el.account " +
           "LEFT JOIN FETCH el.counterpart " +
           "WHERE el.account.id = :accountId " +
           "AND je.company.id = :companyId " +
           "AND je.accountingDate < :date " +
           "AND je.isPosted = true")
    List<EntryLineEntity> findByAccountIdAndPostedBeforeDate(
            @Param("accountId") Integer accountId,
            @Param("companyId") Integer companyId,
            @Param("date") LocalDate date);

    @Query("SELECT el FROM EntryLineEntity el " +
           "JOIN FETCH el.journalEntry je " +
           "JOIN FETCH el.account " +
           "LEFT JOIN FETCH el.counterpart " +
           "WHERE el.account.id = :accountId " +
           "AND je.company.id = :companyId " +
           "AND je.accountingDate >= :startDate " +
           "AND je.accountingDate <= :endDate " +
           "AND je.isPosted = true " +
           "ORDER BY je.accountingDate, el.lineOrder")
    List<EntryLineEntity> findByAccountIdAndPostedBetweenDates(
            @Param("accountId") Integer accountId,
            @Param("companyId") Integer companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT el FROM EntryLineEntity el " +
           "JOIN FETCH el.journalEntry je " +
           "JOIN FETCH el.account " +
           "LEFT JOIN FETCH el.counterpart " +
           "WHERE je.company.id = :companyId " +
           "AND je.accountingDate >= :startDate " +
           "AND je.accountingDate <= :endDate " +
           "AND je.isPosted = true " +
           "ORDER BY je.accountingDate, je.entryNumber, el.lineOrder")
    List<EntryLineEntity> findByCompanyIdAndPostedBetweenDates(
            @Param("companyId") Integer companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
