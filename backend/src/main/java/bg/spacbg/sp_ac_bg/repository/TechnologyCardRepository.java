package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.TechnologyCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnologyCardRepository extends JpaRepository<TechnologyCardEntity, Integer> {

    List<TechnologyCardEntity> findByCompanyId(Integer companyId);

    List<TechnologyCardEntity> findByCompanyIdAndIsActiveTrue(Integer companyId);

    Optional<TechnologyCardEntity> findByCompanyIdAndCode(Integer companyId, String code);

    boolean existsByCompanyIdAndCode(Integer companyId, String code);

    @Query("SELECT tc FROM TechnologyCardEntity tc " +
           "LEFT JOIN FETCH tc.stages " +
           "WHERE tc.id = :id")
    Optional<TechnologyCardEntity> findByIdWithStages(@Param("id") Integer id);

    @Query("SELECT tc FROM TechnologyCardEntity tc " +
           "LEFT JOIN FETCH tc.stages " +
           "WHERE tc.company.id = :companyId AND tc.isActive = true " +
           "ORDER BY tc.name")
    List<TechnologyCardEntity> findActiveByCompanyIdWithStages(@Param("companyId") Integer companyId);

    @Query("SELECT COUNT(tc) FROM TechnologyCardEntity tc WHERE tc.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Integer companyId);
}
