package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.model.dto.UserDto;
import bg.spacbg.sp_ac_bg.model.dto.UserGroupDto;
import bg.spacbg.sp_ac_bg.model.dto.CurrentUserDto;
import bg.spacbg.sp_ac_bg.model.entity.UserEntity;
import bg.spacbg.sp_ac_bg.model.entity.UserGroupEntity;
import bg.spacbg.sp_ac_bg.repository.UserRepository;
import bg.spacbg.sp_ac_bg.repository.UserGroupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                          UserGroupRepository userGroupRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Integer id) {
        return userRepository.findById(id)
                .map(this::toUserDto)
                .orElse(null);
    }

    @Override
    public List<UserGroupDto> getAllGroups() {
        return userGroupRepository.findAll().stream()
                .map(this::toGroupDto)
                .collect(Collectors.toList());
    }

    @Override
    public CurrentUserDto getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .map(this::toCurrentUserDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public UserDto createUser(String username, String email, String password,
                              String firstName, String lastName, Integer groupId, Boolean isActive) {
        // Check if username or email already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Потребител с това потребителско име вече съществува");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Потребител с този email вече съществува");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(isActive != null ? isActive : true);

        if (groupId != null) {
            userGroupRepository.findById(groupId).ifPresent(user::setGroup);
        }

        // Set default periods
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withMonth(12).withDayOfMonth(31);

        user.setDocumentPeriodStart(startOfYear);
        user.setDocumentPeriodEnd(endOfYear);
        user.setAccountingPeriodStart(startOfYear);
        user.setAccountingPeriodEnd(endOfYear);
        user.setVatPeriodStart(startOfYear);
        user.setVatPeriodEnd(endOfYear);

        return toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto updateUser(Integer id, String username, String email,
                              String firstName, String lastName, Integer groupId, Boolean isActive) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Потребителят не е намерен"));

        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Потребител с това потребителско име вече съществува");
            }
            user.setUsername(username);
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("Потребител с този email вече съществува");
            }
            user.setEmail(email);
        }

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (isActive != null) user.setActive(isActive);

        if (groupId != null) {
            userGroupRepository.findById(groupId).ifPresent(user::setGroup);
        }

        return toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public boolean deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Потребителят не е намерен");
        }
        userRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    public CurrentUserDto updateProfile(String username, String newEmail, String firstName, String lastName) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Потребителят не е намерен"));

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new RuntimeException("Потребител с този email вече съществува");
            }
            user.setEmail(newEmail);
        }

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);

        return toCurrentUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Потребителят не е намерен"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Текущата парола е грешна");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean resetUserPassword(Integer id, String newPassword) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Потребителят не е намерен"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    private UserDto toUserDto(UserEntity entity) {
        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setActive(entity.isActive());

        if (entity.getGroup() != null) {
            dto.setGroup(toGroupDto(entity.getGroup()));
        }

        return dto;
    }

    private CurrentUserDto toCurrentUserDto(UserEntity entity) {
        CurrentUserDto dto = new CurrentUserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());

        if (entity.getGroup() != null) {
            dto.setGroup(toGroupDto(entity.getGroup()));
        }

        return dto;
    }

    private UserGroupDto toGroupDto(UserGroupEntity entity) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
