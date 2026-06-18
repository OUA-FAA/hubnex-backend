package com.hubnex.backend.config;

import com.hubnex.backend.security.CustomUserDetailsService;
import com.hubnex.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN_AUTHORITY = "ADMIN";
    private static final String ROLE_ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (WebSecurity web) -> web.ignoring().requestMatchers(
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**"
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/cities/by-agence/**",
                                "/api/cities/by-agence/{agenceId}",
                                "/api/cities/by-agence/{agenceId}/active",
                                "/api/cities/by-agences",
                                "/api/cities/by-agences/**")
                        .hasAnyAuthority(
                                ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY,
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE",
                                "AGENCIES:VIEW", "AGENCIES:CREATE", "AGENCIES_VIEW", "AGENCIES_CREATE",
                                "AGENCE_VIEW", "AGENCE_CREATE",
                                "CITIES:VIEW", "CITIES_VIEW", "CITY_VIEW", "CITY_CREATE")
                        .requestMatchers(SecurityConfig::isDocketRecordRecoveryDeleteRequest)
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY,
                                "MANIFESTE:DELETE", "RECEPTION:DELETE", "DISPATCH:DELETE", "EXPEDITION:DELETE")
                        .requestMatchers(SecurityConfig::isDocketRecordImportRequest)
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY, "MANIFESTE:CREATE")
                        .requestMatchers(HttpMethod.POST, "/api/docket-records/send-to-conveyor")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:EDIT")
                        .requestMatchers(HttpMethod.GET, "/api/docket-records/conveyor-response")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:EDIT", "DISPATCH:EDIT", "EXPEDITION:EDIT")
                        .requestMatchers(HttpMethod.GET,
                                "/api/docket-records/recoveries",
                                "/api/docket-records/recoveries/",
                                "/api/docket-records/recoveries/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:VIEW", "DISPATCH:VIEW", "EXPEDITION:VIEW", "MANIFESTE:VIEW")
                        .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DASHBOARD:VIEW")

                        .requestMatchers(HttpMethod.GET, "/api/users")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "USERS:VIEW", "USERS_VIEW", "USER_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", "USERS:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/users", "/api/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", "USERS:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/users", "/api/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", "USERS:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/users", "/api/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", "USERS:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/groups", "/api/groups/**")
                        .hasAnyAuthority("ROLE_ADMIN", "GROUPS:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/groups", "/api/groups/**")
                        .hasAnyAuthority("ROLE_ADMIN", "GROUPS:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/groups", "/api/groups/**")
                        .hasAnyAuthority("ROLE_ADMIN", "GROUPS:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/groups", "/api/groups/**")
                        .hasAnyAuthority("ROLE_ADMIN", "GROUPS:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/groups", "/api/groups/**")
                        .hasAnyAuthority("ROLE_ADMIN", "GROUPS:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/companies", "/api/companies/**")
                        .hasAnyAuthority("ROLE_ADMIN", "COMPANIES:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/companies", "/api/companies/**")
                        .hasAnyAuthority("ROLE_ADMIN", "COMPANIES:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/companies", "/api/companies/**")
                        .hasAnyAuthority("ROLE_ADMIN", "COMPANIES:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/companies", "/api/companies/**")
                        .hasAnyAuthority("ROLE_ADMIN", "COMPANIES:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/companies", "/api/companies/**")
                        .hasAnyAuthority("ROLE_ADMIN", "COMPANIES:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/hubs")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "HUBS:VIEW", "HUBS_VIEW", "HUB_VIEW",
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "HUBS:VIEW", "HUBS_VIEW", "HUB_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/hubs", "/api/hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN", "HUBS:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/hubs", "/api/hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN", "HUBS:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/hubs", "/api/hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN", "HUBS:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/hubs", "/api/hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN", "HUBS:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/agences")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "AGENCIES:VIEW", "AGENCIES_VIEW", "AGENCE_VIEW",
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/agences/by-hubs", "/api/agences/by-hubs/**")
                        .hasAnyAuthority("ROLE_ADMIN",
                                "AGENCIES:VIEW", "AGENCIES_VIEW", "AGENCE_VIEW",
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/agences/**")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "AGENCIES:VIEW", "AGENCIES_VIEW", "AGENCE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/agences", "/api/agences/**")
                        .hasAnyAuthority("ROLE_ADMIN", "AGENCIES:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/agences", "/api/agences/**")
                        .hasAnyAuthority("ROLE_ADMIN", "AGENCIES:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/agences", "/api/agences/**")
                        .hasAnyAuthority("ROLE_ADMIN", "AGENCIES:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/agences", "/api/agences/**")
                        .hasAnyAuthority("ROLE_ADMIN", "AGENCIES:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/cities")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "CITIES:VIEW", "CITIES_VIEW", "CITY_VIEW",
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/cities/**")
                        .hasAnyAuthority("ROLE_ADMIN", 
                                "CITIES:VIEW", "CITIES_VIEW", "CITY_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/cities", "/api/cities/**")
                        .hasAnyAuthority("ROLE_ADMIN", "CITIES:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/cities", "/api/cities/**")
                        .hasAnyAuthority("ROLE_ADMIN", "CITIES:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/cities", "/api/cities/**")
                        .hasAnyAuthority("ROLE_ADMIN", "CITIES:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/cities", "/api/cities/**")
                        .hasAnyAuthority("ROLE_ADMIN", "CITIES:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/docket-records", "/api/docket-records/**")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/docket-records", "/api/docket-records/**")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/docket-records", "/api/docket-records/**")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/docket-records", "/api/docket-records/**")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/docket-records", "/api/docket-records/**")
                        .hasAnyAuthority("ROLE_ADMIN", "MANIFESTE:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/bon-receptions", "/api/bon-receptions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/bon-receptions", "/api/bon-receptions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/bon-receptions", "/api/bon-receptions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/bon-receptions", "/api/bon-receptions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/bon-receptions", "/api/bon-receptions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "RECEPTION:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/expeditions", "/api/expeditions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "EXPEDITION:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/expeditions", "/api/expeditions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "EXPEDITION:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/expeditions", "/api/expeditions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "EXPEDITION:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/expeditions", "/api/expeditions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "EXPEDITION:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/expeditions", "/api/expeditions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "EXPEDITION:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/dispatch", "/api/dispatch/**", "/api/bon-dispatch", "/api/bon-dispatch/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DISPATCH:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/dispatch", "/api/dispatch/**", "/api/bon-dispatch", "/api/bon-dispatch/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DISPATCH:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/dispatch", "/api/dispatch/**", "/api/bon-dispatch", "/api/bon-dispatch/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DISPATCH:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/dispatch", "/api/dispatch/**", "/api/bon-dispatch", "/api/bon-dispatch/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DISPATCH:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/dispatch", "/api/dispatch/**", "/api/bon-dispatch", "/api/bon-dispatch/**")
                        .hasAnyAuthority("ROLE_ADMIN", "DISPATCH:DELETE")

                        .requestMatchers(HttpMethod.GET, "/api/tracking", "/api/tracking/**")
                        .hasAnyAuthority("ROLE_ADMIN", "TRACKING:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/tracking", "/api/tracking/**")
                        .hasAnyAuthority("ROLE_ADMIN", "TRACKING:CREATE")

                        .requestMatchers(HttpMethod.GET, "/api/roles")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:VIEW",
                                "USERS:VIEW", "USERS:CREATE", "USERS_VIEW", "USERS_CREATE", "USER_VIEW", "USER_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/permissions")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/roles", "/api/roles/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/roles", "/api/roles/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:EDIT")
                        .requestMatchers(HttpMethod.PATCH, "/api/roles", "/api/roles/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:EDIT")
                        .requestMatchers(HttpMethod.DELETE, "/api/roles", "/api/roles/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLES_PERMISSIONS:DELETE")
                        .requestMatchers(HttpMethod.POST, "/api/hubs/import-excel")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, "/api/cities/import-excel").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/docket-records/import-excel/failed-rows/export",
                                "/api/docket-records/import-excel/failed-rows/export/")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, "/api/docket-records/send-to-conveyor")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET, "/api/docket-records/conveyor-response")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET,
                                "/api/docket-records/recoveries",
                                "/api/docket-records/recoveries/",
                                "/api/docket-records/recoveries/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/docket-records/manifests/*",
                                "/api/docket-records/manifests/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET,
                                "/api/bon-receptions/conveyor-ready",
                                "/api/bon-receptions/incomplete-docket-records",
                                "/api/bon-receptions/incomplete-docket-records/export").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bon-receptions/incomplete-docket-records/import-corrected").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/*/permissions", "/api/roles/{roleId}/permissions").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET,
                                "/api/bon-receptions", "/api/bon-receptions/**",
                                "/api/dispatch", "/api/dispatch/**",
                                "/api/bon-dispatch", "/api/bon-dispatch/**",
                                "/api/expeditions", "/api/expeditions/**",
                                "/api/tracking", "/api/tracking/**",
                                "/api/docket-records", "/api/docket-records/**",
                                "/api/etiquettes", "/api/etiquettes/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.POST,
                                "/api/bon-receptions", "/api/bon-receptions/**",
                                "/api/dispatch", "/api/dispatch/**",
                                "/api/bon-dispatch", "/api/bon-dispatch/**",
                                "/api/expeditions", "/api/expeditions/**",
                                "/api/tracking", "/api/tracking/**",
                                "/api/docket-records", "/api/docket-records/**",
                                "/api/etiquettes", "/api/etiquettes/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.PUT,
                                "/api/bon-receptions", "/api/bon-receptions/**",
                                "/api/dispatch", "/api/dispatch/**",
                                "/api/bon-dispatch", "/api/bon-dispatch/**",
                                "/api/expeditions", "/api/expeditions/**",
                                "/api/tracking", "/api/tracking/**",
                                "/api/docket-records", "/api/docket-records/**",
                                "/api/etiquettes", "/api/etiquettes/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/bon-receptions", "/api/bon-receptions/**",
                                "/api/dispatch", "/api/dispatch/**",
                                "/api/bon-dispatch", "/api/bon-dispatch/**",
                                "/api/expeditions", "/api/expeditions/**",
                                "/api/tracking", "/api/tracking/**",
                                "/api/docket-records", "/api/docket-records/**",
                                "/api/etiquettes", "/api/etiquettes/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/bon-receptions", "/api/bon-receptions/**",
                                "/api/dispatch", "/api/dispatch/**",
                                "/api/bon-dispatch", "/api/bon-dispatch/**",
                                "/api/expeditions", "/api/expeditions/**",
                                "/api/tracking", "/api/tracking/**",
                                "/api/docket-records", "/api/docket-records/**",
                                "/api/etiquettes", "/api/etiquettes/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/permissions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/hubs/**", "/api/agences/**", "/api/companies/**", "/api/cities/**", "/api/groups/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAnyAuthority(ROLE_ADMIN_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers("/api/hubs/**", "/api/agences/**", "/api/users/**", "/api/companies/**", "/api/cities/**", "/api/groups/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static boolean isDocketRecordImportRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getServletPath();
        return "/api/docket-records/import-excel".equals(path)
                || "/api/docket-records/import-excel/".equals(path)
                || "/api/docket-records/import-excel/preview".equals(path)
                || "/api/docket-records/import-excel/preview/".equals(path);
    }

    private static boolean isDocketRecordRecoveryDeleteRequest(HttpServletRequest request) {
        if (!"DELETE".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String prefix = "/api/docket-records/recoveries/";
        String path = request.getServletPath();
        if (!path.startsWith(prefix)) {
            return false;
        }

        String recoveryId = path.substring(prefix.length());
        return !recoveryId.isBlank() && !recoveryId.contains("/");
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
