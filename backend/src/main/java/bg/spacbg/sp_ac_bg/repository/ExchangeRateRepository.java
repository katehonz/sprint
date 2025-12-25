package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Integer> {

    List<ExchangeRateEntity> findAllByOrderByValidDateDesc();

    List<ExchangeRateEntity> findByFromCurrency_CodeOrderByValidDateDesc(String code);

    default List<ExchangeRateEntity> findByFromCurrencyCodeOrderByValidDateDesc(String code) {
        return findByFromCurrency_CodeOrderByValidDateDesc(code);
    }

    List<ExchangeRateEntity> findByFromCurrencyIdAndToCurrencyIdOrderByValidDateDesc(
            Integer fromCurrencyId, Integer toCurrencyId);

    @Query("SELECT er FROM ExchangeRateEntity er " +
           "WHERE er.fromCurrency.code = :fromCode " +
           "AND er.toCurrency.code = :toCode " +
           "AND er.validDate = :date")
    Optional<ExchangeRateEntity> findByCodesAndDate(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("date") LocalDate date);

    @Query("SELECT er FROM ExchangeRateEntity er " +
           "WHERE er.fromCurrency.code = :fromCode " +
           "AND er.toCurrency.code = :toCode " +
           "AND er.isActive = true " +
           "ORDER BY er.validDate DESC " +
           "LIMIT 1")
    Optional<ExchangeRateEntity> findLatestByCodesActive(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode);

    @Query("SELECT er FROM ExchangeRateEntity er " +
           "WHERE er.validDate = :date " +
           "AND er.fromCurrency.isBaseCurrency = true")
    List<ExchangeRateEntity> findByValidDateAndBaseCurrency(@Param("date") LocalDate date);

    boolean existsByFromCurrencyIdAndToCurrencyIdAndValidDate(
            Integer fromCurrencyId, Integer toCurrencyId, LocalDate validDate);
}
