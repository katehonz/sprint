package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Integer>,
        JpaSpecificationExecutor<AuditLogEntity> {

    // Намиране по компания
    Page<AuditLogEntity> findByCompany_IdOrderByCreatedAtDesc(Integer companyId, Pageable pageable);

    // Намиране по потребител
    Page<AuditLogEntity> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // Намиране по действие
    Page<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    // Намиране по компания и потребител
    Page<AuditLogEntity> findByCompany_IdAndUser_IdOrderByCreatedAtDesc(
            Integer companyId, Integer userId, Pageable pageable);

    // Търсене по период
    @Query("SELECT a FROM AuditLogEntity a WHERE a.createdAt BETWEEN :fromDate AND :toDate ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByDateRange(
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable);

    // Търсене по компания и период
    @Query("SELECT a FROM AuditLogEntity a WHERE a.company.id = :companyId " +
           "AND a.createdAt BETWEEN :fromDate AND :toDate ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByCompanyAndDateRange(
            @Param("companyId") Integer companyId,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable);

    // Статистика по действия за период
    @Query("SELECT a.action, COUNT(a) FROM AuditLogEntity a " +
           "WHERE a.company.id = :companyId AND a.createdAt >= :fromDate " +
           "GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionForCompany(
            @Param("companyId") Integer companyId,
            @Param("fromDate") OffsetDateTime fromDate);

    // Последни логини на потребител
    @Query("SELECT a FROM AuditLogEntity a WHERE a.user.id = :userId AND a.action = 'LOGIN' " +
           "ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findLastLoginsForUser(@Param("userId") Integer userId, Pageable pageable);

    // Изтриване на стари записи (над 6 месеца)
    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.createdAt < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") OffsetDateTime cutoffDate);

    // Брой записи по компания
    long countByCompany_Id(Integer companyId);
}
