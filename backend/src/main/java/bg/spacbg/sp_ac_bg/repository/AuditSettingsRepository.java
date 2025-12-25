package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.AuditSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditSettingsRepository extends JpaRepository<AuditSettingsEntity, Integer> {

    // Намиране на настройки за роля (глобални)
    @Query("SELECT a FROM AuditSettingsEntity a WHERE a.role = :role AND a.company IS NULL AND a.user IS NULL")
    Optional<AuditSettingsEntity> findGlobalByRole(@Param("role") String role);

    // Намиране на настройки за компания и роля
    @Query("SELECT a FROM AuditSettingsEntity a WHERE a.company.id = :companyId AND a.role = :role AND a.user IS NULL")
    Optional<AuditSettingsEntity> findByCompanyAndRole(
            @Param("companyId") Integer companyId,
            @Param("role") String role);

    // Намиране на настройки за конкретен потребител
    @Query("SELECT a FROM AuditSettingsEntity a WHERE a.user.id = :userId")
    Optional<AuditSettingsEntity> findByUserId(@Param("userId") Integer userId);

    // Всички настройки за компания
    List<AuditSettingsEntity> findByCompany_Id(Integer companyId);

    // Всички глобални настройки
    @Query("SELECT a FROM AuditSettingsEntity a WHERE a.company IS NULL")
    List<AuditSettingsEntity> findAllGlobal();

    // Проверка дали действие трябва да се логва за роля
    @Query("SELECT a FROM AuditSettingsEntity a WHERE " +
           "(a.user.id = :userId OR (a.user IS NULL AND a.role = :role)) " +
           "AND (a.company.id = :companyId OR a.company IS NULL) " +
           "AND a.isEnabled = true " +
           "ORDER BY a.user.id NULLS LAST, a.company.id NULLS LAST")
    List<AuditSettingsEntity> findApplicableSettings(
            @Param("userId") Integer userId,
            @Param("role") String role,
            @Param("companyId") Integer companyId);
}
