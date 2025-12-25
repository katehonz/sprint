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
@Table(name = "controlisy_imports")
public class ControlisyImportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false)
    private OffsetDateTime importDate;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String documentType;

    @Column(columnDefinition = "text")
    private String rawXml;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String parsedData;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean processed;

    private String errorMessage;

    private Integer importedDocuments;

    private Integer importedContractors;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
