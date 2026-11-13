package com.eshoppingzone.product.service;

import com.eshoppingzone.product.dto.OfferRequest;
import com.eshoppingzone.product.dto.ProductRequest;
import com.eshoppingzone.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    List<ProductResponse> searchProducts(String name);
    List<ProductResponse> getProductsByCategory(String category);
    List<ProductResponse> getProductsByMerchantId(Long merchantId);
    List<ProductResponse> getActiveOffers();
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse setOffer(Long id, Long merchantId, OfferRequest request);
    ProductResponse removeOffer(Long id, Long merchantId);
    void deleteProduct(Long id);
    ProductResponse reduceStock(Long id, int quantity);
    ProductResponse restoreStock(Long id, int quantity);
}
