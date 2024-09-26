package edu.uob.database;

import edu.uob.utils.DBException;
import edu.uob.utils.ErrorType;
import edu.uob.utils.Utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Database {

    private final String dbPath;
    private final HashMap<String, Table> tables;
    private final DBMetadata metadata;
    public Database(String dbPath, DBMetadata metadata) {
        this.dbPath = dbPath;
        this.metadata = metadata;
        this.tables = new HashMap<>();
        for (String tableName : metadata.getTables().keySet()) {
            tables.put(tableName, null);
        }
    }
    public void loadTable(String tableName) throws IOException {
        if (!tables.containsKey(tableName)) throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        if (tables.get(tableName) != null) return;
        tables.put(tableName, new Table(metadata.getTables().get(tableName)));
        tables.get(tableName).loadTable(Utils.constructFilePath(dbPath, tableName + ".tab"));
    }
    public void saveTable(String tableName) throws IOException {
        if (!tables.containsKey(tableName)) throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        tables.get(tableName).saveTable(Utils.constructFilePath(dbPath, tableName + ".tab"));
    }

    public void dropTable(String tableName) throws IOException {
        if (!tables.containsKey(tableName)) throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        metadata.getTables().remove(tableName);
        tables.remove(tableName);
        Utils.deleteFile(Utils.constructFilePath(dbPath, tableName + ".tab"));
    }

    public void createTable(String tableName, ArrayList<String> attributes) throws IOException {
        if (tables.containsKey(tableName.toLowerCase())) throw new DBException(ErrorType.DUPLICATE_TABLE_CREATION_EXCEPTION);
        Utils.CaseInsensitiveArrayList<String> noRepeatsAttributes = new Utils.CaseInsensitiveArrayList<>();
        noRepeatsAttributes.add("id");
        for (String attribute : attributes) {
            if (noRepeatsAttributes.contains(attribute))
                throw new DBException(ErrorType.TABLE_CREATION_FAILED_EXCEPTION, "TABLE CREATION FAILED. ATTRIBUTES MUST BE UNIQUE)");
            noRepeatsAttributes.add(attribute);
        }
        metadata.getTables().put(tableName.toLowerCase(), new DBMetadata.Table(tableName,"id", 1, noRepeatsAttributes));
        tables.put(tableName.toLowerCase(), new Table(metadata.getTables().get(tableName.toLowerCase())));
        Path tabFile = Paths.get(Utils.constructFilePath(dbPath,tableName.toLowerCase() + ".tab"));
        Files.createFile(tabFile);
        saveTable(tableName.toLowerCase());
    }

    public void addAttribute(String tableName, String attributeName) throws IOException {
        if (!tables.containsKey(tableName)) throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        if (tables.get(tableName) == null) loadTable(tableName);
        tables.get(tableName).addAttribute(attributeName);
    }

    public void dropAttribute(String tableName, String attributeName) throws IOException {
        if (!tables.containsKey(tableName)) throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        if (tables.get(tableName) == null) loadTable(tableName);
        tables.get(tableName).dropAttribute(attributeName);
    }


    public HashMap<String, Table> getTables() {
        return tables;
    }
}
