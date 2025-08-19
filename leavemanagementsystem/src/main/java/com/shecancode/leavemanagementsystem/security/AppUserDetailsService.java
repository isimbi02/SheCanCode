package com.shecancode.leavemanagementsystem.security;

import com.shecancode.leavemanagementsystem.model.User;
import com.shecancode.leavemanagementsystem.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        // We allow both email and id
        User user = userRepository.findByEmail(usernameOrId)
                .orElseGet(() -> {
                    try {
                        Long id = Long.parseLong(usernameOrId);
                        return userRepository.findById(id).orElse(null);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
        if (user == null) throw new UsernameNotFoundException("User not found");
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(), List.of(authority)
        );
    }
}
