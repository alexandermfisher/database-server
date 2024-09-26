package edu.uob;

import edu.uob.database.DBManager;
import edu.uob.database.DBMetadata;
import edu.uob.database.Table;
import edu.uob.utils.Utils.CaseInsensitiveArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class Utils {

    public static void setupDB(String dbPath) throws IOException {
        File directory = new File(dbPath);
        if (!directory.exists() && !directory.mkdir()) fail("failed to make testing databases directory.");
        DBManager manager = DBManager.getInstance(dbPath);
        manager.createDatabase("testDB", new DBMetadata(new HashMap<>()));
        DBMetadata.Table usersTable = new DBMetadata.Table("users", "id", 4,
                new CaseInsensitiveArrayList<>((List.of("id", "name", "age", "email"))));
        HashMap<String, DBMetadata.Table> tablesMap = new HashMap<>();
        tablesMap.put("users", usersTable);
        DBMetadata metadata = new DBMetadata(tablesMap);
        manager.createDatabase("users", metadata);
        CaseInsensitiveArrayList<String> attributes = new CaseInsensitiveArrayList<>();
        attributes.add("id");
        attributes.add("name");
        attributes.add("age");
        attributes.add("email");
        Table table = new Table(new DBMetadata.Table("users", "id", 1, attributes));
        HashMap<String, String> record1 = new HashMap<>();
        record1.put("id", "1");
        record1.put("name", "Bob");
        record1.put("age", "21");
        record1.put("email", "bob@bob.net");
        HashMap<String, String> record2 = new HashMap<>();
        record2.put("id", "2");
        record2.put("name", "Harry");
        record2.put("age", "32");
        record2.put("email", "harry@harry.com");
        HashMap<String, String> record3 = new HashMap<>();
        record3.put("id", "3");
        record3.put("name", "Chris");
        record3.put("age", "42");
        record3.put("email", "chris@chris.ac.uk");
        table.addRecord(record1);
        table.addRecord(record2);
        table.addRecord(record3);
        String filePath = dbPath + "users/users.tab";
        table.saveTable(filePath);
        manager.delInstance();
    }

    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) deleteDirectory(file);
                    file.delete();
                }
                directory.delete();
            }
            directory.delete();
        }
    }
}

