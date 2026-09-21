package com.skilllink.controller;

import com.skilllink.entity.ProviderProfile;
import com.skilllink.entity.User;
import com.skilllink.enums.Role;
import com.skilllink.repository.ProviderProfileRepository;
import com.skilllink.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/provider-profile")
@CrossOrigin(origins = "*")
public class ProviderProfileController {

    private final ProviderProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProviderProfileController(
            ProviderProfileRepository profileRepository,
            UserRepository userRepository
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    // ================= SAVE / UPDATE PROFILE =================

    @PostMapping("/save")
    public ResponseEntity<?> saveProfile(
            @RequestParam String email,
            @RequestBody ProviderProfile profile
    ) {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        User user = optionalUser.get();

        // Check whether user is a provider
        boolean isProvider = user.getRoles()
                .stream()
                .anyMatch(
                        userRole ->
                                userRole.getRole() == Role.PROVIDER
                );

        if (!isProvider) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("User is not a service provider");
        }

        // Check existing profile
        Optional<ProviderProfile> existingProfile =
                profileRepository.findByUserEmail(email);

        ProviderProfile providerProfile;

        if (existingProfile.isPresent()) {
            providerProfile = existingProfile.get();
        } else {
            providerProfile = new ProviderProfile();
            providerProfile.setUser(user);
        }

        providerProfile.setLocation(
                profile.getLocation()
        );

        providerProfile.setExperience(
                profile.getExperience()
        );

        providerProfile.setDescription(
                profile.getDescription()
        );

        ProviderProfile savedProfile =
                profileRepository.save(providerProfile);

        return ResponseEntity.ok(savedProfile);
    }

    // ================= GET PROFILE =================

    @GetMapping("/{email}")
    public ResponseEntity<?> getProfile(
            @PathVariable String email
    ) {

        Optional<ProviderProfile> profile =
                profileRepository.findByUserEmail(email);

        if (profile.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Provider profile not found");
        }

        return ResponseEntity.ok(profile.get());
    }
}