package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class ImportSourceFileDto {
    private Long id;
    private String filename;
    private LocalDateTime importedAt;
    private int parcelCount;
    private int toolCount;
    private int productCount;
    private int operationCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public int getParcelCount() { return parcelCount; }
    public void setParcelCount(int parcelCount) { this.parcelCount = parcelCount; }

    public int getToolCount() { return toolCount; }
    public void setToolCount(int toolCount) { this.toolCount = toolCount; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public int getOperationCount() { return operationCount; }
    public void setOperationCount(int operationCount) { this.operationCount = operationCount; }
}
