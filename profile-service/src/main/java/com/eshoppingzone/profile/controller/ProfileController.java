package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.service.AuditService;
import com.eshoppingzone.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final AuditService auditService;

    public ProfileController(ProfileService profileService, AuditService auditService) {
        this.profileService = profileService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = profileService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = profileService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAllProfiles(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(profileService.getAllProfiles(role));
    }

    @GetMapping("/delivery-partners")
    public ResponseEntity<List<ProfileResponse>> getDeliveryPartners() {
        return ResponseEntity.ok(profileService.getAllProfiles("DELIVERY_MAN"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {
        ProfileResponse response = profileService.getProfileById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ProfileResponse> getProfileByEmail(@PathVariable String email) {
        ProfileResponse response = profileService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/create")
    public ResponseEntity<ProfileResponse> createProfileByAdmin(@Valid @RequestBody RegisterRequest request) {
        ProfileResponse response = profileService.createProfileByAdmin(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> updateProfile(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProfile(@PathVariable Long id) {
        profileService.deleteProfile(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully", "userId", String.valueOf(id)));
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully", "userId", String.valueOf(id)));
    }

    // --- Audit Log Endpoints ---

    @GetMapping("/audit")
    public ResponseEntity<List<AuditResponse>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }

    @PostMapping("/audit")
    public ResponseEntity<AuditResponse> createAuditLog(@RequestBody AuditRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.recordAuditLog(request));
    }
}
