package yt.wer.efms.dto;

public class PhytoImportRequest {
    private String filePath;
    private String versionTag;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
}
