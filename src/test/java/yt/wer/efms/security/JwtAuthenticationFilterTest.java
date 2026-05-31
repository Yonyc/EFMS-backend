package yt.wer.efms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static UserDetails userDetails(String username) {
        return new CustomUserDetails(username, "pwd", Collections.emptyList(), 1L, false);
    }

    @Test
    void noAuthHeader_doesNotAuthenticate_andProceeds() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void nonBearerHeader_isIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        UserDetails details = userDetails("arnaud");
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtUtil.extractUsername("good-token")).thenReturn("arnaud");
        when(userDetailsService.loadUserByUsername("arnaud")).thenReturn(details);
        when(jwtUtil.validateToken("good-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(details, auth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer stale-token");
        when(jwtUtil.extractUsername("stale-token")).thenReturn("arnaud");
        when(userDetailsService.loadUserByUsername("arnaud")).thenReturn(userDetails("arnaud"));
        when(jwtUtil.validateToken("stale-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unknownUser_clearsContext_andProceeds() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ghost-token");
        when(jwtUtil.extractUsername("ghost-token")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("nope"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        Authentication preset = new TestingAuthenticationToken("already-logged-in", null);
        SecurityContextHolder.getContext().setAuthentication(preset);

        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtUtil.extractUsername("good-token")).thenReturn("arnaud");

        filter.doFilterInternal(request, response, filterChain);

        assertSame(preset, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }
}
