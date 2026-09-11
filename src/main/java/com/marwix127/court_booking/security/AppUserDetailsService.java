package com.marwix127.court_booking.security;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.marwix127.court_booking.user.AppUserRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
    

@Service
@RequiredArgsConstructor 
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;
    @Override 
  public UserDetails loadUserByUsername(String username) {
       
        var user = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
                return User.builder().disabled(!user.isEnabled())
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities("ROLE_" + user.getRole())
                        .build();
}
}
