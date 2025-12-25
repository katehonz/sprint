package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.UserDto;
import bg.spacbg.sp_ac_bg.model.dto.UserGroupDto;
import bg.spacbg.sp_ac_bg.model.dto.CurrentUserDto;
import bg.spacbg.sp_ac_bg.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public List<UserDto> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public UserDto user(@Argument Integer id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public List<UserGroupDto> userGroups() {
        return userService.getAllGroups();
    }

    @QueryMapping
    public CurrentUserDto currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getCurrentUser(username);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public UserDto createUser(@Argument Map<String, Object> input) {
        return userService.createUser(
            (String) input.get("username"),
            (String) input.get("email"),
            (String) input.get("password"),
            (String) input.get("firstName"),
            (String) input.get("lastName"),
            input.get("groupId") != null ? Integer.parseInt(input.get("groupId").toString()) : null,
            input.get("isActive") != null ? (Boolean) input.get("isActive") : true
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public UserDto updateUser(@Argument Integer id, @Argument Map<String, Object> input) {
        return userService.updateUser(
            id,
            (String) input.get("username"),
            (String) input.get("email"),
            (String) input.get("firstName"),
            (String) input.get("lastName"),
            input.get("groupId") != null ? Integer.parseInt(input.get("groupId").toString()) : null,
            input.get("isActive") != null ? (Boolean) input.get("isActive") : null
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public boolean deleteUser(@Argument Integer id) {
        return userService.deleteUser(id);
    }

    @MutationMapping
    public CurrentUserDto updateProfile(@Argument Map<String, Object> input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateProfile(
            username,
            (String) input.get("email"),
            (String) input.get("firstName"),
            (String) input.get("lastName")
        );
    }

    @MutationMapping
    public boolean changePassword(@Argument Map<String, Object> input) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.changePassword(
            username,
            (String) input.get("currentPassword"),
            (String) input.get("newPassword")
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public boolean resetUserPassword(@Argument Integer id, @Argument String newPassword) {
        return userService.resetUserPassword(id, newPassword);
    }
}
