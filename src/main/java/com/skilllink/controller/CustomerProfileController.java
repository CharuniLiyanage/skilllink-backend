package com.skilllink.controller;

import com.skilllink.entity.User;
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
@RequestMapping("/api/customer-profile")
@CrossOrigin(origins = "*")
public class CustomerProfileController {

    private final UserRepository userRepository;

    public CustomerProfileController(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    // ================= CUSTOMER PROFILE IMAGE UPLOAD =================

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadCustomerProfileImage(
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
                    "customer_profile_" +
                            UUID.randomUUID() +
                            extension;

            Path filePath =
                    uploadPath.resolve(fileName);

            // Save image
            Files.write(
                    filePath,
                    image.getBytes()
            );

            // Save image path in database
            String imagePath =
                    "/uploads/profile-images/" +
                            fileName;

            user.setProfileImage(imagePath);

            userRepository.save(user);

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