package com.dat.backend_version_2.controller.training;

import com.dat.backend_version_2.domain.training.Branch;
import com.dat.backend_version_2.dto.training.Branch.BranchReq;
import com.dat.backend_version_2.redis.training.BranchRedisImpl;
import com.dat.backend_version_2.service.training.BranchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;
    private final BranchRedisImpl branchRedis;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<Branch> createBranch(@RequestBody BranchReq.BranchInfo branchReq) {
        Branch branch = branchService.createBranch(branchReq);
        URI location = URI.create("/api/v1/branches/" + branch.getIdBranch()); // hoặc idStudent

        return ResponseEntity
                .created(location) // HTTP 201 + header "Location"
                .body(branch);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN') and @userSec.isActive()")
    public ResponseEntity<List<Branch>> getAllBranches() throws JsonProcessingException {
        List<Branch> branches = branchRedis.getAllBranches();

        if (branches == null) {
            branches = branchService.getAllBranches();
            branchRedis.saveAllBranches(branches);
        }

        return ResponseEntity.ok(branches);
    }

    @GetMapping
    public ResponseEntity<List<Branch>> getActiveBranch() throws JsonProcessingException {
        List<Branch> branches = branchRedis.getAllBranches();
        if (branches == null) {
            branches = branchService.getAllBranches();
            branchRedis.saveAllBranches(branches);
        }

        return ResponseEntity.ok(
                branches.stream()
                        .filter(Branch::isActive)
                        .toList()
        );
    }
}
