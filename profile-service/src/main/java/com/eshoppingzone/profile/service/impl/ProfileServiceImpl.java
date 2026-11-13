package com.eshoppingzone.profile.service.impl;

import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.entity.UserProfile;
import com.eshoppingzone.profile.exception.BadRequestException;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.repository.UserProfileRepository;
import com.eshoppingzone.profile.security.JwtUtil;
import com.eshoppingzone.profile.service.AuditService;
import com.eshoppingzone.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public ProfileServiceImpl(UserProfileRepository userProfileRepository,
                              PasswordEncoder passwordEncoder,
                              JwtUtil jwtUtil,
                              AuditService auditService) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registering user with email: {}", request.getEmail());
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        UserProfile user = new UserProfile();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole().toUpperCase() : "CUSTOMER");

        UserProfile saved = userProfileRepository.save(user);
        logger.info("User registered successfully with ID: {}", saved.getId());

        auditService.recordAuditLog(
                "USER_REGISTERED",
                saved.getEmail(),
                "USER",
                String.valueOf(saved.getId()),
                "New user registered with role " + saved.getRole()
        );

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole());
        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), "User registered successfully");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());
        UserProfile user = userProfileRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Password mismatch for email: {}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        }

        auditService.recordAuditLog(
                "USER_LOGIN",
                user.getEmail(),
                "AUTH",
                String.valueOf(user.getId()),
                "User successfully logged in as " + user.getRole()
        );

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        logger.info("User logged in successfully: {}", user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), "Login successful");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long id) {
        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with ID: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByEmail(String email) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with email: " + email));
        return mapToResponse(user);
    }

    @Override
    public ProfileResponse updateProfile(Long id, UpdateProfileRequest request) {
        logger.info("Updating profile for user ID: {}", id);
        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().toUpperCase());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UserProfile updated = userProfileRepository.save(user);
        logger.info("Profile updated successfully for user ID: {}", id);

        auditService.recordAuditLog(
                "USER_UPDATED",
                "ADMIN",
                "USER",
                String.valueOf(id),
                "Updated details for user: " + updated.getEmail() + ", role=" + updated.getRole()
        );

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> getAllProfiles(String role) {
        logger.info("Fetching all profiles, filter role: {}", role);
        List<UserProfile> list;
        if (role != null && !role.isBlank()) {
            list = userProfileRepository.findAll().stream()
                    .filter(u -> role.equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList());
        } else {
            list = userProfileRepository.findAll();
        }
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ProfileResponse createProfileByAdmin(RegisterRequest request) {
        logger.info("Admin creating user with email: {}, role: {}", request.getEmail(), request.getRole());
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        UserProfile user = new UserProfile();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole().toUpperCase() : "CUSTOMER");

        UserProfile saved = userProfileRepository.save(user);

        auditService.recordAuditLog(
                "USER_CREATED_BY_ADMIN",
                "ADMIN",
                "USER",
                String.valueOf(saved.getId()),
                "Admin created " + saved.getRole() + ": " + saved.getEmail()
        );

        return mapToResponse(saved);
    }

    @Override
    public void deleteProfile(Long id) {
        logger.info("Admin deleting profile for user ID: {}", id);
        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with ID: " + id));

        String email = user.getEmail();
        String role = user.getRole();
        userProfileRepository.delete(user);

        auditService.recordAuditLog(
                "USER_DELETED",
                "ADMIN",
                "USER",
                String.valueOf(id),
                "Admin deleted " + role + " user: " + email
        );
        logger.info("Profile deleted successfully for user ID: {}", id);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        logger.info("Changing password for user ID: {}", userId);
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            logger.warn("Password mismatch when changing password for user ID: {}", userId);
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userProfileRepository.save(user);

        auditService.recordAuditLog(
                "PASSWORD_CHANGED",
                user.getEmail(),
                "USER",
                String.valueOf(userId),
                "User successfully changed account password"
        );
        logger.info("Password changed successfully for user ID: {}", userId);
    }

    private ProfileResponse mapToResponse(UserProfile user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
