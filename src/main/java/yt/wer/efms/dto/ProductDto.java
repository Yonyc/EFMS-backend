package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProductDto {
    private Long id;
    private String name;
    private Long productTypeId;
    private Long cultureTypeId;
    private String cultureTypeName;
    private String cultureTypeColor;
    private Long unitId;
    private Long farmId;
    private Long defaultOperationTypeId;
    private String defaultOperationTypeName;
    private Long overrideToolId;
    private String overrideToolName;
    private String description;
    private String pictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean official;
    private Boolean officialCurrent;
    private String officialAuthNumber;
    private String officialVersionTag;
    private String officialDecisionCode;
    private String officialDecisionCodeEn;
    private String officialDateFirstAuthorization;
    private String officialDateFrom;
    private String officialDateTo;
    private String officialUserGroupCode;
    private String officialUserGroupEn;
    private String officialFormulationTypeCode;
    private String officialFormulationTypeEn;
    private String officialProductTypeCodes;
    private String officialProductTypeEn;
    private String officialDecisionCodeFr;
    private String officialHolderName;
    private String officialActiveSubstances;
    private String officialSaleTo;
    private String officialUseToleratedTo;
    private String officialProductTypeFr;
    private String officialUserGroupFr;
    private String officialFormulationTypeFr;
    private LocalDateTime officialImportedAt;

    public ProductDto() {}

    public ProductDto(Long id, String name, Long productTypeId, Long unitId, Long farmId, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.name = name;
        this.productTypeId = productTypeId;
        this.unitId = unitId;
        this.farmId = farmId;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getProductTypeId() { return productTypeId; }
    public void setProductTypeId(Long productTypeId) { this.productTypeId = productTypeId; }

    public Long getCultureTypeId() { return cultureTypeId; }
    public void setCultureTypeId(Long cultureTypeId) { this.cultureTypeId = cultureTypeId; }

    public String getCultureTypeName() { return cultureTypeName; }
    public void setCultureTypeName(String cultureTypeName) { this.cultureTypeName = cultureTypeName; }

    public String getCultureTypeColor() { return cultureTypeColor; }
    public void setCultureTypeColor(String cultureTypeColor) { this.cultureTypeColor = cultureTypeColor; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getDefaultOperationTypeId() { return defaultOperationTypeId; }
    public void setDefaultOperationTypeId(Long defaultOperationTypeId) { this.defaultOperationTypeId = defaultOperationTypeId; }

    public String getDefaultOperationTypeName() { return defaultOperationTypeName; }
    public void setDefaultOperationTypeName(String defaultOperationTypeName) { this.defaultOperationTypeName = defaultOperationTypeName; }

    public Long getOverrideToolId() { return overrideToolId; }
    public void setOverrideToolId(Long overrideToolId) { this.overrideToolId = overrideToolId; }

    public String getOverrideToolName() { return overrideToolName; }
    public void setOverrideToolName(String overrideToolName) { this.overrideToolName = overrideToolName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

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

    public LocalDateTime getOfficialImportedAt() { return officialImportedAt; }
    public void setOfficialImportedAt(LocalDateTime officialImportedAt) { this.officialImportedAt = officialImportedAt; }

    private List<AttachmentDto> attachments;
    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }
}
