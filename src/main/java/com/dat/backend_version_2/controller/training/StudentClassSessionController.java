package com.dat.backend_version_2.controller.training;

import com.dat.backend_version_2.domain.training.Student;
import com.dat.backend_version_2.dto.training.StudentClassSession.StudentClassSessionReq;
import com.dat.backend_version_2.service.training.StudentClassSessionService;
import com.dat.backend_version_2.service.training.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student-class-session")
@RequiredArgsConstructor
public class StudentClassSessionController {
    private final StudentClassSessionService studentClassSessionService;
    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<Map<String, Object>> createStudentClassSession(@RequestBody StudentClassSessionReq request) {
        Map<String, Object> response = new HashMap<>();
        Student student = studentService.getStudentById(request.getIdUser());

        try {
            studentClassSessionService.createStudentClassSession(student, request.getIdClassSessions());

            // Trả về response
            response.put("status", "success");
            response.put("message", "Đăng ký ClassSessions thành công");
            response.put("idUser", request.getIdUser());
            response.put("idClassSessions", request.getIdClassSessions());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }
}
