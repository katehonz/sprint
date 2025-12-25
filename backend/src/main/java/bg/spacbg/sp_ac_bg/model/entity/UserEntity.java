package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @ManyToOne(fetch = FetchType.EAGER) // Eager fetch for authorities
    @JoinColumn(name = "group_id")
    private UserGroupEntity group;

    @Column(nullable = false)
    private boolean isActive = true;

    // Personal input periods - Documents
    @Column(nullable = false)
    private LocalDate documentPeriodStart;

    @Column(nullable = false)
    private LocalDate documentPeriodEnd;

    @Column(nullable = false)
    private boolean documentPeriodActive = true;

    // Personal input periods - Accounting
    @Column(nullable = false)
    private LocalDate accountingPeriodStart;

    @Column(nullable = false)
    private LocalDate accountingPeriodEnd;

    @Column(nullable = false)
    private boolean accountingPeriodActive = true;

    // Personal input periods - VAT
    @Column(nullable = false)
    private LocalDate vatPeriodStart;

    @Column(nullable = false)
    private LocalDate vatPeriodEnd;

    @Column(nullable = false)
    private boolean vatPeriodActive = true;
    
    private String recoveryCodeHash;
    
    private OffsetDateTime recoveryCodeCreatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.group != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + this.group.getName().toUpperCase()));
        }
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
