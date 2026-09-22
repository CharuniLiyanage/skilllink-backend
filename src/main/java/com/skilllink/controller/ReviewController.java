package com.skilllink.controller;

import com.skilllink.entity.Review;
import com.skilllink.entity.ServiceRequest;
import com.skilllink.entity.User;
import com.skilllink.repository.ReviewRepository;
import com.skilllink.repository.ServiceRequestRepository;
import com.skilllink.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addReview(
            @RequestParam String customerEmail,
            @RequestParam Long serviceRequestId,
            @RequestParam Integer rating,
            @RequestParam String comment
    ) {

        Optional<User> optionalCustomer =
                userRepository.findByEmail(customerEmail);

        if (optionalCustomer.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Customer not found");
        }

        Optional<ServiceRequest> optionalRequest =
                serviceRequestRepository.findById(
                        serviceRequestId
                );

        if (optionalRequest.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Service request not found");
        }

        ServiceRequest request =
                optionalRequest.get();

        if (!request.getCustomer()
                .getId()
                .equals(optionalCustomer.get().getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only review your own booking");
        }

        if (!"COMPLETED".equals(request.getStatus())) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            "You can only review completed bookings"
                    );
        }

        if (rating < 1 || rating > 5) {
            return ResponseEntity
                    .badRequest()
                    .body("Rating must be between 1 and 5");
        }

        Optional<Review> existingReview =
                reviewRepository.findByServiceRequestId(
                        serviceRequestId
                );

        if (existingReview.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("This booking has already been reviewed");
        }

        Review review = new Review();

        review.setRating(rating);
        review.setComment(comment);
        review.setCustomer(optionalCustomer.get());
        review.setProvider(request.getProvider());
        review.setServiceRequest(request);

        Review savedReview =
                reviewRepository.save(review);

        return ResponseEntity.ok(savedReview);
    }
}