package yt.wer.efms.dto;

public class UpdateImportedParcelRequest {
    private String geodata;
    private String validationNotes;

    public String getGeodata() {
        return geodata;
    }

    public void setGeodata(String geodata) {
        this.geodata = geodata;
    }

    public String getValidationNotes() {
        return validationNotes;
    }

    public void setValidationNotes(String validationNotes) {
        this.validationNotes = validationNotes;
    }
}
