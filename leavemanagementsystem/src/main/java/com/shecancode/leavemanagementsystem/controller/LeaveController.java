package com.shecancode.leavemanagementsystem.controller;

import com.shecancode.leavemanagementsystem.model.LeaveRequest;
import com.shecancode.leavemanagementsystem.model.LeaveType;
import com.shecancode.leavemanagementsystem.model.User;
import com.shecancode.leavemanagementsystem.repository.LeaveRequestRepository;
import com.shecancode.leavemanagementsystem.repository.LeaveTypeRepository;
import com.shecancode.leavemanagementsystem.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    public LeaveController(LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository, UserRepository userRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/types")
    public List<LeaveType> getLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @GetMapping("/balance")
    public Map<String, Object> getBalance(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        double accrued = calculateAccruedPto();
        double used = leaveRequestRepository.findByEmployee(user).stream()
                .filter(l -> l.getStatus() == LeaveRequest.Status.APPROVED && l.getLeaveType().getName().equalsIgnoreCase("PTO"))
                .mapToDouble(LeaveRequest::getDays).sum();
        double carryover = Math.min(5.0, getPrevYearUnusedPto(user));
        if (LocalDate.now().isAfter(LocalDate.of(Year.now().getValue(), 1, 31))) {
            carryover = 0.0; // expired after Jan 31
        }
        double balance = Math.max(0, carryover + accrued - used);
        return Map.of("accrued", accrued, "used", used, "carryover", carryover, "balance", balance);
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@AuthenticationPrincipal UserDetails principal,
                                   @RequestBody ApplyRequest req) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        LeaveType type = leaveTypeRepository.findById(req.leaveTypeId()).orElseThrow();
        if (type.isReasonRequired() && (req.reason() == null || req.reason().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }
        double days = ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        LeaveRequest lr = LeaveRequest.builder()
                .employee(user)
                .leaveType(type)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .days(days)
                .status(LeaveRequest.Status.SUBMITTED)
                .reason(req.reason())
                .documentUrl(req.documentUrl())
                .createdAt(LocalDateTime.now())
                .build();
        leaveRequestRepository.save(lr);
        return ResponseEntity.ok(Map.of("id", lr.getId(), "status", lr.getStatus()));
    }

    @GetMapping("/my")
    public List<LeaveRequest> myRequests(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return leaveRequestRepository.findByEmployee(user);
    }

    @GetMapping("/on-leave-today")
    public List<LeaveRequest> onLeaveToday() {
        LocalDate today = LocalDate.now();
        return leaveRequestRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
                .stream().filter(l -> l.getStatus() == LeaveRequest.Status.APPROVED).toList();
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, String> req) {
        LeaveRequest lr = leaveRequestRepository.findById(id).orElseThrow();
        lr.setStatus(LeaveRequest.Status.APPROVED);
        lr.setApproverComment(req.getOrDefault("comment", ""));
        leaveRequestRepository.save(lr);
        return ResponseEntity.ok(Map.of("status", lr.getStatus()));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, String> req) {
        LeaveRequest lr = leaveRequestRepository.findById(id).orElseThrow();
        lr.setStatus(LeaveRequest.Status.REJECTED);
        lr.setApproverComment(req.getOrDefault("comment", ""));
        leaveRequestRepository.save(lr);
        return ResponseEntity.ok(Map.of("status", lr.getStatus()));
    }

    private double calculateAccruedPto() {
        // 1.66 per month linear to current month in year
        int month = LocalDate.now().getMonthValue();
        return 1.66 * month;
    }

    private double getPrevYearUnusedPto(User user) {
        // Simple placeholder calculation: assume 0 for minimal implementation
        return 0.0;
    }

    public record ApplyRequest(@NotNull Long leaveTypeId, @NotNull LocalDate startDate, @NotNull LocalDate endDate, String reason, String documentUrl) {}
}
