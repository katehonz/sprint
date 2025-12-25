package bg.spacbg.sp_ac_bg.repository;

import bg.spacbg.sp_ac_bg.model.entity.UserGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, Integer> {
    Optional<UserGroupEntity> findByName(String name);
}
