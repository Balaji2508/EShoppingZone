package com.eshoppingzone.product.config;

import com.eshoppingzone.product.entity.Product;
import com.eshoppingzone.product.entity.ProductReview;
import com.eshoppingzone.product.repository.ProductRepository;
import com.eshoppingzone.product.repository.ProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    public DataSeeder(ProductRepository productRepository, ProductReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            logger.info("Seeding initial products into product database...");

            Product p1 = new Product(null, "iPhone 15 Pro Max", "Apple", "Apple iPhone 15 Pro Max 256GB Natural Titanium with A17 Pro Chip", 140000.0, "Electronics", 15, "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=400", 2L, "Alice Merchant");
            p1.setOfferPercentage(10.0);
            p1.setOfferActive(true);

            Product p2 = new Product(null, "Apple MacBook Air M2", "Apple", "13.6-inch Liquid Retina Display, 8GB Unified Memory, 256GB SSD Storage", 99900.0, "Electronics", 10, "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400", 2L, "Alice Merchant");
            p2.setOfferPercentage(15.0);
            p2.setOfferActive(true);

            Product p3 = new Product(null, "Sony WH-1000XM5 Headphones", "Sony", "Industry Leading Wireless Noise Cancelling Over-Ear Headphones with Mic", 29990.0, "Electronics", 20, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", 2L, "Alice Merchant");
            p3.setOfferPercentage(20.0);
            p3.setOfferActive(true);

            Product p4 = new Product(null, "Java Programming Masterclass", "Oracle Press", "Complete Masterclass for Java 17, Spring Boot 3 Microservices and Cloud", 899.0, "Books", 50, "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=400", 2L, "Alice Merchant");
            p4.setOfferPercentage(5.0);
            p4.setOfferActive(true);

            Product p5 = new Product(null, "Clean Code by Robert C. Martin", "Prentice Hall", "A Handbook of Agile Software Craftsmanship - Robert C. Martin Series", 650.0, "Books", 35, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400", 2L, "Alice Merchant");
            p5.setOfferPercentage(10.0);
            p5.setOfferActive(true);

            Product p6 = new Product(null, "Cotton Casual Crew T-Shirt", "Levi's", "100% Pure Organic Cotton Premium Regular Fit T-Shirt", 799.0, "Apparel", 100, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=400", 2L, "Alice Merchant");
            p6.setOfferPercentage(25.0);
            p6.setOfferActive(true);

            Product p7 = new Product(null, "Classic Denim Jeans", "Wrangler", "Comfort Stretch Heavy Duty Regular Fit Denim Jeans", 1899.0, "Apparel", 60, "https://images.unsplash.com/photo-1542272604-780c96856592?w=400", 2L, "Alice Merchant");
            p7.setOfferPercentage(15.0);
            p7.setOfferActive(true);

            Product p8 = new Product(null, "Organic Neem Face Wash", "Himalaya", "Purifying Deep Cleansing Herbal Face Wash 150ml with Active Neem", 299.0, "Personal Care", 80, "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400", 2L, "Alice Merchant");
            p8.setOfferPercentage(10.0);
            p8.setOfferActive(true);

            Product p9 = new Product(null, "Aloe Vera Soothing Gel", "Forest Essentials", "100% Natural Organic Hydrating & Moisturizing Gel 200g", 450.0, "Personal Care", 75, "https://images.unsplash.com/photo-1576426863848-c21f53c60b19?w=400", 2L, "Alice Merchant");

            List<Product> savedProducts = productRepository.saveAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8, p9));
            logger.info("Sample products seeded successfully: {} products added", savedProducts.size());

            // Seed initial reviews
            if (reviewRepository.count() == 0 && !savedProducts.isEmpty()) {
                Product firstProduct = savedProducts.get(0);
                reviewRepository.save(new ProductReview(null, firstProduct.getId(), 1L, "John Customer", 5, "Outstanding flagship phone! Blazing fast A17 Pro performance and supreme camera quality."));
                reviewRepository.save(new ProductReview(null, firstProduct.getId(), 2L, "Alice Merchant", 5, "Top selling premium product. Highly recommended by customers!"));

                if (savedProducts.size() > 1) {
                    Product secondProduct = savedProducts.get(1);
                    reviewRepository.save(new ProductReview(null, secondProduct.getId(), 1L, "John Customer", 5, "Best laptop for developers and university work. Battery lasts all day!"));
                }
            }
        }
    }
}
