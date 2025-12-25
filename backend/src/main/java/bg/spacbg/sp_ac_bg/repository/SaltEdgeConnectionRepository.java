package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.SaltEdgeConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaltEdgeConnectionRepository extends JpaRepository<SaltEdgeConnectionEntity, Integer> {

    Optional<SaltEdgeConnectionEntity> findBySaltEdgeConnectionId(String saltEdgeConnectionId);

    Optional<SaltEdgeConnectionEntity> findByBankProfileId(Integer bankProfileId);

    List<SaltEdgeConnectionEntity> findBySaltEdgeCustomerId(String saltEdgeCustomerId);

    List<SaltEdgeConnectionEntity> findByStatus(String status);

    @Query("SELECT c FROM SaltEdgeConnectionEntity c WHERE c.bankProfile.company.id = :companyId")
    List<SaltEdgeConnectionEntity> findByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT c FROM SaltEdgeConnectionEntity c WHERE c.status = 'active' AND c.consentExpiresAt < CURRENT_TIMESTAMP")
    List<SaltEdgeConnectionEntity> findExpiredConsents();
}
