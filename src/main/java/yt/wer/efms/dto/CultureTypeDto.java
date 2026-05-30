package yt.wer.efms.dto;

public class CultureTypeDto {
    private Long id;
    private String code;
    private String name;
    private String nameNl;
    private String category;
    private String color;
    private Long farmId;

    public CultureTypeDto() {}

    public CultureTypeDto(Long id, String code, String name, String nameNl, String category, String color, Long farmId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.nameNl = nameNl;
        this.category = category;
        this.color = color;
        this.farmId = farmId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameNl() { return nameNl; }
    public void setNameNl(String nameNl) { this.nameNl = nameNl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
}
