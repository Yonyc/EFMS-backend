package yt.wer.efms.dto;

public class OperationTypeDto {
    private Long id;
    private String name;
    private Long farmId;
    private Long defaultToolId;
    private String defaultToolName;

    public OperationTypeDto() {}

    public OperationTypeDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public OperationTypeDto(Long id, String name, Long farmId, Long defaultToolId, String defaultToolName) {
        this.id = id;
        this.name = name;
        this.farmId = farmId;
        this.defaultToolId = defaultToolId;
        this.defaultToolName = defaultToolName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getDefaultToolId() { return defaultToolId; }
    public void setDefaultToolId(Long defaultToolId) { this.defaultToolId = defaultToolId; }

    public String getDefaultToolName() { return defaultToolName; }
    public void setDefaultToolName(String defaultToolName) { this.defaultToolName = defaultToolName; }
}
