package yt.wer.efms.dto;

import yt.wer.efms.model.ValidationStatus;

public class ValidateParcelRequest {
    private ValidationStatus validationStatus;
    private String validationNotes;

    public ValidateParcelRequest() {}

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }
}
