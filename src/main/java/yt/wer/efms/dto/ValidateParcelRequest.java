package yt.wer.efms.dto;

import yt.wer.efms.model.ValidationStatus;

public class ValidateParcelRequest {
    private ValidationStatus validationStatus;
    private String validationNotes;
    private Long farmId;

    public ValidateParcelRequest() {}

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
}
