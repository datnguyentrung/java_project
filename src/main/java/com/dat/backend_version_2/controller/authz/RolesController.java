package com.dat.backend_version_2.controller.authz;

import com.dat.backend_version_2.domain.authz.Roles;
import com.dat.backend_version_2.service.authz.RolesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("api/v1/roles")
@RequiredArgsConstructor
public class RolesController {
    private final RolesService rolesService;

    public URI Location(Roles role){
        return URI.create("/api/v1/roles/" + role.getIdRole());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<Roles> createRole(@RequestBody Roles role) {
        Roles roles = rolesService.createRole(role);
        return ResponseEntity
                .created(Location(role)) // HTTP 201 + header "Location"
                .body(roles);
    }
}
