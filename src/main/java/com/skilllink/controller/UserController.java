package com.skilllink.controller;

import com.skilllink.entity.User;
import com.skilllink.entity.UserRole;
import com.skilllink.enums.Role;
import com.skilllink.repository.UserRepository;
import com.skilllink.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ================= REGISTER =================

    @PostMapping("/register")
    public User register(@RequestBody User user) {

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        Role requestedRole = user.getRole();

        if (requestedRole == null) {
            requestedRole = Role.CUSTOMER;
        }

        User savedUser = userRepository.save(user);

        UserRole userRole =
                new UserRole(savedUser, requestedRole);

        savedUser.addRole(userRole);

        return userRepository.save(savedUser);
    }

    // ================= LOGIN =================

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        Optional<User> existingUser =
                userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent() &&
                passwordEncoder.matches(
                        user.getPassword(),
                        existingUser.get().getPassword()
                )) {

            String token = jwtService.generateToken(
                    existingUser.get()
            );

            List<String> roles = existingUser.get()
                    .getRoles()
                    .stream()
                    .map(UserRole::getRole)
                    .map(Enum::name)
                    .toList();

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "token", token,
                            "roles", roles
                    )
            );
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Invalid email or password");
    }

    // ================= ADD ROLE =================

    @PostMapping("/add-role")
    public ResponseEntity<?> addRole(
            @RequestParam String email,
            @RequestParam Role role
    ) {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        User user = optionalUser.get();

        boolean alreadyHasRole = user.getRoles()
                .stream()
                .anyMatch(
                        userRole ->
                                userRole.getRole() == role
                );

        if (alreadyHasRole) {
            return ResponseEntity
                    .badRequest()
                    .body("User already has this role");
        }

        UserRole newUserRole =
                new UserRole(user, role);

        user.addRole(newUserRole);

        userRepository.save(user);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message", "Role added successfully",
                        "email", user.getEmail(),
                        "role", role.name()
                )
        );
    }

    // ================= GET ALL USERS =================

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/providers")
    public ResponseEntity<?> getAllProviders() {

        List<User> providers = userRepository.findAll()
                .stream()
                .filter(user -> user.getRoles()
                        .stream()
                        .anyMatch(userRole ->
                                userRole.getRole() == Role.PROVIDER
                        )
                )
                .toList();

        return ResponseEntity.ok(providers);
    }

    // ================= GET USER BY EMAIL =================

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(
            @RequestParam String email
    ) {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        User user = optionalUser.get();

        return ResponseEntity.ok(
                java.util.Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "phone", user.getPhone(),
                        "profileImage", user.getProfileImage() == null
                                ? ""
                                : user.getProfileImage()
                )
        );
    }

// ================= UPDATE USER PROFILE =================

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestParam String email,
            @RequestBody User updatedUser
    ) {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }

        User user = optionalUser.get();

        user.setName(updatedUser.getName());
        user.setPhone(updatedUser.getPhone());

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "id", savedUser.getId(),
                        "name", savedUser.getName(),
                        "email", savedUser.getEmail(),
                        "phone", savedUser.getPhone()
                )
        );
    }
}