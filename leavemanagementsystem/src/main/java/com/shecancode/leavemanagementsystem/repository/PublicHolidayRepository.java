package com.shecancode.leavemanagementsystem.repository;

import com.shecancode.leavemanagementsystem.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findByDateGreaterThanEqual(LocalDate date);
}