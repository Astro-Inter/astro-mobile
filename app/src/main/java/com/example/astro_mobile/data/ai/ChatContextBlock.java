package com.example.astro_mobile.data.ai;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public final class ChatContextBlock {
    @SerializedName("fonte") private String source;
    @SerializedName("referencia") private String reference;
    @SerializedName("inicio") private String start;
    @SerializedName("fim") private String end;
    @SerializedName("escopo") private String scope;
    @SerializedName("filtros") private Map<String, JsonElement> filters;
    @SerializedName("criterios") private List<String> criteria;
    @SerializedName("avisos") private List<String> warnings;
    @SerializedName("conjunto") private ChatDatasetSummary dataset;
    public String getSource() { return source; }
    public String getReference() { return reference; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getScope() { return scope; }
    public Map<String, JsonElement> getFilters() { return filters; }
    public List<String> getCriteria() { return criteria; }
    public List<String> getWarnings() { return warnings; }
    public ChatDatasetSummary getDataset() { return dataset; }
}
