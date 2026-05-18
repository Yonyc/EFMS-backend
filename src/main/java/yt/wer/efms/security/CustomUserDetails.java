package yt.wer.efms.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {
    private final Long userId;
    private final boolean isAdmin;

    public CustomUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Long userId, boolean isAdmin) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.isAdmin = isAdmin;
    }

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long userId, boolean isAdmin) {
        super(username, password, authorities);
        this.userId = userId;
        this.isAdmin = isAdmin;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
