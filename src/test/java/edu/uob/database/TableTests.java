package edu.uob.database;

import edu.uob.Utils;
import edu.uob.utils.Utils.CaseInsensitiveArrayList;
import edu.uob.utils.DBException;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TableTests {
    // ============================================================================================================== //
    //                                                SETUP                                                           //
    // ============================================================================================================== //
    private DBManager manager;
    private static String dbPath;
    @BeforeAll
    public static void setupDB() throws IOException {
        dbPath = "src/test/java/edu/uob/databases/";
        Utils.setupDB(dbPath);
    }
    @BeforeEach
    public void instantiateManager() { manager = DBManager.getInstance(dbPath); }
    @AfterEach
    public void deleteManager() { manager.delInstance(); }
    @AfterAll
    public static void cleanUpDatabases() { Utils.deleteDirectory(new File("src/test/java/edu/uob/databases/")); }


    // ============================================================================================================== //
    //                                              TESTING                                                           //
    // ============================================================================================================== //

    @Test
    public void testLoadTable() {
        try {
            manager.useDatabase("users");
        } catch (IOException e) {
            fail("Unexpected exception occurred using 'users' db: " + e.getMessage());
        }
        Table table = new Table(manager.getMetadata().getTables().get("users"));
        try {
            table.loadTable(dbPath + "users/users.tab");
        } catch (IOException e) {
            fail("Unexpected exception occurred loading table 'users.tab': " + e.getMessage());
        }

        // Test that the correct data has been loaded in:
        assertEquals("id", table.getPrimaryKey());
        assertEquals(4, table.getNextPrimaryKey());
        String[] expectedAttributes = {"id", "name", "age", "email"};
        String[] actualAttributes = table.getAttributes().toArray(new String[0]);
        assertArrayEquals(expectedAttributes, actualAttributes);

        // assert test record 1: 1	Bob	21	bob@bob.net
        HashMap<String, String> record_1 = null;
        record_1 = table.getRecord(1);
        assertEquals("1", record_1.get("id"));
        assertEquals("Bob", record_1.get("name"));
        assertEquals("bob@bob.net", record_1.get("email"));

        // Test file not found exception thrown
        try {
            table.loadTable("spoof path");
            fail("Expected an exception to be thrown");
        } catch (IOException exception) {
            assertInstanceOf(FileNotFoundException.class, exception);
        }
    }

    @Test
    public void testAddRecord() {
        Table table = null;
        try {
            manager.useDatabase("users");
            table = new Table(manager.getMetadata().getTables().get("users"));
            table.loadTable(dbPath + "users/users.tab");
        } catch (IOException e) {
            fail("Unexpected exception occurred using 'users' db: " + e.getMessage());
        }

        // Create a record to add
        HashMap<String, String> recordToAdd = new HashMap<>();
        recordToAdd.put("name", "Alice");
        recordToAdd.put("age", "25");
        recordToAdd.put("email", "alice@example.com");

        // Add the record
        table.addRecord(4, recordToAdd);

        // Retrieve the added record and check if it exists
        HashMap<String, String> retrievedRecord = table.getRecord(4);
        assertNotNull(retrievedRecord);
        assertEquals("Alice", retrievedRecord.get("name"));
        assertEquals("25", retrievedRecord.get("age"));
        assertEquals("alice@example.com", retrievedRecord.get("email"));
    }

    @Test
    void testGetRecord() {
        // Initialize your table with some data
        Table table = null;
        try {
            manager.useDatabase("users");
            table = new Table(manager.getMetadata().getTables().get("users"));
            table.loadTable(dbPath + "users/users.tab");
        } catch (IOException e) {
            fail("Unexpected exception occurred using 'users' db: " + e.getMessage());
        }

        // Good scenario - testing getRecord(int id)
        HashMap<String, String> record = table.getRecord(1);
        assertEquals("Bob", record.get("name"));
        assertEquals("21", record.get("age"));
        assertEquals("bob@bob.net", record.get("email"));

        // Exception scenario - invalid primary key
        Table finalTable = table;
        assertThrows(DBException.class, () -> {
            finalTable.getRecord(5);
        });

        // Good scenario - testing getRecord(int id, ArrayList<String> attributes)
        CaseInsensitiveArrayList<String> attributes = new CaseInsensitiveArrayList<>();
        attributes.add("name");
        record = table.getRecord(2, attributes);
        assertEquals("Harry", record.get("name"));
        assertNull(record.get("age")); // Age attribute not requested
        assertNull(record.get("email")); // Email attribute not requested

        // Exception scenario - invalid attribute
        attributes.clear();
        attributes.add("height"); // Height attribute is not present
        Table finalTable1 = table;
        assertThrows(DBException.class, () -> {
            finalTable1.getRecord(3, attributes);
        });

        // Good scenario - testing getRecords(ArrayList<String> attributes)
        CaseInsensitiveArrayList<String> allAttributes = new CaseInsensitiveArrayList<>();
        allAttributes.add("name");
        allAttributes.add("age");

        ArrayList<HashMap<String, String>> records = table.getRecords(allAttributes);
        assertEquals(3, records.size());
        assertFalse(records.get(0).containsKey("email"));
        // Checking the first record
        HashMap<String, String> firstRecord = records.get(0);
        assertEquals("Bob", firstRecord.get("name"));
        assertEquals("21", firstRecord.get("age"));

        // Checking the second record
        HashMap<String, String> secondRecord = records.get(1);
        assertEquals("Harry", secondRecord.get("name"));
        assertEquals("32", secondRecord.get("age"));

        // Checking the third record
        HashMap<String, String> thirdRecord = records.get(2);
        assertEquals("Chris", thirdRecord.get("name"));
        assertEquals("42", thirdRecord.get("age"));

    }
    @Test
    public void testSaveTable() {
        CaseInsensitiveArrayList<String> attributes = new CaseInsensitiveArrayList<>();
        attributes.add("id");
        attributes.add("name");
        attributes.add("age");
        attributes.add("email");
        Table table = new Table(new DBMetadata.Table("testDB", "id", 1, attributes));

        HashMap<String, String> record1 = new HashMap<>();
        record1.put("id", "1");
        record1.put("name", "Alice");
        record1.put("age", "25");
        record1.put("email", "alice@example.com");
        table.addRecord(record1);

        HashMap<String, String> record2 = new HashMap<>();
        record2.put("id", "2");
        record2.put("name", "Bob");
        record2.put("age", "30");
        record2.put("email", "bob@example.com");
        table.addRecord(record2);

        // Save the table to a file
        String filePath = dbPath + "testDB/testTable.tab";
        try {
            table.saveTable(filePath);
        } catch (IOException e) {
            fail("Failed to save table: " + e.getMessage());
        }
        // Load the saved table
        Table loadedTable = new Table(new DBMetadata.Table("testDB","id", 1, attributes));
        try {
            loadedTable.loadTable(filePath);
        } catch (IOException e) {
            fail("Failed to load table: " + e.getMessage());
        }

        HashMap<Integer, HashMap<String, String>> originalRecords = table.getRecords();
        HashMap<Integer, HashMap<String, String>> loadedRecords = loadedTable.getRecords();
        assertEquals(originalRecords.size(), loadedRecords.size());

        for (Integer id : originalRecords.keySet()) {
            assertTrue(loadedRecords.containsKey(id));
            HashMap<String, String> originalRecord = originalRecords.get(id);
            HashMap<String, String> loadedRecord = loadedRecords.get(id);
            assertEquals(originalRecord, loadedRecord);
        }
    }
    @Test
    public void testUpdateRecord() {
        Table table = new Table(new DBMetadata.Table("testDB", "id", 1,
                new CaseInsensitiveArrayList<>(Arrays.asList("id", "name", "age", "email"))));

        // Add a record to the table
        HashMap<String, String> originalRecord = new HashMap<>();
        originalRecord.put("id", "1");
        originalRecord.put("name", "Alice");
        originalRecord.put("age", "25");
        originalRecord.put("email", "alice@example.com");
        table.addRecord(1, originalRecord);

        HashMap<String, String> updatedRecord = new HashMap<>();
        updatedRecord.put("age", "30");
        table.updateRecord(1, updatedRecord);

        HashMap<String, String> retrievedRecord = table.getRecord(1);
        assertEquals("Alice", retrievedRecord.get("name")); // Name should remain unchanged
        assertEquals("30", retrievedRecord.get("age")); // Age should be updated
        assertEquals("alice@example.com", retrievedRecord.get("email")); // Email should remain unchanged
    }
    @Test
    public void testAddAttribute() {
        // Create a Table instance with some initial attributes
        CaseInsensitiveArrayList<String> initialAttributes = new CaseInsensitiveArrayList<>(Arrays.asList("id", "name", "age", "email"));
        Table table = new Table(new DBMetadata.Table("testDB", "id", 1, initialAttributes));

        // Add a new attribute
        String newAttribute = "gender";
        table.addAttribute(newAttribute);

        // Check if the new attribute has been added successfully
        List<String> updatedAttributes = table.getAttributes();
        assertTrue(updatedAttributes.contains(newAttribute), "New attribute should be present in the list of attributes.");

        // Check if the new attribute has been added to existing records with default values
        HashMap<Integer, HashMap<String, String>> records = table.getRecords();
        for (HashMap<String, String> record : records.values()) {
            assertTrue(record.containsKey(newAttribute), "New attribute should be present in all records with default value.");
            assertEquals("", record.get(newAttribute), "Default value of new attribute should be empty string.");
        }
    }
}

