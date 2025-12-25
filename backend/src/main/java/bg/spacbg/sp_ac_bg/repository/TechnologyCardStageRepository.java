package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.TechnologyCardStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnologyCardStageRepository extends JpaRepository<TechnologyCardStageEntity, Integer> {

    List<TechnologyCardStageEntity> findByTechnologyCardIdOrderByStageOrderAsc(Integer technologyCardId);

    void deleteByTechnologyCardId(Integer technologyCardId);

    int countByTechnologyCardId(Integer technologyCardId);
}
