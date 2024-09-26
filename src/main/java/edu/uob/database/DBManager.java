package edu.uob.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uob.utils.DBException;
import edu.uob.utils.ErrorType;
import edu.uob.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static edu.uob.utils.Utils.deleteDirectory;
import static java.nio.file.Files.exists;

public class DBManager {
    private static DBManager instance;
    private final String storageFolderPath;
    private String databaseName = null;
    private DBMetadata databaseMetadata = null;
    private Database database = null;
    private final ObjectMapper mapper;
    private DBManager(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
        this.mapper = new ObjectMapper();
        mapper.enable(INDENT_OUTPUT);
    }
    public static DBManager getInstance(String storageFolderPath) {
        if (instance == null) instance = new DBManager(storageFolderPath);
        return instance;
    }
    public boolean databaseExists(String databaseName) {
        // check to see if directory exists:
        Path directory = Paths.get(storageFolderPath, databaseName);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) return false;

        // check to see if metadata exits for database:
        Path metadata = Paths.get(String.valueOf(directory), "metadata.json");
        return Files.exists(metadata);
    }
    public void useDatabase(String databaseName) throws IOException {
        if (!databaseExists(databaseName)) throw new DBException(ErrorType.DATABASE_NOT_FOUND_EXCEPTION);
        closeDatabase();
        this.databaseName = databaseName;
        loadMetadata();
        this.database = new Database(Utils.constructDirectoryPath(storageFolderPath, databaseName), this.databaseMetadata);
    }
    public void createDatabase(String databaseName, DBMetadata metadata) throws IOException {
        if (databaseExists(databaseName)) throw new DBException(ErrorType.DUPLICATE_DATABASE_CREATION_EXCEPTION);
        Path directory = Paths.get(storageFolderPath, databaseName);
        Files.createDirectory(directory);
        Path metadataFile = Paths.get(directory.toString(), "metadata.json");
        Files.createFile(metadataFile);
        if (metadata != null)  mapper.writeValue(metadataFile.toFile(), metadata);
    }
    public void closeDatabase() throws IOException {
        if (databaseName == null) return;
        saveDatabase();
        databaseName = null;
        databaseMetadata = null;
        database = null;
    }
    public void saveDatabase() throws IOException {
        if (databaseName == null) return;

        // Save: Write current database's metadata object to file as JSON:
        Path directory = Paths.get(storageFolderPath, databaseName);
        Path metadataFile = Paths.get(directory.toString(), "metadata.json");
        mapper.writeValue(metadataFile.toFile(), databaseMetadata);

        // close each table that is currently open:
        for (String tableName : database.getTables().keySet()) {
            if (database.getTables().get(tableName) != null) {
                database.saveTable(tableName);
            }
        }
    }
    public void deleteDatabase(String databaseName) {
        Path databasePath = Paths.get(storageFolderPath, databaseName);
        if (!exists(databasePath)) throw new DBException(ErrorType.DATABASE_NOT_FOUND_EXCEPTION);

        if (this.databaseName != null && this.databaseName.equals(databaseName)) {
            this.databaseName = null;
            this.databaseMetadata = null;
            this.database = null;
        }
        if (!deleteDirectory(databasePath.toFile())) throw new DBException(ErrorType.DELETE_DATABASE_EXCEPTION);
    }
    private void loadMetadata() throws IOException {
        // assumes database exits, callers responsibility.
        // Open file handle for databases metadata.json:
        this.databaseMetadata = mapper.readValue(new File(storageFolderPath, File.separator
                + databaseName + File.separator + "metadata.json"), DBMetadata.class);
    }
    // public getters and setters methods:
    public Database getDatabase() {
        return this.database;
    }
    public DBMetadata getMetadata() {
        return this.databaseMetadata;
    }
    public String getDatabaseName() {
        return databaseName;
    }
    public void delInstance() {
        DBManager.instance = null;
    }
}
