package com.example.astro_mobile.data.api.dto;
import java.util.List;
public final class ReceivedFormsPage {
    public List<Item> items;
    public int page;
    public long totalPages;
    public static final class Item {
        public String id, title, description, dueDate, status, reviewStatus;
        public Integer nrCode;
        public Long unitId;
        public boolean viewed;
    }
}
