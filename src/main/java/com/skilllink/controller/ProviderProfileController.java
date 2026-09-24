package com.skilllink.controller;

import com.skilllink.entity.ProviderProfile;
import com.skilllink.entity.User;
import com.skilllink.enums.Role;
import com.skilllink.repository.ProviderProfileRepository;
import com.skilllink.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

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

        // ================= UPDATE USER DATA =================

        if (profile.getUser() != null) {

            if (profile.getUser().getName() != null &&
                    !profile.getUser().getName().trim().isEmpty()) {

                user.setName(
                        profile.getUser().getName().trim()
                );
            }

            if (profile.getUser().getPhone() != null &&
                    !profile.getUser().getPhone().trim().isEmpty()) {

                user.setPhone(
                        profile.getUser().getPhone().trim()
                );
            }

            userRepository.save(user);
        }

        // Save profile image path if provided
        if (profile.getProfileImage() != null &&
                !profile.getProfileImage().isEmpty()) {

            providerProfile.setProfileImage(
                    profile.getProfileImage()
            );
        }

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

    // ================= UPLOAD PROFILE IMAGE =================

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam String email,
            @RequestParam("image") MultipartFile image
    ) {

        try {

            // Check user
            Optional<User> optionalUser =
                    userRepository.findByEmail(email);

            if (optionalUser.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }

            User user = optionalUser.get();

            // Check provider role
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

            // Check image
            if (image == null || image.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Please select an image");
            }

            // Create upload folder
            Path uploadPath = Paths.get(
                    "uploads/profile-images"
            );

            Files.createDirectories(uploadPath);

            // Get file extension
            String originalName =
                    image.getOriginalFilename();

            String extension = "";

            if (originalName != null &&
                    originalName.contains(".")) {

                extension =
                        originalName.substring(
                                originalName.lastIndexOf(".")
                        );
            }

            // Create unique file name
            String fileName =
                    "profile_" +
                            UUID.randomUUID() +
                            extension;

            Path filePath =
                    uploadPath.resolve(fileName);

            // Save image
            Files.write(
                    filePath,
                    image.getBytes()
            );

            // Find provider profile
            Optional<ProviderProfile> existingProfile =
                    profileRepository.findByUserEmail(email);

            ProviderProfile providerProfile;

            if (existingProfile.isPresent()) {

                providerProfile =
                        existingProfile.get();

            } else {

                providerProfile =
                        new ProviderProfile();

                providerProfile.setUser(user);
            }

            // Save image path in database
            String imagePath =
                    "/uploads/profile-images/" +
                            fileName;

            providerProfile.setProfileImage(
                    imagePath
            );

            profileRepository.save(
                    providerProfile
            );

            return ResponseEntity.ok(
                    imagePath
            );

        } catch (IOException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            "Failed to upload image: " +
                                    e.getMessage()
                    );
        }
    }
}