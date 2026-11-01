package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;
import java.math.BigInteger;

/** Metadados do conjunto recebido, independentes das páginas visuais. */
public class ChatDatasetSummary {
    @SerializedName("solicitada") private BigInteger requested;
    @SerializedName("retornada") private int returned;
    @SerializedName("total") private BigInteger total;
    @SerializedName("limitado") private boolean limited;
    @SerializedName("tem_mais") private boolean hasMore;
    @SerializedName("motivo_limite") private String limitReason;
    @SerializedName("unidade_total") private String totalUnit;

    public BigInteger getRequested() { return requested; }
    public int getReturned() { return returned; }
    public BigInteger getTotal() { return total; }
    public boolean isLimited() { return limited; }
    public boolean hasMore() { return hasMore; }
    public String getLimitReason() { return limitReason; }
    public String getTotalUnit() { return totalUnit; }
}
