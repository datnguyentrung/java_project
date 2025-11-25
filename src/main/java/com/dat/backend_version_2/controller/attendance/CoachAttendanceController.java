package com.dat.backend_version_2.controller.attendance;

import com.dat.backend_version_2.dto.attendance.CoachAttendanceDTO;
import com.dat.backend_version_2.dto.attendance.CoachAttendanceRes;
import com.dat.backend_version_2.dto.authentication.LoginRes;
import com.dat.backend_version_2.enums.authentication.UserStatus;
import com.dat.backend_version_2.redis.attendance.CoachAttendanceRedisImpl;
import com.dat.backend_version_2.service.attendance.CoachAttendanceService;
import com.dat.backend_version_2.util.ConverterUtils;
import com.dat.backend_version_2.util.SecurityUtil;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.dat.backend_version_2.util.error.UserNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Controller xử lý điểm danh huấn luyện viên
 * Bao gồm các chức năng: nhận diện khuôn mặt bất đồng bộ, tạo điểm danh, và truy vấn lịch sử điểm danh
 */
@RestController
@RequestMapping("/api/v1/coach-attendance")
@RequiredArgsConstructor
@Slf4j
public class CoachAttendanceController {
    private final CoachAttendanceService coachAttendanceService;
    private final CoachAttendanceRedisImpl coachAttendanceRedis;

    /**
     * API tạo điểm danh cho huấn luyện viên
     *
     * @param createRequest Thông tin điểm danh
     * @return Thông báo kết quả điểm danh
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> createCoachAttendance(
            @RequestBody CoachAttendanceDTO.CreateRequest createRequest,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) throws IdInvalidException, JsonProcessingException {
        String idUser = authentication.getName();

        LoginRes.UserLogin userLogin = SecurityUtil.getCurrentUser(jwt);
        if (!userLogin.getRole().equals("COACH")) {
            return ResponseEntity.status(403).body("Chỉ huấn luyện viên mới có quyền điểm danh");
        }

        if (userLogin.getStatus() == UserStatus.BANNED) {
            // Log chuyên nghiệp cho trường hợp BANNED
            log.warn("AUTH_DENIED | Status: BANNED | Subject ID: {} | Account ID: {} | Reason: Explicitly blocked.",
                    jwt.getSubject(),
                    userLogin.getIdAccount());

            return ResponseEntity.status(403).body("Tài khoản huấn luyện viên đang bị vô hiệu hóa");

        } else if (userLogin.getStatus() == UserStatus.INACTIVE) {
            // Log chuyên nghiệp cho trường hợp INACTIVE
            log.warn("AUTH_DENIED | Status: INACTIVE | Subject ID: {} | Account ID: {} | Reason: Account pending activation or approval.",
                    jwt.getSubject(),
                    userLogin.getIdAccount());

            // Hoàn thiện ResponseEntity cho INACTIVE
            return ResponseEntity.status(403).body("Tài khoản chưa được kích hoạt hoặc đang chờ duyệt");
        }

        createRequest.setIdUser(idUser);
        coachAttendanceService.markCoachAttendance(createRequest);

        LocalDate date = createRequest.getCreatedAt().toLocalDate();

        String key = coachAttendanceRedis.getKeyByIdCoachAndYearAndMonth(
                userLogin.getIdAccount(),
                date.getYear(),
                date.getMonthValue());

        coachAttendanceRedis.deleteByKey(key);

        return ResponseEntity.ok("Điểm danh huấn luyện viên thành công");
    }

    /**
     * API lấy lịch sử điểm danh của huấn luyện viên theo tháng/năm
     * Sử dụng Redis cache để tối ưu hiệu năng
     *
     * @param idCoach ID của huấn luyện viên
     * @param year    Năm cần truy vấn
     * @param month   Tháng cần truy vấn
     * @return Danh sách điểm danh của huấn luyện viên
     */
    @GetMapping("/{idCoach}/year-month")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<CoachAttendanceRes>> getCoachAttendanceByYearAndMonth(
            @PathVariable String idCoach,
            @RequestParam Integer year,
            @RequestParam Integer month) throws UserNotFoundException, JsonProcessingException {

        // Kiểm tra cache Redis trước
        List<CoachAttendanceRes> coachAttendanceResList =
                coachAttendanceRedis.getCoachAttendanceByIdCoachAndYearAndMonth(
                        idCoach,
                        year,
                        month
                );

        // Nếu không có trong cache, lấy từ database và lưu vào cache
        if (coachAttendanceResList == null) {
            coachAttendanceResList =
                    coachAttendanceService.getCoachAttendanceByIdCoachAndYearAndMonth(
                            idCoach,
                            year,
                            month
                    );
            coachAttendanceRedis.saveCoachAttendanceByIdCoachAndYearAndMonth(
                    idCoach,
                    year,
                    month,
                    coachAttendanceResList
            );
        }
        return ResponseEntity.ok(coachAttendanceResList);
    }
}
