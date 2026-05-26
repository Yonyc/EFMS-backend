package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private String name;

    @Column(name = "official")
    private Boolean official;

    @Column(name = "official_current")
    private Boolean officialCurrent;

    @Column(name = "official_auth_number")
    private String officialAuthNumber;

    @Column(name = "official_version_tag")
    private String officialVersionTag;

    @Column(name = "official_decision_code")
    private String officialDecisionCode;

    @Column(name = "official_decision_code_en")
    private String officialDecisionCodeEn;

    @Column(name = "official_date_first_authorization")
    private String officialDateFirstAuthorization;

    @Column(name = "official_date_from")
    private String officialDateFrom;

    @Column(name = "official_date_to")
    private String officialDateTo;

    @Column(name = "official_user_group_code")
    private String officialUserGroupCode;

    @Column(name = "official_user_group_en")
    private String officialUserGroupEn;

    @Column(name = "official_formulation_type_code")
    private String officialFormulationTypeCode;

    @Column(name = "official_formulation_type_en")
    private String officialFormulationTypeEn;

    @Column(name = "official_product_type_codes", columnDefinition = "TEXT")
    private String officialProductTypeCodes;

    @Column(name = "official_product_type_en", columnDefinition = "TEXT")
    private String officialProductTypeEn;

    @Column(name = "official_decision_code_fr")
    private String officialDecisionCodeFr;

    @Column(name = "official_holder_name")
    private String officialHolderName;

    @Column(name = "official_active_substances", columnDefinition = "TEXT")
    private String officialActiveSubstances;

    @Column(name = "official_sale_to")
    private String officialSaleTo;

    @Column(name = "official_use_tolerated_to")
    private String officialUseToleratedTo;

    @Column(name = "official_product_type_fr", columnDefinition = "TEXT")
    private String officialProductTypeFr;

    @Column(name = "official_user_group_fr")
    private String officialUserGroupFr;

    @Column(name = "official_formulation_type_fr")
    private String officialFormulationTypeFr;

    @Column(name = "official_payload_json", columnDefinition = "TEXT")
    private String officialPayloadJson;

    @Column(name = "official_imported_at")
    private LocalDateTime officialImportedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id")
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_operation_type_id")
    private OperationType defaultOperationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_tool_id")
    private Tool overrideTool;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "picture_url", length = 2048)
    private String pictureUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getOfficial() { return official; }
    public void setOfficial(Boolean official) { this.official = official; }

    public Boolean getOfficialCurrent() { return officialCurrent; }
    public void setOfficialCurrent(Boolean officialCurrent) { this.officialCurrent = officialCurrent; }

    public String getOfficialAuthNumber() { return officialAuthNumber; }
    public void setOfficialAuthNumber(String officialAuthNumber) { this.officialAuthNumber = officialAuthNumber; }

    public String getOfficialVersionTag() { return officialVersionTag; }
    public void setOfficialVersionTag(String officialVersionTag) { this.officialVersionTag = officialVersionTag; }

    public String getOfficialDecisionCode() { return officialDecisionCode; }
    public void setOfficialDecisionCode(String officialDecisionCode) { this.officialDecisionCode = officialDecisionCode; }

    public String getOfficialDecisionCodeEn() { return officialDecisionCodeEn; }
    public void setOfficialDecisionCodeEn(String officialDecisionCodeEn) { this.officialDecisionCodeEn = officialDecisionCodeEn; }

    public String getOfficialDateFirstAuthorization() { return officialDateFirstAuthorization; }
    public void setOfficialDateFirstAuthorization(String officialDateFirstAuthorization) { this.officialDateFirstAuthorization = officialDateFirstAuthorization; }

    public String getOfficialDateFrom() { return officialDateFrom; }
    public void setOfficialDateFrom(String officialDateFrom) { this.officialDateFrom = officialDateFrom; }

    public String getOfficialDateTo() { return officialDateTo; }
    public void setOfficialDateTo(String officialDateTo) { this.officialDateTo = officialDateTo; }

    public String getOfficialUserGroupCode() { return officialUserGroupCode; }
    public void setOfficialUserGroupCode(String officialUserGroupCode) { this.officialUserGroupCode = officialUserGroupCode; }

    public String getOfficialUserGroupEn() { return officialUserGroupEn; }
    public void setOfficialUserGroupEn(String officialUserGroupEn) { this.officialUserGroupEn = officialUserGroupEn; }

    public String getOfficialFormulationTypeCode() { return officialFormulationTypeCode; }
    public void setOfficialFormulationTypeCode(String officialFormulationTypeCode) { this.officialFormulationTypeCode = officialFormulationTypeCode; }

    public String getOfficialFormulationTypeEn() { return officialFormulationTypeEn; }
    public void setOfficialFormulationTypeEn(String officialFormulationTypeEn) { this.officialFormulationTypeEn = officialFormulationTypeEn; }

    public String getOfficialProductTypeCodes() { return officialProductTypeCodes; }
    public void setOfficialProductTypeCodes(String officialProductTypeCodes) { this.officialProductTypeCodes = officialProductTypeCodes; }

    public String getOfficialProductTypeEn() { return officialProductTypeEn; }
    public void setOfficialProductTypeEn(String officialProductTypeEn) { this.officialProductTypeEn = officialProductTypeEn; }

    public String getOfficialDecisionCodeFr() { return officialDecisionCodeFr; }
    public void setOfficialDecisionCodeFr(String officialDecisionCodeFr) { this.officialDecisionCodeFr = officialDecisionCodeFr; }

    public String getOfficialHolderName() { return officialHolderName; }
    public void setOfficialHolderName(String officialHolderName) { this.officialHolderName = officialHolderName; }

    public String getOfficialActiveSubstances() { return officialActiveSubstances; }
    public void setOfficialActiveSubstances(String officialActiveSubstances) { this.officialActiveSubstances = officialActiveSubstances; }

    public String getOfficialSaleTo() { return officialSaleTo; }
    public void setOfficialSaleTo(String officialSaleTo) { this.officialSaleTo = officialSaleTo; }

    public String getOfficialUseToleratedTo() { return officialUseToleratedTo; }
    public void setOfficialUseToleratedTo(String officialUseToleratedTo) { this.officialUseToleratedTo = officialUseToleratedTo; }

    public String getOfficialProductTypeFr() { return officialProductTypeFr; }
    public void setOfficialProductTypeFr(String officialProductTypeFr) { this.officialProductTypeFr = officialProductTypeFr; }

    public String getOfficialUserGroupFr() { return officialUserGroupFr; }
    public void setOfficialUserGroupFr(String officialUserGroupFr) { this.officialUserGroupFr = officialUserGroupFr; }

    public String getOfficialFormulationTypeFr() { return officialFormulationTypeFr; }
    public void setOfficialFormulationTypeFr(String officialFormulationTypeFr) { this.officialFormulationTypeFr = officialFormulationTypeFr; }

    public String getOfficialPayloadJson() { return officialPayloadJson; }
    public void setOfficialPayloadJson(String officialPayloadJson) { this.officialPayloadJson = officialPayloadJson; }

    public LocalDateTime getOfficialImportedAt() { return officialImportedAt; }
    public void setOfficialImportedAt(LocalDateTime officialImportedAt) { this.officialImportedAt = officialImportedAt; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public OperationType getDefaultOperationType() { return defaultOperationType; }
    public void setDefaultOperationType(OperationType defaultOperationType) { this.defaultOperationType = defaultOperationType; }

    public Tool getOverrideTool() { return overrideTool; }
    public void setOverrideTool(Tool overrideTool) { this.overrideTool = overrideTool; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
}
