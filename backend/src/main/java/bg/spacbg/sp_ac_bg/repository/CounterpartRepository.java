package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.CounterpartEntity;
import bg.spacbg.sp_ac_bg.model.enums.CounterpartType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterpartRepository extends JpaRepository<CounterpartEntity, Integer> {

    List<CounterpartEntity> findByCompanyId(Integer companyId);

    List<CounterpartEntity> findByCompanyIdAndIsActiveTrue(Integer companyId);

    Optional<CounterpartEntity> findByCompanyIdAndEik(Integer companyId, String eik);

    Optional<CounterpartEntity> findByCompanyIdAndVatNumber(Integer companyId, String vatNumber);

    List<CounterpartEntity> findByCompanyIdAndCounterpartType(Integer companyId, CounterpartType type);

    @Query("SELECT c FROM CounterpartEntity c WHERE c.company.id = :companyId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR c.eik LIKE CONCAT('%', :query, '%') " +
           "OR c.vatNumber LIKE CONCAT('%', :query, '%'))")
    List<CounterpartEntity> searchByCompanyIdAndQuery(
            @Param("companyId") Integer companyId,
            @Param("query") String query);

    boolean existsByCompanyIdAndEik(Integer companyId, String eik);
}
