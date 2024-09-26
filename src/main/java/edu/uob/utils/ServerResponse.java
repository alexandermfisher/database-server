package edu.uob.utils;

public class ServerResponse {
    private static final String SUCCESS = "[OK]";
    private static final String ERROR = "[ERROR]";
    public static String success() {
        return SUCCESS;
    }
    public static String success(String result) {
        return SUCCESS + "\n" + result;
    }
    public static String error(DBException exception) {
        return ERROR + ": " + exception.getMessage();
    }
    public static String error(Exception exception) {
        return ERROR + ": " + exception.getMessage();
    }
    public static String error(RuntimeException exception) {
        return ERROR + ": " + exception.getMessage();
    }
}
