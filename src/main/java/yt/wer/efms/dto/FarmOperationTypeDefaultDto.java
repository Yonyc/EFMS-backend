package yt.wer.efms.dto;

public class FarmOperationTypeDefaultDto {
    private Long operationTypeId;
    private String operationTypeName;
    private Long toolId;
    private String toolName;

    public FarmOperationTypeDefaultDto() {}

    public FarmOperationTypeDefaultDto(Long operationTypeId, String operationTypeName,
                                       Long toolId, String toolName) {
        this.operationTypeId = operationTypeId;
        this.operationTypeName = operationTypeName;
        this.toolId = toolId;
        this.toolName = toolName;
    }

    public Long getOperationTypeId() { return operationTypeId; }
    public void setOperationTypeId(Long operationTypeId) { this.operationTypeId = operationTypeId; }

    public String getOperationTypeName() { return operationTypeName; }
    public void setOperationTypeName(String operationTypeName) { this.operationTypeName = operationTypeName; }

    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
}
