package com.Ecommerce.Project.controller;

import com.Ecommerce.Project.DTO.ReviewRequestDTO;
import com.Ecommerce.Project.Entity.RoleUpgrade;
import com.Ecommerce.Project.service.RoleUpgradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roleUpgrade")
public class RoleUpgradeRequestController {


    private RoleUpgradeService roleUpgradeService;

    public  RoleUpgradeRequestController(RoleUpgradeService roleUpgradeService)
    {
        this.roleUpgradeService=roleUpgradeService;
    }

    @GetMapping
    public ResponseEntity<List<RoleUpgrade>>  getRoleUpgradeRequest()
    {
        return ResponseEntity.ok(roleUpgradeService.getRoleUpgradeRequest());
    }

    @PostMapping
    public ResponseEntity<String> roleUpgradeRequest(@RequestParam("userId") int userId)
    {
        System.out.println("controller");
        return ResponseEntity.ok(roleUpgradeService.saveRoleRequest(userId));
    }
    @PostMapping("/reviewRequest")
    public ResponseEntity<String> reviewRoleRequest(@RequestBody ReviewRequestDTO reviewRequestDTO)
    {
        return ResponseEntity.ok(roleUpgradeService.reviewRoleRequest(reviewRequestDTO.getUserId(),reviewRequestDTO.getAdminId(),reviewRequestDTO.getStatus()));
    }
}
