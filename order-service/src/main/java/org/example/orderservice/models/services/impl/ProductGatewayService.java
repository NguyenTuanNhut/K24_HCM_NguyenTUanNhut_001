package org.example.orderservice.models.services.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.exceptions.ProductNotFoundException;
import org.example.orderservice.exceptions.ProductServiceException;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductGatewayService {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    public ProductResponse getProductById(Long productId) {
        try {
            return productClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException e) {
            log.error("Feign exception when calling product service for productId: {}", productId, e);
            throw new ProductServiceException("Product service error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected exception when calling product service for productId: {}", productId, e);
            throw new ProductServiceException("Product service error: " + e.getMessage(), e);
        }
    }

    public ProductResponse getProductByIdFallback(Long productId, Throwable throwable) {
        if (throwable instanceof ProductNotFoundException notFoundException) {
            throw notFoundException;
        }
        if (throwable instanceof ProductServiceException productServiceException) {
            throw productServiceException;
        }
        log.error("Fallback triggered for productId: {} due to: {}", productId, throwable.getMessage(), throwable);
        throw new ProductServiceException("Product service is currently unavailable or circuit is open: " + throwable.getMessage(), throwable);
    }
}
