package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, Integer> {
    Optional<CurrencyEntity> findByCode(String code);
    Optional<CurrencyEntity> findByIsBaseCurrencyTrue();
    List<CurrencyEntity> findByIsActiveTrue();
    boolean existsByCode(String code);
}
