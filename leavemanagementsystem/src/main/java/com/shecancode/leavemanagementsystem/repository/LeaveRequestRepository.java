package com.shecancode.leavemanagementsystem.repository;

import com.shecancode.leavemanagementsystem.model.LeaveRequest;
import com.shecancode.leavemanagementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployee(User user);
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);
}
