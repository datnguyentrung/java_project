package com.dat.backend_version_2.controller.upload;

import com.dat.backend_version_2.dto.upload.BytescaleUploadRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bytescale-upload")
@RequiredArgsConstructor
public class BytescaleUploadController {

    @Value("${bytescale.api-key}")
    private String bytescaleApiKey;

    /**
     * Tạo một "vé upload" (Authorization Token) cho client
     *
     * @param bytescaleUploadRequest đối tượng chứa thông tin folder và tên file
     * @return ResponseEntity chứa chuỗi JWT (token)
     */
    @PostMapping("/auth-token")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> createUploadAuthToken(
            @RequestBody BytescaleUploadRequest bytescaleUploadRequest) {

        System.out.println(bytescaleUploadRequest);
        String folderPath = getBytescaleFolderPath(bytescaleUploadRequest.getFolderName());

        if (folderPath.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid folderName: " + bytescaleUploadRequest.getFolderName());
        }

        // 1. Tạo SecretKey từ API Key của bạn
        // Chú ý: API Key của Bytescale đã là 'secret_' nên ta dùng nó trực tiếp
        SecretKey key = Keys.hmacShaKeyFor(bytescaleApiKey.getBytes(StandardCharsets.UTF_8));

        // 2. Đặt thời gian hết hạn (ví dụ: 1 giờ kể từ bây giờ)
        Instant now = Instant.now();
        Instant expiryTime = now.plusSeconds(3600); // 1 giờ

        // 3. Tạo các 'claims' (nội dung của vé)
        Map<String, Object> claims = new HashMap<>();
        claims.put("path", folderPath + bytescaleUploadRequest.getFileName());
        claims.put("maxFileSizeBytes", 10485760); // 10MB
        claims.put("exp", expiryTime.getEpochSecond()); // Thời gian hết hạn
        claims.put("iat", now.getEpochSecond()); // Thời gian cấp
        claims.put("sub", "upload"); // Chủ đề

        String jwt = Jwts.builder()
                .claims(claims) // Thêm tất cả claims
                .signWith(key) // Ký bằng API Key của bạn
                .compact(); // Hoàn thành

        // 5. Trả chuỗi JWT này về cho client
        // Client sẽ dùng token này với SDK của Bytescale để upload
        return ResponseEntity.ok(jwt);
    }

    /**
     * Chuyển đổi tên folder thành đường dẫn tương ứng trong Bytescale
     *
     * @param folderName tên folder (ví dụ: "coach-attendance", "student-attendance")
     * @return đường dẫn đầy đủ trong Bytescale, trả về chuỗi rỗng nếu folder không hợp lệ
     */
    private String getBytescaleFolderPath(String folderName) {
        return switch (folderName) {
            case "coach_attendance" -> "/attendance/coach-attendance/";
            case "student_attendance" -> "/attendance/student-attendance/";
            default -> "";
        };
    }
}
