package com.dat.backend_version_2.consumer;

import com.dat.backend_version_2.config.RabbitMQConfig;
import com.dat.backend_version_2.dto.attendance.StudentAttendanceDTO;
import com.dat.backend_version_2.service.attendance.StudentAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentAttendanceConsumer {
    private final StudentAttendanceService studentAttendanceService;

    @RabbitListener(
            queues = RabbitMQConfig.ATTENDANCE_QUEUE,
            errorHandler = "rabbitMQErrorHandler"
    )
    public void handleAttendanceMessage(
            StudentAttendanceDTO.StudentMarkAttendance attendance,
            @Header("idUser") String idUser
    ) {
        try {
            log.info("Received attendance message from RabbitMQ: {} for User: {}", attendance, idUser);

            // Validate input data first
            validateAttendanceData(attendance, idUser);

            // Process the attendance
            studentAttendanceService.markAttendance(attendance, idUser);

            log.info("Successfully processed attendance for student: {}", idUser);

        } catch (IllegalArgumentException e) {
            log.error("Invalid data in attendance message: {}", e.getMessage());
            // Reject message and don't requeue for validation errors
            throw new AmqpRejectAndDontRequeueException("Invalid attendance data: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Critical error processing attendance message for user {}: {}", idUser, e.getMessage(), e);
            // Reject message and don't requeue for processing errors
            throw new AmqpRejectAndDontRequeueException("Failed to process attendance: " + e.getMessage(), e);
        }
    }

    /**
     * Validates attendance data to ensure all required fields are present and valid
     *
     * @param attendance the attendance data to validate
     * @param idUser     the user ID from header
     * @throws IllegalArgumentException if any validation fails
     */
    private void validateAttendanceData(StudentAttendanceDTO.StudentMarkAttendance attendance, String idUser) {
        if (attendance == null) {
            throw new IllegalArgumentException("Attendance data cannot be null");
        }

        if (idUser == null || idUser.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        if (attendance.getAttendanceAccountKey() == null) {
            throw new IllegalArgumentException("Attendance key cannot be null");
        }

        if (attendance.getAttendanceAccountKey().getIdAccount() == null) {
            throw new IllegalArgumentException("Student ID in attendance key cannot be null or empty");
        }

        if (attendance.getAttendanceAccountKey().getIdClassSession() == null ||
                attendance.getAttendanceAccountKey().getIdClassSession().trim().isEmpty()) {
            throw new IllegalArgumentException("Class session ID cannot be null or empty");
        }

        if (attendance.getAttendanceAccountKey().getAttendanceDate() == null) {
            throw new IllegalArgumentException("Attendance date cannot be null");
        }

        if (attendance.getAttendanceStatus() == null) {
            throw new IllegalArgumentException("Attendance status cannot be null");
        }
    }

    @RabbitListener(
            queues = RabbitMQConfig.EVALUATION_QUEUE,
            errorHandler = "rabbitMQErrorHandler"
    )
    public void handleEvaluationMessage(
            StudentAttendanceDTO.StudentMarkEvaluation evaluation,
            @Header("idUser") String idUser) {
        try {
            log.info("Received evaluation message from RabbitMQ: {} for User: {}", evaluation, idUser);

            // Validate input data first
            validateEvaluationData(evaluation, idUser);

            // Process the evaluation
            studentAttendanceService.markEvaluation(evaluation, idUser);

            log.info("Successfully processed evaluation for student: {}", idUser);

        } catch (IllegalArgumentException e) {
            log.error("Invalid data in evaluation message: {}", e.getMessage());
            // Reject message and don't requeue for validation errors
            throw new AmqpRejectAndDontRequeueException("Invalid evaluation data: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Critical error processing evaluation message for user {}: {}", idUser, e.getMessage(), e);
            // Reject message and don't requeue for processing errors
            throw new AmqpRejectAndDontRequeueException("Failed to process evaluation: " + e.getMessage(), e);
        }
    }

    /**
     * Validates evaluation data to ensure all required fields are present and valid
     *
     * @param evaluation the evaluation data to validate
     * @param idUser     the user ID from header
     * @throws IllegalArgumentException if any validation fails
     */
    private void validateEvaluationData(StudentAttendanceDTO.StudentMarkEvaluation evaluation, String idUser) {
        if (evaluation == null) {
            throw new IllegalArgumentException("Evaluation data cannot be null");
        }

        if (idUser == null || idUser.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (evaluation.getAttendanceAccountKey() == null) {
            throw new IllegalArgumentException("Evaluation key cannot be null");
        }

        if (evaluation.getAttendanceAccountKey().getIdAccount() == null) {
            throw new IllegalArgumentException("Student ID in evaluation key cannot be null or empty");
        }

        if (evaluation.getAttendanceAccountKey().getIdClassSession() == null ||
                evaluation.getAttendanceAccountKey().getIdClassSession().trim().isEmpty()) {
            throw new IllegalArgumentException("Class session ID cannot be null or empty");
        }

        if (evaluation.getAttendanceAccountKey().getAttendanceDate() == null) {
            throw new IllegalArgumentException("Evaluation date cannot be null");
        }

        if (evaluation.getEvaluationStatus() == null) {
            throw new IllegalArgumentException("Evaluation score cannot be null");
        }
    }
}
