package com.example.astro_mobile.data.ai;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Matriz longa: cada linha é um par recebido, sem inferir células ausentes. */
public final class ChatMatrixBlock extends ChatDatasetSummary {
    @SerializedName("chave_pessoa") private String personKey;
    @SerializedName("chave_categoria") private String categoryKey;
    @SerializedName("chave_situacao") private String stateKey;
    @SerializedName("colunas") private List<Column> columns;
    @SerializedName("linhas") private List<Map<String, JsonElement>> rows;
    public String getPersonKey() { return personKey; }
    public String getCategoryKey() { return categoryKey; }
    public String getStateKey() { return stateKey; }
    public List<Column> getColumns() { return columns; }
    public List<Map<String, JsonElement>> getRows() { return rows; }

    public static final class Column {
        @SerializedName("chave") private String key;
        @SerializedName("titulo") private String title;
        public String getKey() { return key; }
        public String getTitle() { return title; }
    }

    public static final class CellKey {
        private final BigInteger person;
        private final int nr;
        public CellKey(BigInteger person, int nr) { this.person = person; this.nr = nr; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof CellKey)) return false;
            CellKey key = (CellKey) other;
            return person.equals(key.person) && nr == key.nr;
        }
        @Override public int hashCode() { return Objects.hash(person, nr); }
    }
}
