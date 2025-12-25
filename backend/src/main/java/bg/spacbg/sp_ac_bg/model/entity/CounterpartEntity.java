package bg.spacbg.sp_ac_bg.model.entity;

import bg.spacbg.sp_ac_bg.model.enums.CounterpartType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "counterparts")
public class CounterpartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String eik;

    private String vatNumber;

    private String street;

    private String address;

    /**
     * Full address as returned by VIES (single line format).
     */
    private String longAddress;

    private String city;

    private String postalCode;

    private String country;

    private String phone;

    private String email;

    private String contactPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounterpartType counterpartType;

    @Column(nullable = false)
    private boolean isCustomer;

    @Column(nullable = false)
    private boolean isSupplier;

    @Column(nullable = false)
    private boolean isVatRegistered;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @OneToMany(mappedBy = "counterpart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntryLineEntity> entryLines;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
