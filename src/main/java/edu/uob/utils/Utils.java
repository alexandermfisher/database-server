package edu.uob.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.*;
import java.util.*;

public class Utils {
    public static String constructFilePath(String baseDirectory, String fileName) {
        Path directoryPath = Paths.get(baseDirectory);
        Path filePath = directoryPath.resolve(fileName);
        return filePath.toString();
    }
    public static String constructDirectoryPath(String baseDirectory, String tableName) throws IOException {
        Path directoryPath = Paths.get(baseDirectory, tableName);
        Files.createDirectories(directoryPath);
        return directoryPath.toString();
    }
    public static boolean deleteDirectory(File directory) {
        if (!directory.exists()) { return true; }
        File[] directoryContents = directory.listFiles();
        if (directoryContents != null) {
            for (File file : directoryContents) {
                if (file.isDirectory()) {
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }
    public static void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.delete(path);
    }
    public static class CaseInsensitiveArrayList<S> extends ArrayList<String> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final Set<String> lowercaseSet = new HashSet<>();
        public CaseInsensitiveArrayList() {}
        public CaseInsensitiveArrayList(List<String> elements) { for (String element : elements) this.add(element); }
        @Override
        public boolean add(String element) {
            lowercaseSet.add(element.toLowerCase());
            return super.add(element);
        }
        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof String searchElement))
                return false;
            return lowercaseSet.contains(searchElement.toLowerCase());
        }
        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof String)) return false;
            String searchElement = ((String) obj).toLowerCase();
            for (String element : super.stream().toList()) {
               if (searchElement.equalsIgnoreCase(element)) {
                   super.remove(element);
                   lowercaseSet.remove(searchElement);
                   return true;
               }
            }
            return false;
        }
    }
    public static String prettyPrintTable(HashMap<Integer, HashMap<String, String>> table, ArrayList<String> attributes,
                                          Set<Integer> recordIDs, boolean joinCMD) {
        StringBuilder response = new StringBuilder("\n");
        StringBuilder header = new StringBuilder(formatHeader(attributes, 15));

        // Create the line of equal signs
        String line = "=".repeat(Math.max(0, header.length()));

        response.append(header.toString().trim()).append("\n");
        response.append(line.trim()).append("\n");

        for (int id : recordIDs) {
            if (table.containsKey(id)) {
                HashMap<String, String> record = table.get(id);
                StringBuilder formattedRecord = new StringBuilder();
                for (String attribute : attributes) {
                    if (!joinCMD) attribute = attribute.toLowerCase();
                    formattedRecord.append("| ").append(padRight(record.getOrDefault(attribute, ""), 15)).append(" ");
                }
                response.append(formattedRecord.toString().trim()).append("\n");
            }
        }
        return response.toString();
    }
    private static String padRight(String s, int width) { return String.format("%-" + width + "s", s); }
    private static String formatHeader(ArrayList<String> attributes, int width) {
        StringBuilder formattedHeader = new StringBuilder();
        for (String attribute : attributes) {
            formattedHeader.append("| ").append(padRight(attribute, width)).append(" ");
        }
        return formattedHeader.toString().trim();
    }

    public static String printTable(HashMap<Integer, HashMap<String, String>> table, ArrayList<String> attributes, Set<Integer> recordIDs, boolean join) {
        StringBuilder response = new StringBuilder();
        response.append(formatHeader(attributes)).append("\t\n");
        for (int id : recordIDs) {
            if (table.containsKey(id)) {
                HashMap<String, String> record = table.get(id);
                StringBuilder formattedRecord = new StringBuilder();
                for (String attribute : attributes) {
                    if (!join) attribute = attribute.toLowerCase();
                    formattedRecord.append(record.getOrDefault(attribute, "")).append("\t");
                }

                response.append(formattedRecord).append("\n");
            }
        }
        return response.toString();
    }

    private static String formatHeader(ArrayList<String> attributes) {
        StringBuilder formattedHeader = new StringBuilder();
        for (String attribute : attributes) {
            formattedHeader.append(attribute).append("\t");
        }
        return formattedHeader.toString().trim();
    }

}
