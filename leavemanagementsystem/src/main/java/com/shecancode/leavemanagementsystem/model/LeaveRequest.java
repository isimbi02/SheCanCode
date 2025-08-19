package com.shecancode.leavemanagementsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User employee;

    @ManyToOne(optional = false)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;

    private double days;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String reason;

    private String documentUrl;

    private String approverComment;

    private LocalDateTime createdAt;

    public enum Status { SUBMITTED, APPROVED, REJECTED, CANCELED }
}
