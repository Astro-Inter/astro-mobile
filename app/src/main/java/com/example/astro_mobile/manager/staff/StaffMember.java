package com.example.astro_mobile.manager.staff;

public final class StaffMember {
    public final String name;
    public final String role;
    public final int pendingCount;
    public final int avatarIndex;

    public StaffMember(String name, String role, int pendingCount, int avatarIndex) {
        this.name = name;
        this.role = role;
        this.pendingCount = pendingCount;
        this.avatarIndex = avatarIndex;
    }
}
