package com.dat.backend_version_2.controller.training;

import com.dat.backend_version_2.domain.training.Coach;
import com.dat.backend_version_2.dto.training.Coach.CoachReq;
import com.dat.backend_version_2.dto.training.Coach.CoachRes;
import com.dat.backend_version_2.mapper.training.CoachMapper;
import com.dat.backend_version_2.service.training.CoachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/coaches")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService coachService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<CoachRes.PersonalInfo> createCoach(
            @Valid @RequestBody CoachReq.CoachInfo coachInfo) {
        Coach coach = coachService.createCoach(coachInfo);
        CoachRes.PersonalInfo personalInfo = CoachMapper.coachToPersonalInfo(coach);

        URI location = URI.create("/api/v1/coaches/" + coach.getIdAccount());

        return ResponseEntity
                .created(location)
                .body(personalInfo);
    }
}
