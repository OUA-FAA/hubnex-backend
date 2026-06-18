package com.hubnex.backend.security;

import com.hubnex.backend.repository.UserRepository;
import com.hubnex.backend.service.RoleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String INACTIVE_ACCOUNT_MESSAGE =
            "Votre compte est d\u00e9sactiv\u00e9. Contactez l\u2019administrateur.";

    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findWithAccessByLogin(username)
                .map(user -> {
                    if (!Boolean.TRUE.equals(user.getActif())) {
                        throw new DisabledException(INACTIVE_ACCOUNT_MESSAGE);
                    }
                    return new CustomUserDetails(user, roleAccessService.resolveAuthorities(user));
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
