package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, Integer> {

    default SystemSettingsEntity getSettings() {
        return findById(1).orElseGet(() -> {
            SystemSettingsEntity settings = new SystemSettingsEntity();
            settings.setId(1);
            return save(settings);
        });
    }
}
