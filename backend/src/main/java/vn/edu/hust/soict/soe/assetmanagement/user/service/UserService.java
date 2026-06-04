package vn.edu.hust.soict.soe.assetmanagement.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.user.dto.CreateUserRequest;
import vn.edu.hust.soict.soe.assetmanagement.user.dto.UserDto;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.Role;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.User;
import vn.edu.hust.soict.soe.assetmanagement.user.repository.RoleRepository;
import vn.edu.hust.soict.soe.assetmanagement.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;

/**
 * User service.
 * Implements UserDetailsService for Spring Security integration.
 * Contains business logic for user management (CRUD, role assignment, etc.).
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapperService userMapperService;

    // ── UserDetailsService ──────

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
    }

    // ── Helper Methods ──────
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + username));
        return userMapperService.toDto(user);
    }

    // ── CRUD ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapperService::toDto) 
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id));
        return userMapperService.toDto(user); 
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email đã được sử dụng: " + request.getEmail());
        }

        Role role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò: " + request.getRoleCode()));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .isActive(true)
                .build();

        user.getRoles().add(role);
        return userMapperService.toDto(userRepository.save(user)); 
    }

    @Transactional
    public void deactivateUser(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id));
        user.setActive(false);
        userRepository.save(user);
    }
}