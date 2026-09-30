package com.bank.api;

import com.bank.api.dto.IncidentResponse;
import com.bank.api.dto.TransferResponse;
import com.bank.security.UserPrincipal;
import com.bank.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<List<IncidentResponse>> list(
            @RequestParam(defaultValue = "open") String status
    ) {
        List<IncidentResponse> incidents;
        if ("all".equalsIgnoreCase(status)) {
            incidents = incidentService.listAll();
        } else {
            incidents = incidentService.listOpen();
        }
        return ResponseEntity.ok(incidents);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<TransferResponse> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        TransferResponse response = incidentService.approve(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<TransferResponse> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        TransferResponse response = incidentService.reject(id, principal.getId());
        return ResponseEntity.ok(response);
    }
}