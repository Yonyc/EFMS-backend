package yt.wer.efms.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ParcelShareRoleConverter implements AttributeConverter<ParcelShareRole, String> {
    @Override
    public String convertToDatabaseColumn(ParcelShareRole role) {
        if (role == null) return null;
        return role.name().toLowerCase();
    }

    @Override
    public ParcelShareRole convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData.toLowerCase()) {
            case "editor", "share" -> ParcelShareRole.EDITOR;
            case "viewer", "user" -> ParcelShareRole.VIEWER;
            default -> ParcelShareRole.valueOf(dbData.toUpperCase());
        };
    }
}