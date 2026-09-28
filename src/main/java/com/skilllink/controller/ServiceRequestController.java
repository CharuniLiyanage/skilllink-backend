package com.skilllink.controller;

import com.skilllink.entity.Notification;
import com.skilllink.entity.Service;
import com.skilllink.entity.ServiceRequest;
import com.skilllink.entity.User;
import com.skilllink.repository.NotificationRepository;
import com.skilllink.repository.ServiceRepository;
import com.skilllink.repository.ServiceRequestRepository;
import com.skilllink.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/service-requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationRepository notificationRepository;

    public ServiceRequestController(
            ServiceRequestRepository requestRepository,
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            NotificationRepository notificationRepository
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.notificationRepository = notificationRepository;
    }

    // ==================== CREATE SERVICE REQUEST ====================

    @PostMapping("/create")
    public ResponseEntity<?> createRequest(
            @RequestParam String customerEmail,
            @RequestParam String providerEmail,
            @RequestParam Long serviceId,
            @RequestParam String requestedDate,
            @RequestParam String requestedTime,
            @RequestParam String address,
            @RequestParam String description
    ) {

        Optional<User> customer =
                userRepository.findByEmail(customerEmail);

        if (customer.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        Optional<User> provider =
                userRepository.findByEmail(providerEmail);

        if (provider.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Provider not found");
        }

        Optional<Service> service =
                serviceRepository.findById(serviceId);

        if (service.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Service not found");
        }

        ServiceRequest request =
                new ServiceRequest();

        request.setCustomer(customer.get());
        request.setProvider(provider.get());
        request.setService(service.get());

        request.setRequestedDate(
                LocalDate.parse(requestedDate)
        );

        request.setRequestedTime(
                LocalTime.parse(requestedTime)
        );

        request.setAddress(address);
        request.setDescription(description);

        request.setStatus("PENDING");

        ServiceRequest savedRequest =
                requestRepository.save(request);

        // ==================== PROVIDER NOTIFICATION ====================

        Notification notification =
                new Notification();

        notification.setUser(provider.get());

        notification.setTitle(
                "New Service Request"
        );

        notification.setMessage(
                customer.get().getName()
                        + " sent you a service request for "
                        + service.get().getName()
        );

        notification.setRead(false);

        notification.setCreatedAt(
                LocalDateTime.now()
        );

        notificationRepository.save(notification);

        return ResponseEntity.ok(savedRequest);
    }

    // ==================== GET CUSTOMER REQUESTS ====================

    @GetMapping("/customer")
    public ResponseEntity<?> getCustomerRequests(
            @RequestParam String email
    ) {

        Optional<User> customer =
                userRepository.findByEmail(email);

        if (customer.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        return ResponseEntity.ok(
                requestRepository.findByCustomerEmail(email)
        );
    }

    // ==================== GET PROVIDER REQUESTS ====================

    @GetMapping("/provider")
    public ResponseEntity<?> getProviderRequests(
            @RequestParam String email
    ) {

        Optional<User> provider =
                userRepository.findByEmail(email);

        if (provider.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Provider not found");
        }

        return ResponseEntity.ok(
                requestRepository.findByProviderEmail(email)
        );
    }

    // ==================== UPDATE REQUEST STATUS ====================

    @PutMapping("/update-status/{id}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam String status
    ) {

        Optional<ServiceRequest> optionalRequest =
                requestRepository.findById(id);

        if (optionalRequest.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Service request not found");
        }

        Optional<User> optionalProvider =
                userRepository.findByEmail(email);

        if (optionalProvider.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Provider not found");
        }

        ServiceRequest request =
                optionalRequest.get();

        User provider =
                optionalProvider.get();

        // ==================== SECURITY CHECK ====================

        if (request.getProvider() == null ||
                !request.getProvider()
                        .getId()
                        .equals(provider.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(
                            "You can only update your own requests"
                    );
        }

        // ==================== VALID STATUS CHECK ====================

        if (!status.equals("ACCEPTED") &&
                !status.equals("REJECTED") &&
                !status.equals("COMPLETED")) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid status");
        }

        // ==================== UPDATE STATUS ====================

        request.setStatus(status);

        ServiceRequest updatedRequest =
                requestRepository.save(request);

        // ==================== CUSTOMER NOTIFICATION ====================

        Notification notification =
                new Notification();

        notification.setUser(
                request.getCustomer()
        );

        if ("ACCEPTED".equals(status)) {

            notification.setTitle(
                    "Request Accepted"
            );

            notification.setMessage(
                    "Your service request has been accepted by "
                            + provider.getName()
            );

        } else if ("REJECTED".equals(status)) {

            notification.setTitle(
                    "Request Rejected"
            );

            notification.setMessage(
                    "Your service request has been rejected by "
                            + provider.getName()
            );

        } else if ("COMPLETED".equals(status)) {

            notification.setTitle(
                    "Service Completed"
            );

            notification.setMessage(
                    "Your service request has been marked as completed by "
                            + provider.getName()
            );
        }

        notification.setRead(false);

        notification.setCreatedAt(
                LocalDateTime.now()
        );

        notificationRepository.save(
                notification
        );

        return ResponseEntity.ok(
                updatedRequest
        );
    }

    // ==================== PROVIDER BOOKINGS ====================

    @GetMapping("/provider/bookings")
    public ResponseEntity<?> getProviderBookings(
            @RequestParam String email
    ) {

        Optional<User> provider =
                userRepository.findByEmail(email);

        if (provider.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Provider not found");
        }

        List<ServiceRequest> bookings =
                requestRepository
                        .findByProviderEmail(email)
                        .stream()
                        .filter(request ->
                                "ACCEPTED"
                                        .equals(request.getStatus())
                                        ||
                                        "COMPLETED"
                                                .equals(request.getStatus())
                        )
                        .toList();

        return ResponseEntity.ok(
                bookings
        );
    }
}