package com.eshoppingzone.product.service;

import com.eshoppingzone.product.dto.ReviewRequest;
import com.eshoppingzone.product.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse addReview(ReviewRequest request);
    List<ReviewResponse> getReviewsByProductId(Long productId);
    List<ReviewResponse> getReviewsByUserId(Long userId);
    List<ReviewResponse> getAllReviews();
    ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request);
    void deleteReview(Long reviewId, Long userId, String userRole);
}
