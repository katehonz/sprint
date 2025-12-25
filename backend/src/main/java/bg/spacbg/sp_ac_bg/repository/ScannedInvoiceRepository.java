package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity.InvoiceDirection;
import bg.spacbg.sp_ac_bg.model.entity.ScannedInvoiceEntity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScannedInvoiceRepository extends JpaRepository<ScannedInvoiceEntity, Integer> {

    List<ScannedInvoiceEntity> findByCompanyIdOrderByCreatedAtDesc(Integer companyId);

    List<ScannedInvoiceEntity> findByCompanyIdAndDirectionOrderByCreatedAtDesc(Integer companyId, InvoiceDirection direction);

    List<ScannedInvoiceEntity> findByCompanyIdAndStatusOrderByCreatedAtDesc(Integer companyId, ProcessingStatus status);

    List<ScannedInvoiceEntity> findByCompanyIdAndDirectionAndStatusOrderByCreatedAtDesc(
            Integer companyId, InvoiceDirection direction, ProcessingStatus status);

    List<ScannedInvoiceEntity> findByCompanyIdAndRequiresManualReviewTrueOrderByCreatedAtDesc(Integer companyId);

    long countByCompanyIdAndStatus(Integer companyId, ProcessingStatus status);

    long countByCompanyIdAndDirectionAndStatus(Integer companyId, InvoiceDirection direction, ProcessingStatus status);
}
