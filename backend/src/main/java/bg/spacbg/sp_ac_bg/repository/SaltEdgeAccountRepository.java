package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.SaltEdgeAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaltEdgeAccountRepository extends JpaRepository<SaltEdgeAccountEntity, Integer> {

    Optional<SaltEdgeAccountEntity> findBySaltEdgeAccountId(String saltEdgeAccountId);

    List<SaltEdgeAccountEntity> findBySaltEdgeConnectionId(String saltEdgeConnectionId);

    List<SaltEdgeAccountEntity> findByBankProfileId(Integer bankProfileId);

    Optional<SaltEdgeAccountEntity> findByIban(String iban);

    List<SaltEdgeAccountEntity> findByIsMappedFalse();

    List<SaltEdgeAccountEntity> findBySaltEdgeConnectionIdAndIsMappedFalse(String saltEdgeConnectionId);
}
