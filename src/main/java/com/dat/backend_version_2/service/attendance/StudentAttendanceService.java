package com.dat.backend_version_2.service.attendance;

import com.dat.backend_version_2.domain.attendance.StudentAttendance;
import com.dat.backend_version_2.domain.training.ClassSession;
import com.dat.backend_version_2.domain.training.Coach;
import com.dat.backend_version_2.domain.training.Student;
import com.dat.backend_version_2.dto.attendance.AttendanceDTO;
import com.dat.backend_version_2.dto.attendance.StudentAttendanceDTO;
import com.dat.backend_version_2.enums.attendance.AttendanceStatus;
import com.dat.backend_version_2.mapper.attendance.StudentAttendanceMapper;
import com.dat.backend_version_2.repository.attendance.StudentAttendanceRepository;
import com.dat.backend_version_2.service.training.ClassSessionService;
import com.dat.backend_version_2.service.training.CoachService;
import com.dat.backend_version_2.service.training.StudentClassSessionService;
import com.dat.backend_version_2.service.training.StudentService;
import com.dat.backend_version_2.util.ConverterUtils;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.dat.backend_version_2.util.error.UserNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentAttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentClassSessionService studentClassSessionService;
    private final ClassSessionService classSessionService;
    private final CoachService coachService;
    private final StudentService studentService;

    // Tìm bản điểm danh theo id
    public StudentAttendance getStudentAttendanceById(AttendanceDTO.StudentAttendanceKey key) throws IdInvalidException {
        return studentAttendanceRepository.findById(key)
                .orElseThrow(() -> new IdInvalidException("StudentAttendance not found with key: " + key));
    }

    public boolean hasAttendance(
            Student student, ClassSession classSession, LocalDate attendanceDate) {
        return studentAttendanceRepository.existsByStudentAndClassSessionAndAttendanceDate(student, classSession, attendanceDate);
    }

    // Điểm danh theo mã lớp học
    @Transactional
    public void createAttendancesInternal(
            StudentAttendanceDTO.StudentAttendanceClassSession request) {
        String idClassSession = request.getIdClassSession();
        LocalDate attendanceDate = request.getAttendanceDate();

        List<UUID> allStudentIdsInClassSession = studentClassSessionService
                .getStudentIdsByClassSession(idClassSession);

        if (allStudentIdsInClassSession.isEmpty()) {
            log.warn("No students found in class session: {}", idClassSession);
            return;
        }

        Set<UUID> attendedStudentIds = studentAttendanceRepository
                .findAttendedStudentIdsByClassSessionAndDate(idClassSession, attendanceDate);

        List<UUID> unrecordedStudentIds = allStudentIdsInClassSession.stream()
                .filter(studentId -> !attendedStudentIds.contains(studentId))
                .toList();

        List<StudentAttendance> attendances = unrecordedStudentIds.stream()
                .map(studentId -> {
                    StudentAttendance attendance = new StudentAttendance();
                    attendance.setAttendanceDate(attendanceDate);
                    attendance.setIdUser(studentId);
                    attendance.setIdClassSession(idClassSession);
                    return attendance;
                })
                .toList();

        if (!attendances.isEmpty()) {
            try {
                List<StudentAttendance> savedAttendances = studentAttendanceRepository.saveAll(attendances);
                log.info("Successfully saved {} attendance records for session {}", savedAttendances.size(), idClassSession);
            } catch (Exception e) {
                log.error("Error saving attendances for session {}: {}", idClassSession, e.getMessage(), e);
                throw e; // Re-throw to ensure transaction rollback if needed
            }
        } else {
            log.info("No new attendance records to save for session {}", idClassSession);
        }

        log.info("Finished createAttendancesInternal for session {}", idClassSession);
    }

    @Transactional
    public void createAttendancesByClassSessionAndDate(
            StudentAttendanceDTO.StudentAttendanceClassSession request
    ) throws IdInvalidException, JsonProcessingException {
        // 1. Validation (BẮT BUỘC cho Controller)
        classSessionService.validateActiveClassSession(request.getIdClassSession());

        // 2. Gọi hàm lõi
        createAttendancesInternal(request);
    }

    // Lấy danh sách điểm danh ở 1 lớp học trong ngày
    public List<StudentAttendanceDTO.StudentAttendanceDetail> getAttendanceByClassSessionAndDate(String idClassSession, LocalDate attendanceDate) throws IdInvalidException {
        return studentAttendanceRepository.findByIdClassSessionAndAttendanceDate(idClassSession, attendanceDate).stream()
                .map(StudentAttendanceMapper::studentAttendanceToStudentAttendanceDetail)
                .toList();
    }

    // Sửa cột "Điểm danh" trong 1 bản ghi
    // idAccount: mã Huấn Luyện Viên điểm danh
    @Transactional
    public void markAttendance(StudentAttendanceDTO.StudentMarkAttendance markAttendance, String idUser
    ) throws IdInvalidException, ResponseStatusException {
        StudentAttendance studentAttendance = studentAttendanceRepository.findByStudentAttendanceAccountKey(markAttendance.getAttendanceAccountKey())
                .orElseThrow(() -> new IdInvalidException("StudentAttendance not found with key: " + markAttendance.getAttendanceAccountKey()));

        Coach coach = coachService.getAndValidateActiveCoach(UUID.fromString(idUser));

        studentAttendance.setAttendanceStatus(markAttendance.getAttendanceStatus());
        if (markAttendance.getAttendanceStatus() == AttendanceStatus.V ||
                markAttendance.getAttendanceStatus() == AttendanceStatus.P) {
            studentAttendance.setEvaluationCoach(null);
            studentAttendance.setEvaluationStatus(null);
        } else {
            studentAttendance.setAttendanceTime(LocalTime.now());
            studentAttendance.setAttendanceCoach(coach);
        }
        studentAttendanceRepository.save(studentAttendance);
    }

    // Sửa cột "Đánh giá" trong 1 bản ghi
    // idAccount: mã Huấn Luyện Viên đánh giá
    @Transactional
    public void markEvaluation(StudentAttendanceDTO.StudentMarkEvaluation markEvaluation, String idUser
    ) throws IdInvalidException, ResponseStatusException {
        StudentAttendance studentAttendance = studentAttendanceRepository.findByStudentAttendanceAccountKey(markEvaluation.getAttendanceAccountKey())
                .orElseThrow(() -> new IdInvalidException("StudentAttendance not found with key: " + markEvaluation.getAttendanceAccountKey()));

        if (studentAttendance.getAttendanceStatus() == AttendanceStatus.V
                || studentAttendance.getAttendanceStatus() == AttendanceStatus.P) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EvaluationStatus is not allowed when AttendanceStatus is V or P"
            );
        }

        Coach coach = coachService.getCoachById(UUID.fromString(idUser));
        if (!coach.getIsActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Coach no longer have permission to use this feature"
            );
        }

        studentAttendance.setEvaluationStatus(markEvaluation.getEvaluationStatus());
        studentAttendance.setEvaluationCoach(coach);
        studentAttendanceRepository.save(studentAttendance);
    }

    // Tạo bản điểm danh mới
    // idAccount: mã Huấn Luyện Viên đánh giá
    @Transactional
    public void createStudentAttendance(AttendanceDTO.AttendanceInfo request, String idUser) throws IdInvalidException, JsonProcessingException {
//        AttendanceDTO.StudentAttendanceKey studentAttendanceKey = request.getAttendanceKey();
//        AttendanceDTO.AttendanceInfo attendanceInfo = request.getAttendanceInfo();

        ClassSession classSession = classSessionService
                .getClassSessionById(request.getIdClassSession());
        if (!classSession.getIsActive()) {
            throw new RuntimeException("ClassSession is not active: " + request.getIdClassSession());
        }

        Student student = studentService.getStudentByIdAccount(request.getIdAccount());
        if (!student.getIsActive()) {
            throw new RuntimeException("Student is not active: " + request.getIdAccount());
        }

        Coach coach = coachService.getCoachById(UUID.fromString(idUser));
        if (!coach.getIsActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Coach no longer have permission to use this feature"
            );
        }

        if (hasAttendance(student, classSession, LocalDate.now())) {
            throw new DuplicateKeyException("Attendance already exists for this student");
        }

        StudentAttendance studentAttendance = StudentAttendanceMapper
                .attendanceInfoToStudentAttendance(request);
        studentAttendance.setStudent(student);
        studentAttendance.setClassSession(classSession);
        studentAttendance.setAttendanceCoach(coach);

        if (request.getEvaluation().getEvaluationStatus() != null) {
            studentAttendance.setEvaluationCoach(coach);
        }
        studentAttendanceRepository.save(studentAttendance);
    }

    public List<StudentAttendanceDTO.StudentAttendanceDetail> getAttendancesByQuarter(
            String idAccount, int year, int quarter) throws IllegalArgumentException, UserNotFoundException {
        List<Integer> months = ConverterUtils.getMonthsByQuarter(quarter);
        Student student = studentService.getActiveStudentByIdAccount(idAccount);
        List<StudentAttendance> attendanceList = studentAttendanceRepository
                .findByStudentAndYearAndQuarter(student, year, months);
        return attendanceList.stream()
                .map(StudentAttendanceMapper::studentAttendanceToStudentAttendanceDetail)
                .toList();
    }


}
