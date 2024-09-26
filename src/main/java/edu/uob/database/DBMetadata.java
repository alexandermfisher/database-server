package edu.uob.database;//package edu.uob.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.uob.utils.Utils;
import edu.uob.utils.Utils.CaseInsensitiveArrayList;
import java.util.ArrayList;
import java.util.HashMap;

public class DBMetadata {
    private HashMap<String, Table> tables;

    @JsonCreator
    public DBMetadata(@JsonProperty("tables") HashMap<String, Table> tables) {
        this.tables = tables;
    }

    public HashMap<String, Table> getTables() {
        return tables;
    }

    public void setTables(HashMap<String, Table> tables) {
        this.tables = tables;
    }

    @Override
    public String toString() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static class Table {
        private final String originalTableName;
        private final String primaryKey;
        private int nextPrimaryKey;
        private CaseInsensitiveArrayList<String> attributes;

        @JsonCreator
        public Table(@JsonProperty("originalTableName") String originalTableName,
                     @JsonProperty("primaryKey") String primaryKey,
                     @JsonProperty("nextPrimaryKey") int nextPrimaryKey,
                     @JsonProperty("attributes") CaseInsensitiveArrayList<String> attributes) {
            this.originalTableName = originalTableName;
            this.primaryKey = primaryKey;
            this.nextPrimaryKey = nextPrimaryKey;
            this.attributes = attributes;
        }

        public String getPrimaryKey() {
            return primaryKey;
        }
        public int getNextPrimaryKey() {
            return nextPrimaryKey;
        }
        public String getOriginalTableName() {return originalTableName; }
        public void incrementNextPrimaryKey() {
            this.nextPrimaryKey++;
        }
        public CaseInsensitiveArrayList<String> getAttributes() { return attributes; }
        public void setAttributes(CaseInsensitiveArrayList<String> attributes) {
            this.attributes = attributes;
        }
    }
}


