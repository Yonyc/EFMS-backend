package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ImportPreviewDto {
    private Long importId;
    private String importName;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private boolean hasActionData;
    private int parcelCount;
    private int equipmentCount;
    private int productCount;
    private int activityCount;
    private List<ImportSourceFileDto> files;

    public Long getImportId() { return importId; }
    public void setImportId(Long importId) { this.importId = importId; }

    public String getImportName() { return importName; }
    public void setImportName(String importName) { this.importName = importName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public boolean isHasActionData() { return hasActionData; }
    public void setHasActionData(boolean hasActionData) { this.hasActionData = hasActionData; }

    public int getParcelCount() { return parcelCount; }
    public void setParcelCount(int parcelCount) { this.parcelCount = parcelCount; }

    public int getEquipmentCount() { return equipmentCount; }
    public void setEquipmentCount(int equipmentCount) { this.equipmentCount = equipmentCount; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public int getActivityCount() { return activityCount; }
    public void setActivityCount(int activityCount) { this.activityCount = activityCount; }

    public List<ImportSourceFileDto> getFiles() { return files; }
    public void setFiles(List<ImportSourceFileDto> files) { this.files = files; }


    public static class EquipmentDto {
        private Long id;
        private String sourceGuid;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSourceGuid() { return sourceGuid; }
        public void setSourceGuid(String sourceGuid) { this.sourceGuid = sourceGuid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ProductDto {
        private Long id;
        private String sourceGuid;
        private String name;
        private String code;
        private String unitSymbol;
        private String botanicalSpecies;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSourceGuid() { return sourceGuid; }
        public void setSourceGuid(String sourceGuid) { this.sourceGuid = sourceGuid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getUnitSymbol() { return unitSymbol; }
        public void setUnitSymbol(String unitSymbol) { this.unitSymbol = unitSymbol; }

        public String getBotanicalSpecies() { return botanicalSpecies; }
        public void setBotanicalSpecies(String botanicalSpecies) { this.botanicalSpecies = botanicalSpecies; }
    }

    public static class ActivityDto {
        private Long id;
        private String operationName;
        private String operationCategory;
        private String startingDate;
        private Integer durationMinutes;
        private String comment;
        private String periodName;
        private List<ParcelRefDto> parcels;
        private List<ProductUsageDto> productUsages;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getOperationName() { return operationName; }
        public void setOperationName(String operationName) { this.operationName = operationName; }

        public String getOperationCategory() { return operationCategory; }
        public void setOperationCategory(String operationCategory) { this.operationCategory = operationCategory; }

        public String getStartingDate() { return startingDate; }
        public void setStartingDate(String startingDate) { this.startingDate = startingDate; }

        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getPeriodName() { return periodName; }
        public void setPeriodName(String periodName) { this.periodName = periodName; }

        public List<ParcelRefDto> getParcels() { return parcels; }
        public void setParcels(List<ParcelRefDto> parcels) { this.parcels = parcels; }

        public List<ProductUsageDto> getProductUsages() { return productUsages; }
        public void setProductUsages(List<ProductUsageDto> productUsages) { this.productUsages = productUsages; }
    }

    public static class ParcelRefDto {
        private String plotId;
        private String parcelName;
        private Double workedSurfaceHa;

        public String getPlotId() { return plotId; }
        public void setPlotId(String plotId) { this.plotId = plotId; }

        public String getParcelName() { return parcelName; }
        public void setParcelName(String parcelName) { this.parcelName = parcelName; }

        public Double getWorkedSurfaceHa() { return workedSurfaceHa; }
        public void setWorkedSurfaceHa(Double workedSurfaceHa) { this.workedSurfaceHa = workedSurfaceHa; }
    }

    public static class ProductUsageDto {
        private String supplyId;
        private String supplyName;
        private Double quantity;
        private String unitSymbol;

        public String getSupplyId() { return supplyId; }
        public void setSupplyId(String supplyId) { this.supplyId = supplyId; }

        public String getSupplyName() { return supplyName; }
        public void setSupplyName(String supplyName) { this.supplyName = supplyName; }

        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }

        public String getUnitSymbol() { return unitSymbol; }
        public void setUnitSymbol(String unitSymbol) { this.unitSymbol = unitSymbol; }
    }
}
