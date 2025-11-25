package com.dat.backend_version_2.controller.attendance;

import com.dat.backend_version_2.dto.attendance.AttendanceDTO;
import com.dat.backend_version_2.dto.attendance.TrialAttendanceDTO;
import com.dat.backend_version_2.service.attendance.TrialAttendanceService;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trial-attendance")
@RequiredArgsConstructor
public class TrialAttendanceController {
    private final TrialAttendanceService trialAttendanceService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<TrialAttendanceDTO.TrialAttendanceDetail>> getAllTrialAttendance(){
        return ResponseEntity.ok(trialAttendanceService.getAllTrialAttendance());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> createTrialAttendance(
            @RequestBody @Valid AttendanceDTO.AttendanceInfo attendanceDTO,
            Authentication authentication) {
        String idUser = authentication.getName();

        try {
            trialAttendanceService.markAttendance(attendanceDTO, idUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Điểm danh thử thành công cho học viên: " + attendanceDTO.getIdAccount());
        } catch (IdInvalidException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<TrialAttendanceDTO.TrialAttendanceDetail>> getCurrentTrialAttendance(){
        return ResponseEntity.ok(trialAttendanceService.getCurrentTrialAttendance());
    }

    @PatchMapping("/evaluation")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> markEvaluation(
            @RequestBody TrialAttendanceDTO.TrialMarkEvaluation markEvaluation,
            Authentication authentication) throws IdInvalidException, ResponseStatusException {
        String idAccount = authentication.getName();
        trialAttendanceService.markEvaluation(markEvaluation, idAccount);
        return ResponseEntity.ok(
                "Evaluation updated successfully for key: " + markEvaluation.getAttendanceKey()
        );
    }
}
