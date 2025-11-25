package com.dat.backend_version_2.controller.attendance;

import com.dat.backend_version_2.dto.attendance.AttendanceDTO;
import com.dat.backend_version_2.dto.attendance.StudentAttendanceDTO;
import com.dat.backend_version_2.producer.MessageProducer;
import com.dat.backend_version_2.service.attendance.StudentAttendanceService;
import com.dat.backend_version_2.util.SecurityUtil;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.dat.backend_version_2.util.error.UserNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/student-attendance")
@RequiredArgsConstructor
public class StudentAttendanceController {
    private final StudentAttendanceService studentAttendanceService;
    private final MessageProducer messageProducer;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> createAttendance(
            @RequestBody AttendanceDTO.AttendanceInfo attendanceDTO,
            Authentication authentication) throws IdInvalidException, JsonProcessingException {
        String idUser = authentication.getName();

        studentAttendanceService.createStudentAttendance(attendanceDTO, idUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                "Attendance created successfully for student: " + attendanceDTO.getIdAccount()
        );
    }

    @PatchMapping("/attendance")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> markAttendance(
            @RequestBody StudentAttendanceDTO.StudentMarkAttendance markAttendance,
            Authentication authentication) throws ResponseStatusException {
        String idUser = authentication.getName();

        messageProducer.sendAttendanceRequest(markAttendance, idUser);
        return ResponseEntity.ok(
                "Attendance update request sent successfully for key: " + markAttendance.getAttendanceAccountKey()
        );
    }

    // Sửa đánh giá
    @PatchMapping("/evaluation")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> markEvaluation(
            @RequestBody StudentAttendanceDTO.StudentMarkEvaluation markEvaluation,
            Authentication authentication) throws ResponseStatusException {
        String idUser = authentication.getName();

        messageProducer.sendEvaluationRequest(markEvaluation, idUser);
        return ResponseEntity.ok(
                "Evaluation updated successfully for key: " + markEvaluation.getAttendanceAccountKey()
        );
    }

    // Điểm danh theo mã buổi học và ngày điểm danh
    @PostMapping("/class-session")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<Map<String, Object>> attendanceByClassSession(
            @RequestParam String idClassSession,
            @RequestParam LocalDate attendanceDate
    ) throws ResponseStatusException {
        try {
            // Lấy status từ JWT token
            Optional<String> userStatus = SecurityUtil.getCurrentUserStatus();
            if (userStatus.isPresent() && !"ACTIVE".equals(userStatus.get())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error",
                                "message", "Tài khoản người dùng không hoạt động. Trạng thái: " + userStatus.get()
                        ));
            }

            studentAttendanceService.createAttendancesByClassSessionAndDate(
                    new StudentAttendanceDTO.StudentAttendanceClassSession(
                            idClassSession,
                            attendanceDate
                    )
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Điểm danh thành công");
            response.put("idClassSession", idClassSession);

            return ResponseEntity.ok(response);
        } catch (RuntimeException | IdInvalidException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Lấy danh sách điểm danh ở 1 lớp học trong ngày
    @GetMapping("/class-session")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<StudentAttendanceDTO.StudentAttendanceDetail>> getAttendanceByClassSession(
            @RequestParam String idClassSession,
            @RequestParam LocalDate attendanceDate) throws IdInvalidException {

        return ResponseEntity.status(HttpStatus.OK).body(
                studentAttendanceService.getAttendanceByClassSessionAndDate(idClassSession, attendanceDate));
    }

    @GetMapping("/quarter")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<StudentAttendanceDTO.StudentAttendanceDetail>> getAttendancesByIdAccountAndQuarter(
            @RequestParam String idAccount,
            @RequestParam int year,
            @RequestParam int quarter) throws IllegalArgumentException, UserNotFoundException {
        return ResponseEntity.status(HttpStatus.OK).body(
                studentAttendanceService.getAttendancesByQuarter(idAccount, year, quarter));
    }
}