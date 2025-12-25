package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.InventoryBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalanceEntity, Integer> {

    List<InventoryBalanceEntity> findByCompanyId(Integer companyId);

    Optional<InventoryBalanceEntity> findByCompanyIdAndAccountId(Integer companyId, Integer accountId);

    boolean existsByCompanyIdAndAccountId(Integer companyId, Integer accountId);
}
