package com.example.astro_mobile.manager.staff;

import com.example.astro_mobile.data.api.dto.CollaboratorSummaryData;

public final class StaffMember {
    public final long id;
    public final String name;
    public final String email;
    public final String role;
    public final String unit;
    public final String photoUrl;
    public final String initials;
    public final long pendingCount;

    public StaffMember(CollaboratorSummaryData data) {
        id = data.id;
        name = data.name.trim();
        email = null;
        role = data.roleName == null ? "" : data.roleName;
        unit = null;
        photoUrl = data.profilePhotoUrl;
        pendingCount = data.pendingCount;
        initials = initialsFrom(name);
    }

    private String initialsFrom(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
