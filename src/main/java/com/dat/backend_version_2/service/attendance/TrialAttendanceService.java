package com.dat.backend_version_2.service.attendance;

import com.dat.backend_version_2.domain.attendance.TrialAttendance;
import com.dat.backend_version_2.domain.registration.Registration;
import com.dat.backend_version_2.domain.training.ClassSession;
import com.dat.backend_version_2.domain.training.Coach;
import com.dat.backend_version_2.dto.attendance.AttendanceDTO;
import com.dat.backend_version_2.dto.attendance.TrialAttendanceDTO;
import com.dat.backend_version_2.enums.attendance.AttendanceStatus;
import com.dat.backend_version_2.mapper.attendance.TrialAttendanceMapper;
import com.dat.backend_version_2.repository.attendance.TrialAttendanceRepository;
import com.dat.backend_version_2.service.registration.RegistrationService;
import com.dat.backend_version_2.service.training.ClassSessionService;
import com.dat.backend_version_2.service.training.CoachService;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrialAttendanceService {
    private final TrialAttendanceRepository trialAttendanceRepository;
    private final ClassSessionService classSessionService;
    private final RegistrationService registrationService;
    private final CoachService coachService;

    public List<TrialAttendanceDTO.TrialAttendanceDetail> getAllTrialAttendance() {
        return trialAttendanceRepository.findAll()
                .stream()
                .map(TrialAttendanceMapper::trialAttendanceToTrialAttendanceDetail)
                .toList();
    }

    @Transactional
    public void markAttendance(AttendanceDTO.AttendanceInfo request, String idUser
    ) throws IdInvalidException, JsonProcessingException {

        classSessionService.validateActiveClassSession(request.getIdClassSession());

        registrationService.getAndValidateRegisterdRegistration(request.getIdAccount());

        Coach coach = coachService.getAndValidateActiveCoach(UUID.fromString(idUser));

        TrialAttendance trialAttendance = TrialAttendanceMapper
                .attendanceInfoToTrialAttendance(request);
        trialAttendance.setIdClassSession(request.getIdClassSession());
        trialAttendance.setIdRegistration(UUID.fromString(request.getIdAccount()));
        trialAttendance.setAttendanceCoach(coach);
        trialAttendance.setAttendanceDate(LocalDate.now());
        if (request.getEvaluation().getEvaluationStatus() != null) {
            trialAttendance.setEvaluationCoach(coach);
        }

        trialAttendanceRepository.save(trialAttendance);
    }

    public List<TrialAttendanceDTO.TrialAttendanceDetail> getCurrentTrialAttendance() {
        return trialAttendanceRepository.findAllWithRegistrationAndClassSessionByAttendanceDate(LocalDate.now())
                .stream()
                .map(TrialAttendanceMapper::trialAttendanceToTrialAttendanceDetail)
                .toList();
    }

    public TrialAttendance getTrialAttendanceById(AttendanceDTO.TrialAttendanceKey key) throws IdInvalidException {
        return trialAttendanceRepository.findById(key)
                .orElseThrow(() -> new IdInvalidException("TrialAttendance not found with key: " + key));
    }

    public void markEvaluation(TrialAttendanceDTO.TrialMarkEvaluation markEvaluation, String idUser
    ) throws IdInvalidException {
        TrialAttendance trialAttendance = getTrialAttendanceById(markEvaluation.getAttendanceKey());

        if (trialAttendance.getAttendanceStatus()== AttendanceStatus.V
                || trialAttendance.getAttendanceStatus()== AttendanceStatus.P) {
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

        trialAttendance.setEvaluationStatus(markEvaluation.getEvaluationStatus());
        trialAttendance.setEvaluationCoach(coach);
        trialAttendanceRepository.save(trialAttendance);
    }
}
