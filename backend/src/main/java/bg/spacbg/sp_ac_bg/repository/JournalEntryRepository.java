package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Integer>,
        JpaSpecificationExecutor<JournalEntryEntity> {

    List<JournalEntryEntity> findByCompany_Id(Integer companyId);

    List<JournalEntryEntity> findByCompany_IdAndIsPostedFalse(Integer companyId);

    List<JournalEntryEntity> findByCompany_IdAndIsPostedTrue(Integer companyId);

    Optional<JournalEntryEntity> findByEntryNumber(String entryNumber);

    @Query("SELECT je FROM JournalEntryEntity je WHERE je.company.id = :companyId " +
           "AND je.accountingDate BETWEEN :fromDate AND :toDate")
    List<JournalEntryEntity> findByCompanyIdAndDateRange(
            @Param("companyId") Integer companyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT je FROM JournalEntryEntity je WHERE je.company.id = :companyId " +
           "AND je.vatDate BETWEEN :fromDate AND :toDate")
    List<JournalEntryEntity> findByCompanyIdAndVatDateBetween(
        @Param("companyId") Integer companyId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @Query("SELECT je FROM JournalEntryEntity je WHERE je.company.id = :companyId " +
           "AND je.documentNumber = :documentNumber")
    List<JournalEntryEntity> findByCompanyIdAndDocumentNumber(
            @Param("companyId") Integer companyId,
            @Param("documentNumber") String documentNumber);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(je.entry_number FROM LENGTH(:prefix) + 1) AS integer)), 0) " +
           "FROM journal_entries je WHERE je.company_id = :companyId " +
           "AND je.entry_number LIKE :prefix || '%'", nativeQuery = true)
    Integer findMaxEntryNumberByPrefix(
            @Param("companyId") Integer companyId,
            @Param("prefix") String prefix);

    @Query("SELECT COUNT(je) FROM JournalEntryEntity je WHERE je.company.id = :companyId " +
           "AND YEAR(je.accountingDate) = :year")
    Long countByCompanyIdAndYear(
            @Param("companyId") Integer companyId,
            @Param("year") Integer year);
}
