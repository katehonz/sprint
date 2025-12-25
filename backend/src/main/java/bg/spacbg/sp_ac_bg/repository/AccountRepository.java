package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Integer> {

    List<AccountEntity> findByCompanyIdAndIsActiveTrue(Integer companyId);

    List<AccountEntity> findByCompanyId(Integer companyId);

    List<AccountEntity> findByCompanyIdAndParentIsNull(Integer companyId);

    List<AccountEntity> findByCompanyIdAndParentId(Integer companyId, Integer parentId);

    List<AccountEntity> findByCompanyIdAndIsAnalyticalTrue(Integer companyId);

    Optional<AccountEntity> findByCompanyIdAndCode(Integer companyId, String code);

    boolean existsByCompanyIdAndCode(Integer companyId, String code);

    @Query("SELECT a FROM AccountEntity a WHERE a.company.id = :companyId ORDER BY a.code")
    List<AccountEntity> findAllByCompanyIdOrderByCode(@Param("companyId") Integer companyId);

    @Query("SELECT a FROM AccountEntity a WHERE a.company.id = :companyId AND a.accountClass = :accountClass")
    List<AccountEntity> findByCompanyIdAndAccountClass(
            @Param("companyId") Integer companyId,
            @Param("accountClass") Integer accountClass);
}
