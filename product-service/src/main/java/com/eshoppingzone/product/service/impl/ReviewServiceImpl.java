package com.eshoppingzone.product.service.impl;

import com.eshoppingzone.product.dto.ReviewRequest;
import com.eshoppingzone.product.dto.ReviewResponse;
import com.eshoppingzone.product.entity.ProductReview;
import com.eshoppingzone.product.exception.BadRequestException;
import com.eshoppingzone.product.exception.ResourceNotFoundException;
import com.eshoppingzone.product.repository.ProductRepository;
import com.eshoppingzone.product.repository.ProductReviewRepository;
import com.eshoppingzone.product.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewServiceImpl(ProductReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Override
    public ReviewResponse addReview(ReviewRequest request) {
        if (!productRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException("Product not found with ID: " + request.getProductId());
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars");
        }

        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new BadRequestException("Review comment cannot be empty");
        }

        // If review already exists for this user and product, update it instead of creating duplicate
        ProductReview review = reviewRepository.findByProductIdAndUserId(request.getProductId(), request.getUserId())
                .orElse(new ProductReview());

        review.setProductId(request.getProductId());
        review.setUserId(request.getUserId());
        review.setUserName(request.getUserName() != null ? request.getUserName() : "Verified Customer");
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());

        ProductReview saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Only the review owner can modify this review");
        }

        if (request.getRating() != null) {
            if (request.getRating() < 1 || request.getRating() > 5) {
                throw new BadRequestException("Rating must be between 1 and 5 stars");
            }
            review.setRating(request.getRating());
        }

        if (request.getComment() != null && !request.getComment().trim().isEmpty()) {
            review.setComment(request.getComment().trim());
        }

        review.setUpdatedAt(LocalDateTime.now());
        ProductReview saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId, String userRole) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isOwner = userId != null && review.getUserId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new BadRequestException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponse(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getUserName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
