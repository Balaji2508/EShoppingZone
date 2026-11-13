package com.eshoppingzone.profile.config;

import com.eshoppingzone.profile.entity.UserProfile;
import com.eshoppingzone.profile.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userProfileRepository.existsByEmail("customer@gmail.com")) {
            UserProfile customer = new UserProfile();
            customer.setName("John Customer");
            customer.setEmail("customer@gmail.com");
            customer.setPassword(passwordEncoder.encode("customer123"));
            customer.setPhone("9876543210");
            customer.setAddress("123 Market Street, Tech City, Karnataka 560001");
            customer.setRole("CUSTOMER");
            userProfileRepository.save(customer);
            logger.info("Customer user seeded: customer@gmail.com");
        }

        if (!userProfileRepository.existsByEmail("merchant@gmail.com")) {
            UserProfile merchant = new UserProfile();
            merchant.setName("Alice Merchant");
            merchant.setEmail("merchant@gmail.com");
            merchant.setPassword(passwordEncoder.encode("merchant123"));
            merchant.setPhone("9123456780");
            merchant.setAddress("456 Wholesale Avenue, Trade Center, Maharashtra 400001");
            merchant.setRole("MERCHANT");
            userProfileRepository.save(merchant);
            logger.info("Merchant user seeded: merchant@gmail.com");
        }

        if (!userProfileRepository.existsByEmail("delivery@gmail.com")) {
            UserProfile delivery = new UserProfile();
            delivery.setName("David Delivery");
            delivery.setEmail("delivery@gmail.com");
            delivery.setPassword(passwordEncoder.encode("delivery123"));
            delivery.setPhone("9870001122");
            delivery.setAddress("Express Delivery Hub 4, Indiranagar, Bangalore 560038");
            delivery.setRole("DELIVERY_MAN");
            userProfileRepository.save(delivery);
            logger.info("Delivery user seeded: delivery@gmail.com");
        }

        if (!userProfileRepository.existsByEmail("admin@gmail.com")) {
            UserProfile admin = new UserProfile();
            admin.setName("Super Admin");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("9000000000");
            admin.setAddress("Headquarters, EShoppingZone HQ, Suite 100");
            admin.setRole("ADMIN");
            userProfileRepository.save(admin);
            logger.info("Admin user seeded: admin@gmail.com");
        }
    }
}
