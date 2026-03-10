package yt.wer.efms.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) return null;
        return role.name().toLowerCase();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData.toLowerCase()) {
            case "admin" -> Role.ADMIN;
            case "share", "editor" -> Role.EDITOR;
            case "user", "viewer" -> Role.VIEWER;
            default -> Role.valueOf(dbData.toUpperCase());
        };
    }
}