package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.*;
import java.util.List;

public interface ProfileService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    ProfileResponse getProfileById(Long id);
    ProfileResponse getProfileByEmail(String email);
    ProfileResponse updateProfile(Long id, UpdateProfileRequest request);
    List<ProfileResponse> getAllProfiles(String role);
    ProfileResponse createProfileByAdmin(RegisterRequest request);
    void deleteProfile(Long id);
    void changePassword(Long userId, ChangePasswordRequest request);
}
