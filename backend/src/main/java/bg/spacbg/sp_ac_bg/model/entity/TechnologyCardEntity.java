package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "technology_cards", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_id", "code"})
})
public class TechnologyCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_account_id", nullable = false)
    private AccountEntity outputAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal outputQuantity = BigDecimal.ONE;

    @Column(length = 20)
    private String outputUnit;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "technologyCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    private List<TechnologyCardStageEntity> stages;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
