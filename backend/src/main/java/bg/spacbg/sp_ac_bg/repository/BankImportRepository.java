package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.BankImportEntity;
import bg.spacbg.sp_ac_bg.model.enums.BankImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankImportRepository extends JpaRepository<BankImportEntity, Integer> {
    List<BankImportEntity> findByCompanyIdOrderByImportedAtDesc(Integer companyId);
    List<BankImportEntity> findByBankProfileIdOrderByImportedAtDesc(Integer bankProfileId);
    List<BankImportEntity> findByCompanyIdAndStatus(Integer companyId, BankImportStatus status);
}
