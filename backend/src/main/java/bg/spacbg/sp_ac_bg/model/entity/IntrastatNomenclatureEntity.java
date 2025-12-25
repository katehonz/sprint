package bg.spacbg.sp_ac_bg.model.entity;

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
@Table(name = "intrastat_nomenclatures")
public class IntrastatNomenclatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String cnCode;

    @Column(nullable = false)
    private String descriptionBg;

    private String descriptionEn;

    @Column(nullable = false)
    private String unitOfMeasure;

    @Column(nullable = false)
    private String unitDescription;

    private String parentCode;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "nomenclature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntrastatAccountMappingEntity> accountMappings;

    @OneToMany(mappedBy = "nomenclature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntrastatDeclarationItemEntity> declarationItems;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
