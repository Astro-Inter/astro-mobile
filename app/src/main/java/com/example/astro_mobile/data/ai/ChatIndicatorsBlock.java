package com.example.astro_mobile.data.ai;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class ChatIndicatorsBlock {
    @SerializedName("titulo") private String title;
    @SerializedName("universo") private String universe;
    @SerializedName("abrangencia") private String coverage;
    @SerializedName("indicadores") private List<Indicator> indicators;
    public String getTitle() { return title; }
    public String getUniverse() { return universe; }
    public String getCoverage() { return coverage; }
    public List<Indicator> getIndicators() { return indicators; }

    public static final class Indicator {
        @SerializedName("chave") private String key;
        @SerializedName("rotulo") private String label;
        @SerializedName("valor") private BigDecimal value;
        @SerializedName("unidade") private String unit;
        @SerializedName("numerador") private BigInteger numerator;
        @SerializedName("denominador") private BigInteger denominator;
        @SerializedName("criterio") private String criterion;
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public BigDecimal getValue() { return value; }
        public String getUnit() { return unit; }
        public BigInteger getNumerator() { return numerator; }
        public BigInteger getDenominator() { return denominator; }
        public String getCriterion() { return criterion; }
    }
}
