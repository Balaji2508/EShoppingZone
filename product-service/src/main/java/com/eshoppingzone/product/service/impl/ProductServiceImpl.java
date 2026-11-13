package com.eshoppingzone.product.service.impl;

import com.eshoppingzone.product.dto.OfferRequest;
import com.eshoppingzone.product.dto.ProductRequest;
import com.eshoppingzone.product.dto.ProductResponse;
import com.eshoppingzone.product.dto.ReviewResponse;
import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.entity.ProductReview;
import com.eshoppingzone.product.exception.BadRequestException;
import com.eshoppingzone.product.exception.ResourceNotFoundException;
import com.eshoppingzone.product.repository.ProductRepository;
import com.eshoppingzone.product.repository.ProductReviewRepository;
import com.eshoppingzone.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        logger.info("Fetching all products");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        logger.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String name) {
        logger.info("Searching products with keyword: {}", name);
        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        logger.info("Fetching products in category: {}", category);
        return productRepository.findByCategoryIgnoreCase(category.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByMerchantId(Long merchantId) {
        logger.info("Fetching products for merchant ID: {}", merchantId);
        return productRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveOffers() {
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getOfferActive()) && p.getOfferPercentage() != null && p.getOfferPercentage() > 0)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        logger.info("Creating new product: {}", request.getName());
        Product product = new Product();
        product.setName(request.getName());
        product.setBrand(request.getBrand() != null && !request.getBrand().isBlank() ? request.getBrand() : "Generic");
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setMerchantId(request.getMerchantId() != null ? request.getMerchantId() : 2L);
        product.setMerchantName(request.getMerchantName() != null && !request.getMerchantName().isBlank() ? request.getMerchantName() : "Alice Merchant");
        product.setOfferPercentage(request.getOfferPercentage() != null ? request.getOfferPercentage() : 0.0);
        product.setOfferActive(request.getOfferActive() != null ? request.getOfferActive() : false);
        product.setOfferStartDate(request.getOfferStartDate());
        product.setOfferEndDate(request.getOfferEndDate());

        Product saved = productRepository.save(product);
        logger.info("Product created with ID: {} for Merchant: {}", saved.getId(), saved.getMerchantName());
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        logger.info("Updating product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setName(request.getName());
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getMerchantId() != null) {
            product.setMerchantId(request.getMerchantId());
        }
        if (request.getMerchantName() != null && !request.getMerchantName().isBlank()) {
            product.setMerchantName(request.getMerchantName());
        }
        if (request.getOfferPercentage() != null) {
            product.setOfferPercentage(request.getOfferPercentage());
        }
        if (request.getOfferActive() != null) {
            product.setOfferActive(request.getOfferActive());
        }
        if (request.getOfferStartDate() != null) {
            product.setOfferStartDate(request.getOfferStartDate());
        }
        if (request.getOfferEndDate() != null) {
            product.setOfferEndDate(request.getOfferEndDate());
        }

        Product updated = productRepository.save(product);
        logger.info("Product updated successfully: ID {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public ProductResponse setOffer(Long id, Long merchantId, OfferRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (merchantId != null && !merchantId.equals(product.getMerchantId())) {
            throw new BadRequestException("Only the merchant who owns this product can modify its offers");
        }

        product.setOfferPercentage(request.getOfferPercentage());
        product.setOfferActive(request.getOfferActive() != null ? request.getOfferActive() : true);
        product.setOfferStartDate(request.getOfferStartDate());
        product.setOfferEndDate(request.getOfferEndDate());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse removeOffer(Long id, Long merchantId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (merchantId != null && !merchantId.equals(product.getMerchantId())) {
            throw new BadRequestException("Only the merchant who owns this product can modify its offers");
        }

        product.setOfferPercentage(0.0);
        product.setOfferActive(false);
        product.setOfferStartDate(null);
        product.setOfferEndDate(null);

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        reviewRepository.deleteByProductId(id);
        productRepository.delete(product);
        logger.info("Product and associated reviews deleted successfully: ID {}", id);
    }

    @Override
    public ProductResponse reduceStock(Long id, int quantity) {
        logger.info("Reducing stock for product ID: {} by quantity: {}", id, quantity);
        if (quantity <= 0) {
            throw new BadRequestException("Quantity to reduce must be greater than zero");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (product.getStockQuantity() < quantity) {
            logger.warn("Insufficient stock for product ID: {}. Available: {}, Requested: {}",
                    id, product.getStockQuantity(), quantity);
            throw new BadRequestException("Insufficient product stock for: " + product.getName()
                    + " (Available: " + product.getStockQuantity() + ", Requested: " + quantity + ")");
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product saved = productRepository.save(product);
        logger.info("Stock updated for product ID: {}. New Stock: {}", id, saved.getStockQuantity());
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse restoreStock(Long id, int quantity) {
        logger.info("Restoring stock for product ID: {} by quantity: {}", id, quantity);
        if (quantity <= 0) {
            throw new BadRequestException("Quantity to restore must be greater than zero");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        Product saved = productRepository.save(product);
        logger.info("Stock restored for product ID: {}. New Stock: {}", id, saved.getStockQuantity());
        return mapToResponse(saved);
    }

    private ProductResponse mapToResponse(Product product) {
        double offerPct = (product.getOfferPercentage() != null && Boolean.TRUE.equals(product.getOfferActive())) ? product.getOfferPercentage() : 0.0;
        double discountAmt = (product.getPrice() * offerPct) / 100.0;
        double finalPrice = Math.max(0.0, product.getPrice() - discountAmt);

        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        double avgRating = reviews.isEmpty() ? 4.5 : reviews.stream().mapToInt(ProductReview::getRating).average().orElse(4.5);
        // Round avgRating to 1 decimal place
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        List<ReviewResponse> reviewDtos = reviews.stream()
                .map(r -> new ReviewResponse(r.getId(), r.getProductId(), r.getUserId(), r.getUserName(), r.getRating(), r.getComment(), r.getCreatedAt(), r.getUpdatedAt()))
                .collect(Collectors.toList());

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand() != null ? product.getBrand() : "Generic",
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getMerchantId() != null ? product.getMerchantId() : 2L,
                product.getMerchantName() != null ? product.getMerchantName() : "Alice Merchant",
                offerPct,
                Boolean.TRUE.equals(product.getOfferActive()),
                discountAmt,
                finalPrice,
                avgRating,
                reviews.size(),
                reviewDtos
        );
    }
}
