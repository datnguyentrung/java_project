package com.dat.backend_version_2.controller.registration;

import com.dat.backend_version_2.dto.registration.RegistrationDTO;
import com.dat.backend_version_2.service.registration.RegistrationService;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> createRegistration(
            @RequestBody @Valid RegistrationDTO.PersonalInfo personalInfo) {

        registrationService.createRegistration(personalInfo);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration created successfully for " + personalInfo.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<RegistrationDTO>> getAllRegistrations() {
        return ResponseEntity.ok(registrationService.getAllRegistrations());
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyAuthority('COACH', 'ADMIN') and @userSec.isActive()")
    public ResponseEntity<String> updateRegistration(
            @RequestBody @Valid RegistrationDTO registrationDTO) throws IdInvalidException, JsonProcessingException {
        registrationService.updateRegistration(
                registrationDTO.getIdRegistration(),
                registrationDTO.getPersonalInfo()
        );

        if (registrationDTO.getRegistrationInfo().getIdBranch() != null) {
            registrationService.registerRegistration(registrationDTO);
        }

        return ResponseEntity.ok("Update registration successfully");
    }
}
