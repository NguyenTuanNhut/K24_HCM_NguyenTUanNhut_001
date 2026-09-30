package org.example.notifyservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.example.notifyservice.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedConsumer {

    private final EmailService emailService;

    public OrderCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "${notification.kafka.order-created-topic:order-created}",
            groupId = "${spring.kafka.consumer.group-id:notify-service}"
    )
    public void consume(String email) {
        log.info("Received order created event for email: {}", email);
        emailService.sendOrderCreatedEmail(email);
    }
}
