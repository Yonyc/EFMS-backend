package yt.wer.efms.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private static User user(Long id, String username, boolean admin) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("stored-hash");
        u.setAdmin(admin);
        return u;
    }

    @Test
    void loadsNonAdminUser_withRoleUserOnly() {
        when(userRepository.findByUsername("arnaud")).thenReturn(Optional.of(user(7L, "arnaud", false)));

        UserDetails details = service.loadUserByUsername("arnaud");

        assertEquals("arnaud", details.getUsername());
        assertEquals("stored-hash", details.getPassword());
        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(details.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        CustomUserDetails custom = (CustomUserDetails) details;
        assertEquals(7L, custom.getUserId());
        assertTrue(!custom.isAdmin());
    }

    @Test
    void loadsAdminUser_withRoleAdmin() {
        when(userRepository.findByUsername("boss")).thenReturn(Optional.of(user(1L, "boss", true)));

        UserDetails details = service.loadUserByUsername("boss");

        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(((CustomUserDetails) details).isAdmin());
    }

    @Test
    void throwsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }
}
