package yt.wer.efms.model;

public enum ValidationStatus {
    PENDING,    // Awaiting review
    APPROVED,   // Validated and ready to convert
    REJECTED,   // Rejected by user
    CONVERTED   // Already converted to a Parcel
}
