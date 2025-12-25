package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Integer> {
    Optional<CompanyEntity> findByEik(String eik);
    List<CompanyEntity> findByIsActiveTrue();
    boolean existsByEik(String eik);
}
