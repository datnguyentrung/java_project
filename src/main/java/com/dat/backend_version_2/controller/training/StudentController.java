package com.dat.backend_version_2.controller.training;

import com.dat.backend_version_2.domain.training.Student;
import com.dat.backend_version_2.dto.training.Student.StudentReq;
import com.dat.backend_version_2.dto.training.Student.StudentRes;
import com.dat.backend_version_2.mapper.training.StudentMapper;
import com.dat.backend_version_2.redis.training.StudentRedisImpl;
import com.dat.backend_version_2.service.training.StudentService;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final StudentRedisImpl studentRedis;

    /**
     * Tạo mới student
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<StudentRes.PersonalInfo> createStudent(
            @Valid @RequestBody StudentReq.StudentInfo studentInfo) throws IdInvalidException, JsonProcessingException {
        Student student = studentService.createStudent(studentInfo);
        StudentRes.PersonalInfo personalInfo = StudentMapper.studentToPersonalInfo(student);

        URI location = URI.create("/api/v1/students/" + student.getIdAccount());

        return ResponseEntity
                .created(location)
                .body(personalInfo);
    }

    /**
     * Lấy tất cả students với caching
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<StudentRes.PersonalAcademicInfo>> getAllStudents()
            throws JsonProcessingException {
        List<StudentRes.PersonalAcademicInfo> students = studentRedis.getAllStudents();
        if (students == null) {
            students = studentService.getAllStudents();
            studentRedis.saveAllStudents(students);
        }
        return ResponseEntity.ok(students);
    }

    /**
     * Lấy students theo branch ID
     */
    @GetMapping("/branch/{idBranch}")
    public ResponseEntity<List<StudentRes.PersonalAcademicInfo>> getStudentByBranch(
            @PathVariable Integer idBranch) throws IdInvalidException, JsonProcessingException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(studentService.getStudentsByIdBranch(idBranch));
    }

    /**
     * Lấy students theo class session ID với caching tối ưu
     */
    @GetMapping("/class-session/{idClassSession}")
    public ResponseEntity<List<StudentRes.PersonalAcademicInfo>> getStudentByClassSession(
            @PathVariable String idClassSession) throws IdInvalidException, JsonProcessingException {

        log.debug("Fetching students for class session: {}", idClassSession);

        // Kiểm tra cache trước
        List<StudentRes.PersonalAcademicInfo> students = studentRedis.getStudentsByIdClassSession(idClassSession);

        if (students != null) {
            // Cache HIT - return luôn mà không gọi service
            log.debug("Cache hit - returning cached data for class session: {}", idClassSession);
            return ResponseEntity.ok(students);
        }

        // Cache MISS - gọi service và lưu vào cache
        log.debug("Cache miss - fetching students from database for class session: {}", idClassSession);
        students = studentService.getStudentsByIdClassSession(idClassSession);
        studentRedis.saveStudentsByIdClassSession(idClassSession, students);

        return ResponseEntity.ok(students);
    }
}