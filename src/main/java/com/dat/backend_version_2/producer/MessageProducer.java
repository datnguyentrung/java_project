package com.dat.backend_version_2.producer;

import com.dat.backend_version_2.config.RabbitMQConfig;
import com.dat.backend_version_2.dto.attendance.StudentAttendanceDTO;
import com.dat.backend_version_2.service.messaging.RabbitMQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProducer {

    private final RabbitMQService rabbitMQService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Sends attendance request to RabbitMQ with proper error handling
     *
     * @param markAttendance the attendance data to send
     * @param idUser the user ID to include in headers
     * @throws RuntimeException if RabbitMQ operation fails
     */
    public void sendAttendanceRequest(
            StudentAttendanceDTO.StudentMarkAttendance markAttendance,
            String idUser) {

        try {
            log.info("Sending attendance message to RabbitMQ: {} from account: {}", markAttendance, idUser);

            // Validate input
            if (markAttendance == null) {
                throw new IllegalArgumentException("Mark attendance data cannot be null");
            }
            if (idUser == null || idUser.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be null or empty");
            }

            // Prepare headers
            Map<String, Object> headers = new HashMap<>();
            headers.put("idUser", idUser);

            // Send message using the error-safe service
            rabbitMQService.sendMessage(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ATTENDANCE_ROUTING_KEY,
                markAttendance,
                headers
            );

            log.info("Successfully sent attendance message to RabbitMQ for user: {}", idUser);

        } catch (RabbitMQService.RabbitMQException e) {
            log.error("Failed to send attendance message to RabbitMQ for user {}: {}", idUser, e.getMessage(), e);
            throw new RuntimeException("Failed to send attendance request: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid data for attendance message: {}", e.getMessage());
            throw new RuntimeException("Invalid attendance data: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending attendance message for user {}: {}", idUser, e.getMessage(), e);
            throw new RuntimeException("Unexpected error sending attendance request: " + e.getMessage(), e);
        }
    }

    /**
     * Sends evaluation request to RabbitMQ with proper error handling
     *
     * @param evaluationRequest the evaluation data to send
     * @param idAccount the account ID
     * @throws RuntimeException if RabbitMQ operation fails
     */
    public void sendEvaluationRequest(
            StudentAttendanceDTO.StudentMarkEvaluation evaluationRequest,
            String idAccount) {

        try {
            log.info("Sending evaluation message to RabbitMQ: {} from account: {}", evaluationRequest, idAccount);

            // Validate input
            if (evaluationRequest == null) {
                throw new IllegalArgumentException("Evaluation request data cannot be null");
            }
            if (idAccount == null || idAccount.trim().isEmpty()) {
                throw new IllegalArgumentException("Account ID cannot be null or empty");
            }

            // Prepare headers
            Map<String, Object> headers = new HashMap<>();
            headers.put("idAccount", idAccount);

            // Send message using the error-safe service
            rabbitMQService.sendMessage(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.EVALUATION_ROUTING_KEY,
                evaluationRequest,
                headers
            );

            log.info("Successfully sent evaluation message to RabbitMQ for account: {}", idAccount);

        } catch (RabbitMQService.RabbitMQException e) {
            log.error("Failed to send evaluation message to RabbitMQ for account {}: {}", idAccount, e.getMessage(), e);
            throw new RuntimeException("Failed to send evaluation request: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid data for evaluation message: {}", e.getMessage());
            throw new RuntimeException("Invalid evaluation data: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending evaluation message for account {}: {}", idAccount, e.getMessage(), e);
            throw new RuntimeException("Unexpected error sending evaluation request: " + e.getMessage(), e);
        }
    }
}
