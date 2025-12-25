package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.FixedAssetCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedAssetCategoryRepository extends JpaRepository<FixedAssetCategoryEntity, Integer> {

    List<FixedAssetCategoryEntity> findByIsActiveTrue();

    Optional<FixedAssetCategoryEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<FixedAssetCategoryEntity> findByTaxCategory(Integer taxCategory);
}
