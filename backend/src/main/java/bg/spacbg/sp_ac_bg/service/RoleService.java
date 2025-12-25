package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.entity.Role;
import bg.spacbg.sp_ac_bg.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }
    
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    public Role updateRole(Long id, Role roleDetails) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        role.setName(roleDetails.getName());
        role.setPermissions(roleDetails.getPermissions());

        return roleRepository.save(role);
    }
}
