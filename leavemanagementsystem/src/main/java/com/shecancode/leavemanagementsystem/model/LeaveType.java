package com.shecancode.leavemanagementsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., PTO, Sick, Compassionate, Maternity

    private boolean reasonRequired;

    private boolean documentRequired;

    // accrual rate per month, default 1.66 for PTO
    private double accrualRatePerMonth;

    // carryover max days into next year
    private double carryoverMax;
}
