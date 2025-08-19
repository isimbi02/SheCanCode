package com.shecancode.leavemanagementsystem.config;

import com.shecancode.leavemanagementsystem.model.LeaveType;
import com.shecancode.leavemanagementsystem.model.PublicHoliday;
import com.shecancode.leavemanagementsystem.model.Role;
import com.shecancode.leavemanagementsystem.model.User;
import com.shecancode.leavemanagementsystem.repository.LeaveTypeRepository;
import com.shecancode.leavemanagementsystem.repository.PublicHolidayRepository;
import com.shecancode.leavemanagementsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(LeaveTypeRepository leaveTypeRepository,
                               UserRepository userRepository,
                               PublicHolidayRepository publicHolidayRepository,
                               PasswordEncoder encoder) {
        return args -> {
            if (leaveTypeRepository.count() == 0) {
                leaveTypeRepository.save(LeaveType.builder().name("PTO").reasonRequired(false).documentRequired(false).accrualRatePerMonth(1.66).carryoverMax(5).build());
                leaveTypeRepository.save(LeaveType.builder().name("SICK").reasonRequired(false).documentRequired(false).accrualRatePerMonth(0).carryoverMax(0).build());
                leaveTypeRepository.save(LeaveType.builder().name("COMPASSIONATE").reasonRequired(true).documentRequired(false).accrualRatePerMonth(0).carryoverMax(0).build());
                leaveTypeRepository.save(LeaveType.builder().name("MATERNITY").reasonRequired(true).documentRequired(true).accrualRatePerMonth(0).carryoverMax(0).build());
            }
            if (userRepository.count() == 0) {
                userRepository.save(User.builder().email("admin@demo.com").passwordHash(encoder.encode("Admin123!"))
                        .fullName("Admin User").role(Role.ADMIN).googleAvatarUrl(avatar("admin@demo.com")).build());
                userRepository.save(User.builder().email("manager@demo.com").passwordHash(encoder.encode("Manager123!"))
                        .fullName("Manager User").role(Role.MANAGER).googleAvatarUrl(avatar("manager@demo.com")).build());
                userRepository.save(User.builder().email("staff@demo.com").passwordHash(encoder.encode("Staff123!"))
                        .fullName("Staff User").role(Role.STAFF).googleAvatarUrl(avatar("staff@demo.com")).build());
            }
            if (publicHolidayRepository.count() == 0) {
                publicHolidayRepository.save(PublicHoliday.builder().name("New Year's Day").date(LocalDate.of(LocalDate.now().getYear(),1,1)).build());
                publicHolidayRepository.save(PublicHoliday.builder().name("Independence Day").date(LocalDate.of(LocalDate.now().getYear(),7,4)).build());
                publicHolidayRepository.save(PublicHoliday.builder().name("Christmas Day").date(LocalDate.of(LocalDate.now().getYear(),12,25)).build());
            }
        };
    }

    private String avatar(String email) {
        return "https://www.gravatar.com/avatar/" + Integer.toHexString(email.hashCode());
    }
}
