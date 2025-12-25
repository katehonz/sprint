package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.BankProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankProfileRepository extends JpaRepository<BankProfileEntity, Integer> {
    List<BankProfileEntity> findByCompanyId(Integer companyId);
    List<BankProfileEntity> findByCompanyIdAndIsActiveTrue(Integer companyId);
    Optional<BankProfileEntity> findByIban(String iban);
    boolean existsByIban(String iban);
}
