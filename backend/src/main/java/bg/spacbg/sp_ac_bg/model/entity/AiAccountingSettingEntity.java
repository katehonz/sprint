package bg.spacbg.sp_ac_bg.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_accounting_settings")
public class AiAccountingSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private String salesRevenueAccount;

    @Column(nullable = false)
    private String salesServicesAccount;

    @Column(nullable = false)
    private String salesReceivablesAccount;

    @Column(nullable = false)
    private String purchaseExpenseAccount;

    @Column(nullable = false)
    private String purchasePayablesAccount;

    @Column(nullable = false)
    private String vatInputAccount;

    @Column(nullable = false)
    private String vatOutputAccount;

    private String nonRegisteredPersonsAccount;

    @Column(nullable = false)
    private String nonRegisteredVatOperation;

    @Column(nullable = false)
    private Integer accountCodeLength;

    @Column(nullable = false)
    private String salesDescriptionTemplate;

    @Column(nullable = false)
    private String purchaseDescriptionTemplate;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
