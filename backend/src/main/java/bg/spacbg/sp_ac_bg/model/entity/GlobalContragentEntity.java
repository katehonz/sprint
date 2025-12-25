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
@Table(name = "global_contragents")
public class GlobalContragentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vatNumber;

    private String eik;

    private String registrationNumber;

    private String customerId;

    private String supplierId;

    private String companyName;

    private String companyNameBg;

    private String legalForm;

    private String status;

    private String address;

    private String longAddress;

    private String streetName;

    private String city;

    private String postalCode;

    private String country;

    private String contactPerson;

    private String phone;

    private String email;

    private String website;

    private String contragentType; // Renamed from 'type' as it's a reserved keyword

    private String iban;

    private String bic;

    private String bankName;

    @Column(nullable = false)
    private boolean vatValid;

    @Column(nullable = false)
    private boolean eikValid;

    @Column(nullable = false)
    private boolean valid;

    private OffsetDateTime lastValidatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
