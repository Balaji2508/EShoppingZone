package com.eshoppingzone.product.controller;

import com.eshoppingzone.product.dto.OfferRequest;
import com.eshoppingzone.product.dto.ProductRequest;
import com.eshoppingzone.product.dto.ProductResponse;
import com.eshoppingzone.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "keyword", required = false) String keyword) {
        String searchTerm = keyword != null ? keyword : (name != null ? name : "");
        return ResponseEntity.ok(productService.searchProducts(searchTerm));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ProductResponse>> getProductsByMerchantId(@PathVariable Long merchantId) {
        return ResponseEntity.ok(productService.getProductsByMerchantId(merchantId));
    }

    @GetMapping("/offers/active")
    public ResponseEntity<List<ProductResponse>> getActiveOffers() {
        return ResponseEntity.ok(productService.getActiveOffers());
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PostMapping("/{id}/offer")
    public ResponseEntity<ProductResponse> setOffer(@PathVariable Long id,
                                                    @RequestParam(required = false) Long merchantId,
                                                    @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(productService.setOffer(id, merchantId, request));
    }

    @DeleteMapping("/{id}/offer")
    public ResponseEntity<ProductResponse> removeOffer(@PathVariable Long id,
                                                       @RequestParam(required = false) Long merchantId) {
        return ResponseEntity.ok(productService.removeOffer(id, merchantId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    @PutMapping("/{id}/reduce-stock")
    public ResponseEntity<ProductResponse> reduceStock(@PathVariable Long id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.reduceStock(id, quantity));
    }

    @PutMapping("/{id}/restore-stock")
    public ResponseEntity<ProductResponse> restoreStock(@PathVariable Long id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.restoreStock(id, quantity));
    }
}
