package org.example.orderservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.models.constants.OrderStatus;
import org.example.orderservice.models.dto.requests.CreateOrderDetailRequest;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderDetailResponse;
import org.example.orderservice.models.dto.responses.OrderResponse;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.example.orderservice.models.entities.Order;
import org.example.orderservice.models.entities.OrderDetail;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.example.orderservice.models.services.OrderService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductGatewayService productGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_CREATED_TOPIC = "order-created";

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        double calculatedTotal = 0.0;
        List<OrderDetailRecord> preparedDetails = new ArrayList<>();

        for (CreateOrderDetailRequest item : request.items()) {
            ProductResponse product = productGatewayService.getProductById(item.productId());
            double unitPrice = product.price();
            double subtotal = unitPrice * item.quantity();
            calculatedTotal += subtotal;

            preparedDetails.add(new OrderDetailRecord(product.id(), product.name(), item.quantity(), unitPrice, subtotal));
        }

        Order order = Order.builder()
                .customerName(request.customerName())
                .total(calculatedTotal)
                .status(OrderStatus.CONFIRMED)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        for (OrderDetailRecord record : preparedDetails) {
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(savedOrder)
                    .productId(record.productId())
                    .quantity(record.quantity())
                    .unitPrice(record.unitPrice())
                    .build();

            OrderDetail savedDetail = orderDetailRepository.save(orderDetail);

            detailResponses.add(new OrderDetailResponse(
                    savedDetail.getId(),
                    record.productId(),
                    record.productName(),
                    record.quantity(),
                    record.unitPrice(),
                    record.subtotal()
            ));
        }

        try {
            kafkaTemplate.send(ORDER_CREATED_TOPIC, request.customerEmail());
        } catch (Exception e) {
            log.error("Failed to send Kafka event to topic {} for email {}", ORDER_CREATED_TOPIC, request.customerEmail(), e);
        }

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getCustomerName(),
                savedOrder.getTotal(),
                savedOrder.getStatus(),
                detailResponses
        );
    }

    private record OrderDetailRecord(
            Long productId,
            String productName,
            Integer quantity,
            Double unitPrice,
            Double subtotal
    ) {}
}
