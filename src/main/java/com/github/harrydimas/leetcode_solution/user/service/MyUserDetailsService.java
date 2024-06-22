package com.github.harrydimas.leetcode_solution.user.service;

import com.github.harrydimas.leetcode_solution.user.model.MyUserDetails;
import com.github.harrydimas.leetcode_solution.user.repos.UserRepository;
import com.github.harrydimas.leetcode_solution.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found."));
        return new MyUserDetails(user);
    }
}
