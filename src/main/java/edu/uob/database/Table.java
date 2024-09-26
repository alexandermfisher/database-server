package edu.uob.database;

import edu.uob.utils.DBException;
import edu.uob.utils.ErrorType;
import edu.uob.utils.Utils.CaseInsensitiveArrayList;
import java.io.*;
import java.util.*;

public class Table {
    private final HashMap<Integer, HashMap<String, String>> records;
    private final String originalTableName;
    private final String primaryKey;
    private int nextPrimaryKey;
    private final CaseInsensitiveArrayList<String> attributes;
    public Table(String originalTableName, String primaryKey, int nextPrimaryKey, CaseInsensitiveArrayList<String> attributes) {
        this.originalTableName = originalTableName;
        this.primaryKey = primaryKey;
        this.nextPrimaryKey = nextPrimaryKey;
        this.attributes = attributes;
        this.records = new HashMap<>();
    }
    // Method to instantiate the Table using metadata
    public Table(DBMetadata.Table metadata) {
        this(metadata.getOriginalTableName(), metadata.getPrimaryKey(), metadata.getNextPrimaryKey(), metadata.getAttributes());
    }
    public void loadTable(String tableDataFilePath) throws IOException {
        BufferedReader buffer;
        buffer = new BufferedReader(new FileReader(tableDataFilePath));

        String line;
        boolean firstLine = true; // Flag to indicate whether it's the first line
        while ((line = buffer.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            String[] data = line.split("\t");
            HashMap<String, String> record = new HashMap<>();
            for (int i = 0; i < data.length; i++) {
                record.put(attributes.get(i).toLowerCase(), data[i]);
            }
            addRecord(Integer.parseInt(record.get("id")), record);
        }
    }
    public void saveTable(String tableDataFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(tableDataFilePath));

        // Write header
        StringBuilder headerBuilder = new StringBuilder();
        for (String attribute : attributes) { headerBuilder.append(attribute).append("\t"); }
        writer.write(headerBuilder.toString().trim());
        writer.newLine();

        // Write records
        for (Map.Entry<Integer, HashMap<String, String>> entry : records.entrySet()) {
            StringBuilder recordBuilder = new StringBuilder();
            for (String attribute : attributes) {
                recordBuilder.append(entry.getValue().getOrDefault(attribute.toLowerCase(), "")).append("\t");
            }
            writer.write(recordBuilder.toString().trim());
            writer.newLine();
        }
        writer.close();
    }
    public void addAttribute(String attribute) {
        if (attributes.contains(attribute)) throw new DBException(ErrorType.DUPLICATE_ATTRIBUTE_EXCEPTION);
        attributes.add(attribute);
        for (HashMap<String, String> record : records.values()) record.put(attribute.toLowerCase(), "NULL");
    }
    public void dropAttribute(String attribute) {
        if (!attributes.contains(attribute)) throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
        if (attribute.equalsIgnoreCase("id")) throw new DBException(ErrorType.PK_DROP_EXCEPTION);
        attributes.remove(attribute);
        for (HashMap<String, String> record : records.values()) record.remove(attribute.toLowerCase());
    }
    public void addRecord(int id, HashMap<String, String> record) {
        validateKeySet(record.keySet());
        records.put(id, record);
    }
    public void addRecord(HashMap<String, String> record) {
        validateKeySet(record.keySet());
        record.put("id", Integer.toString(nextPrimaryKey));
        records.put(nextPrimaryKey, record);
        nextPrimaryKey++;
    }
    public void updateRecord(int id, HashMap<String, String> newRecord) {
        if (!records.containsKey(id)) throw new DBException(ErrorType.INVALID_PRIMARY_KEY_EXCEPTION);
        validateKeySet(newRecord.keySet());
        for (String key : newRecord.keySet()) {
            records.get(id).replace(key, newRecord.get(key));
        }
    }
    public void deleteRecords(Set<Integer> ids) {
        for (Integer id : ids) { records.remove(id); }
    }
    public HashMap<String, String> getRecord(int id) {
        if (!records.containsKey(id)) throw new DBException(ErrorType.INVALID_PRIMARY_KEY_EXCEPTION);
        return new HashMap<>(records.get(id));
    }
    public HashMap<String, String> getRecord(int id, ArrayList<String> attributes) {
        if (!records.containsKey(id)) throw new DBException(ErrorType.INVALID_PRIMARY_KEY_EXCEPTION);
        validateKeySet(attributes);
        HashMap<String, String> requestedRecord = new HashMap<>();
        for (String attribute : attributes) { requestedRecord.put(attribute, records.get(id).get(attribute)); }
        return requestedRecord;
    }
    public ArrayList<HashMap<String, String>> getRecords(ArrayList<String> attributes) {
        validateKeySet(attributes);
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        for (Integer id : records.keySet()) {
            HashMap<String, String> record = getRecord(id, attributes);
            result.add(record);
        }
        return result;
    }
    public HashMap<Integer, HashMap<String, String>> getRecords() { return records; }
    public String getPrimaryKey() { return primaryKey; }
    public String getOriginalTableName() {return this.originalTableName; }
    public int getNextPrimaryKey() { return nextPrimaryKey; }
    public CaseInsensitiveArrayList<String> getAttributes() { return attributes; }
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for (int id : records.keySet()) {
            string.append("Record ID: ").append(id).append("\n");
            HashMap<String, String> record = records.get(id);
            for (String attribute : attributes) {
                string.append(attribute).append(": ").append(record.get(attribute)).append("\n");
            }
            string.append("\n");
        }
        return string.toString();
    }
    private void validateKeySet(Collection<String> attributes) {
        for (String attribute : attributes) {
            if (!this.attributes.contains(attribute)) throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
        }
    }
}
