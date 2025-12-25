package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.SaltEdgeCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaltEdgeCustomerRepository extends JpaRepository<SaltEdgeCustomerEntity, Integer> {

    Optional<SaltEdgeCustomerEntity> findByCompanyId(Integer companyId);

    Optional<SaltEdgeCustomerEntity> findBySaltEdgeCustomerId(String saltEdgeCustomerId);

    boolean existsByCompanyId(Integer companyId);
}
