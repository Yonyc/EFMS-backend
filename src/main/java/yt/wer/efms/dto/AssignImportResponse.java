package yt.wer.efms.dto;

public class AssignImportResponse {
    private Long importId;
    private Long farmId;
    private int convertedCount;
    private int skippedCount;

    public AssignImportResponse() {}

    public AssignImportResponse(Long importId, Long farmId, int convertedCount, int skippedCount) {
        this.importId = importId;
        this.farmId = farmId;
        this.convertedCount = convertedCount;
        this.skippedCount = skippedCount;
    }

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public int getConvertedCount() {
        return convertedCount;
    }

    public void setConvertedCount(int convertedCount) {
        this.convertedCount = convertedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
}
