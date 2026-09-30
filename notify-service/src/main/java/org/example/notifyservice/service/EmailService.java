package org.example.notifyservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${notification.mail.from:${MAIL_FROM:${MAIL_USERNAME:no-reply@example.com}}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendOrderCreatedEmail(String recipient) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(recipient);
            message.setSubject("Xác nhận đơn hàng thành công");
            message.setText("Cảm ơn bạn đã đặt hàng! Đơn hàng của bạn đã được tạo thành công trên hệ thống.");
            mailSender.send(message);
            log.info("Email sent successfully to {}", recipient);
        } catch (Exception e) {
            log.error("Failed to send email to {}", recipient, e);
        }
    }
}
