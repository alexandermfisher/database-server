package edu.uob.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uob.Utils;
import edu.uob.utils.DBException;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class DBManagerTests {
    private static String dbPath;
    @BeforeAll
    public static void setupDB() throws IOException {

        dbPath = "src/test/java/edu/uob/databases/";
        if (new File(dbPath).exists()) { Utils.deleteDirectory(new File(dbPath)); }
        Utils.setupDB(dbPath);
    }
    @AfterAll
    public static void cleanUpDatabases() { Utils.deleteDirectory(new File("src/test/java/edu/uob/databases/")); }


    @Test
    public void testDatabaseExists() {
        DBManager manager = DBManager.getInstance("incorrect_path_to_dbs");
        assertFalse(manager.databaseExists("users"));
        assertFalse(manager.databaseExists("spoof_database"));
        manager.delInstance();
        manager = DBManager.getInstance(dbPath);
        assertTrue(manager.databaseExists(  "users"));
        assertFalse(manager.databaseExists("spoof_database"));
        manager.delInstance();
    }

    @Test
    public void testUseDatabase() throws IOException {
        DBManager manager = DBManager.getInstance(dbPath);
        // incorrect databaseName --> database does not exist:
        Assertions.assertThrows(DBException.class, () -> manager.useDatabase("spoof_database"));

        // correct databaseName --> database exists and should load:
        manager.useDatabase("users");
        DBMetadata metadata = manager.getMetadata();
        assertEquals("id", metadata.getTables().get("users").getPrimaryKey());
        assertEquals(4, metadata.getTables().get("users").getNextPrimaryKey());
        assertEquals("id", metadata.getTables().get("users").getAttributes().get(0));
        assertEquals("name", metadata.getTables().get("users").getAttributes().get(1));
        assertEquals("age", metadata.getTables().get("users").getAttributes().get(2));
        assertEquals("email", metadata.getTables().get("users").getAttributes().get(3));
        manager.closeDatabase();
        manager.delInstance();
    }
    @Test
    public void testCreateDatabaseExceptions() {
        // Test 1: DatabaseExistsException:
        DBManager manager1 = DBManager.getInstance(dbPath);
        DBMetadata metadata = new DBMetadata(null);
        assertThrows(DBException.class, () -> manager1.createDatabase("users", metadata));

        // Test 2: IOException:
        manager1.delInstance();
        // Set manager to nonexistent path to trigger io exception:
        DBManager manager2 = DBManager.getInstance("/path/does/not/exist");
        assertThrows(IOException.class, () -> manager2.createDatabase("spoof_database", metadata));
        manager2.delInstance();
    }

    @Test
    public void testCreateDatabaseWithMetadata() throws IOException {
        // Create test data
        HashMap<String, DBMetadata.Table> tables = new HashMap<>();
        edu.uob.utils.Utils.CaseInsensitiveArrayList<String> attributes = new edu.uob.utils.Utils.CaseInsensitiveArrayList<>();
        attributes.add("id");
        attributes.add("name");
        DBMetadata.Table sportsTable = new DBMetadata.Table("testDB", "id", 1, attributes);
        tables.put("sports", sportsTable);
        DBMetadata metadata = new DBMetadata(tables);

        // Create database with metadata
        DBManager manager = DBManager.getInstance(dbPath);
        manager.deleteDatabase("testDB");
        manager.createDatabase("testDB", metadata);

        // Read metadata from file
        String metadataFilePath = dbPath + "testDB/metadata.json";
        File metadataFile = new File(metadataFilePath);
        assertTrue(metadataFile.exists());

        ObjectMapper objectMapper = new ObjectMapper();
        DBMetadata savedMetadata = objectMapper.readValue(metadataFile, DBMetadata.class);

        // Assert saved metadata is correct
        assertNotNull(savedMetadata);
        HashMap<String, DBMetadata.Table> savedTables = savedMetadata.getTables();
        assertTrue(savedTables.containsKey("sports"));
        DBMetadata.Table savedSportsTable = savedTables.get("sports");
        assertEquals("id", savedSportsTable.getPrimaryKey());
        assertEquals(1, savedSportsTable.getNextPrimaryKey());
        assertEquals(attributes, savedSportsTable.getAttributes());

        manager.deleteDatabase("testDB");
        assertFalse(manager.databaseExists("testDB"));
        manager.delInstance();
    }
     @Test
    public void testCloseDatabase() throws IOException {
        DBManager manager = DBManager.getInstance(dbPath);
        manager.createDatabase("testDB", new DBMetadata(new HashMap<>()));
        manager.useDatabase("testDB");
        assertTrue(manager.databaseExists("testDB"));
        manager.closeDatabase();

        // Verify that the database name and metadata are set to null after closing:
        assertNull(manager.getDatabaseName());
        assertNull(manager.getMetadata());
        manager.deleteDatabase("testDB");
        assertFalse(manager.databaseExists("testDB"));
        manager.delInstance();
    }
}

