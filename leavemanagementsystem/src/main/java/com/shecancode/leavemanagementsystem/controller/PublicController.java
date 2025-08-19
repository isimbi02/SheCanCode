package com.shecancode.leavemanagementsystem.controller;

import com.shecancode.leavemanagementsystem.model.PublicHoliday;
import com.shecancode.leavemanagementsystem.repository.PublicHolidayRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicHolidayRepository publicHolidayRepository;

    public PublicController(PublicHolidayRepository publicHolidayRepository) {
        this.publicHolidayRepository = publicHolidayRepository;
    }

    @GetMapping("/holidays")
    public List<PublicHoliday> upcomingHolidays() {
        return publicHolidayRepository.findByDateGreaterThanEqual(LocalDate.now());
    }
}
