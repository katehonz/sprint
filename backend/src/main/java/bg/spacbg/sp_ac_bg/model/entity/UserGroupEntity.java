package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_groups")
public class UserGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean canCreateCompanies = false;

    @Column(nullable = false)
    private boolean canEditCompanies = false;

    @Column(nullable = false)
    private boolean canDeleteCompanies = false;

    @Column(nullable = false)
    private boolean canManageUsers = false;

    @Column(nullable = false)
    private boolean canViewReports = false;

    @Column(nullable = false)
    private boolean canPostEntries = false;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<UserEntity> users;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
