package edu.uob.interpreter;

import edu.uob.parser.Command;
import edu.uob.parser.Condition;
import edu.uob.database.DBManager;
import edu.uob.database.DBMetadata;
import edu.uob.database.Table;
import edu.uob.tokenizer.TokenType;
import edu.uob.utils.DBException;
import edu.uob.utils.ErrorType;
import edu.uob.utils.Utils;

import java.io.IOException;
import java.util.*;

public class Interpreter implements CommandVisitor {
    private final DBManager manager;
    private String response;
    public Interpreter(DBManager manager) { this.manager = manager; }
    public String getResponse() { return response; }
    public void interpret(Command command) throws IOException {
        response = null;
        command.accept(this);
    };
    @Override
    public void visit(Command.Use useCommand) throws IOException { manager.useDatabase(useCommand.getDatabaseName()); }
    @Override
    public void visit(Command.CreateDatabase createDatabaseCommand) throws IOException {
        manager.createDatabase(createDatabaseCommand.getDatabaseName(), new DBMetadata(new HashMap<>()));
    }
    @Override
    public void visit(Command.CreateTable createTableCommand) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        manager.getDatabase().createTable(createTableCommand.getTableName(), createTableCommand.getAttributes().getAttributes());
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Drop drop) throws IOException {
        // Drop Database:
        if (drop.getDropType() == Command.Drop.DropType.DATABASE) {
            manager.deleteDatabase( ((Command.DropDatabase) drop).getDatabaseName());
            return;
        }
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        // Drop Table:
        manager.getDatabase().dropTable(drop.getName());
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Alter alter) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        if (alter.getAlterationType() == Command.Alter.AlterationType.ADD) {
            manager.getDatabase().addAttribute(alter.getTableName(), alter.getAttributeName());
        } else {
            manager.getDatabase().dropAttribute(alter.getTableName(), alter.getAttributeName());
        }
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Insert insert) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);

        String tableName = insert.getTableName();
        manager.getDatabase().loadTable(tableName);
        Table table = manager.getDatabase().getTables().get(tableName);

        List<String> attributeNames = table.getAttributes();
        List<String> values = insert.getValues().getValues();
        if (attributeNames.size() != values.size() + 1) {
            throw new DBException(ErrorType.INVALID_VALUE_EXCEPTION);
        }

        // Map values to attributes
        HashMap<String, String> newRecord = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            String attributeName = attributeNames.get(i + 1).toLowerCase();
            String value = values.get(i);
            newRecord.put(attributeName.toLowerCase(), value);
        }

        table.addRecord(newRecord);
        // update next Primary Key manually as int is not passed by reference:
        manager.getMetadata().getTables().get(tableName).incrementNextPrimaryKey();
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Select select) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        if (!manager.getDatabase().getTables().containsKey(select.getTableName()))
            throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        manager.getDatabase().loadTable(select.getTableName());
        Table table = manager.getDatabase().getTables().get(select.getTableName());

        // Attributes requested:
        ArrayList<String> attributes = processWildCardList(select);

        // Records requested:
        Condition condition = select.getCondition();
        Set<Integer> recordIDs;
        if (condition == null) { recordIDs = manager.getDatabase().getTables().get(select.getTableName()).getRecords().keySet(); }
        else { recordIDs = processCondition(condition, table); }
        response = Utils.prettyPrintTable(table.getRecords(), attributes, recordIDs, false);
    }
    @Override
    public void visit(Command.Delete delete) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        if (!manager.getDatabase().getTables().containsKey(delete.getTableName()))
            throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        manager.getDatabase().loadTable(delete.getTableName());
        Table table = manager.getDatabase().getTables().get(delete.getTableName());

        // Records to be deleted:
        Condition condition = delete.getCondition();
        Set<Integer> recordIDs;
        recordIDs = processCondition(condition, table);
        table.deleteRecords(recordIDs);
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Update update) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);
        if (!manager.getDatabase().getTables().containsKey(update.getTableName()))
            throw new DBException(ErrorType.TABLE_NOT_FOUND_EXCEPTION);
        manager.getDatabase().loadTable(update.getTableName());
        Table table = manager.getDatabase().getTables().get(update.getTableName());

        HashMap<String, String> nameValueList = update.getNameValueList();
        Condition condition = update.getCondition();

        Set<Integer> recordIDs = processCondition(condition, table);

        for (Integer id : recordIDs) {
            HashMap<String, String> record = table.getRecord(id);
            for (Map.Entry<String, String> entry : nameValueList.entrySet()) {
                String attributeName = entry.getKey();
                String attributeValue = entry.getValue();
                if (attributeName.equalsIgnoreCase("id") || !table.getAttributes().contains(attributeName)) {
                    throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
                }
                record.put(attributeName.toLowerCase(), attributeValue);
            }
            table.updateRecord(id, record);
        }
        manager.saveDatabase();
    }
    @Override
    public void visit(Command.Join join) throws IOException {
        if (manager.getDatabase() == null) throw new DBException(ErrorType.NO_DATABASE_IN_USE);

        String firstTableName = join.getFirstTableName();
        String secondTableName = join.getSecondTableName();
        String firstAttributeName = join.getFirstAttributeName();
        String secondAttributeName = join.getSecondAttributeName();

        // Retrieve the tables from the database
        manager.getDatabase().loadTable(firstTableName);
        Table firstTable = manager.getDatabase().getTables().get(firstTableName);
        manager.getDatabase().loadTable(secondTableName);
        Table secondTable = manager.getDatabase().getTables().get(secondTableName);

        if (!firstTable.getAttributes().contains(firstAttributeName)
                || !secondTable.getAttributes().contains(secondAttributeName)) {
            throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
        }

        // Perform the join operation
        Table resultTable = performJoin(firstTable, secondTable, firstAttributeName, secondAttributeName);

        response = Utils.prettyPrintTable(resultTable.getRecords(), resultTable.getAttributes(), resultTable.getRecords().keySet(), true);
    }
    private Table performJoin(Table firstTable, Table secondTable, String firstAttributeName, String secondAttributeName) {
        Table resultTable = new Table( "joinTable","id", 1, new Utils.CaseInsensitiveArrayList<>());
        resultTable.addAttribute("id");
        // Add attributes from the first table to the result table, excluding the join column and id
        for (String attribute : firstTable.getAttributes()) {
            if (!attribute.equalsIgnoreCase(firstAttributeName) && !attribute.equalsIgnoreCase("id")) {
                resultTable.addAttribute(firstTable.getOriginalTableName() + "." + attribute);
            }
        }
        // Add attributes from the second table to the result table, excluding the join column and id
        for (String attribute : secondTable.getAttributes()) {
            if (!attribute.equalsIgnoreCase(secondAttributeName) && !attribute.equalsIgnoreCase("id")) {
                resultTable.addAttribute(secondTable.getOriginalTableName() + "." + attribute);
            }
        }
        // Perform the join operation
        for (Map.Entry<Integer, HashMap<String, String>> entry : firstTable.getRecords().entrySet()) {
            String attributeValueFromFirstTable = entry.getValue().get(firstAttributeName.toLowerCase());
            for (Map.Entry<Integer, HashMap<String, String>> entry2 : secondTable.getRecords().entrySet()) {
                String attributeValueFromSecondTable = entry2.getValue().get(secondAttributeName.toLowerCase());
                if (attributeValueFromFirstTable.equals(attributeValueFromSecondTable)) {
                    HashMap<String, String> mergedRecord = new HashMap<>();
                    // Add attributes from the first table to the merged record
                    for (String attribute : firstTable.getAttributes()) {
                        if (!attribute.equalsIgnoreCase(firstAttributeName) && !attribute.equals("id")) {
                            mergedRecord.put(firstTable.getOriginalTableName() + "." + attribute, entry.getValue().get(attribute.toLowerCase()));
                        }
                    }
                    // Add attributes from the second table to the merged record
                    for (String attribute : secondTable.getAttributes()) {
                        if (!attribute.equalsIgnoreCase(secondAttributeName) && !attribute.equals("id")) {
                            mergedRecord.put(secondTable.getOriginalTableName() + "." + attribute, entry2.getValue().get(attribute.toLowerCase()));
                        }
                    }
                    resultTable.addRecord(mergedRecord);
                }
            }
        }

        return resultTable;
    }
    private ArrayList<String> processWildCardList(Command.Select select) {
        if (select.isSelectAll()) {
            return manager.getDatabase().getTables().get(select.getTableName()).getAttributes();
        }

        ArrayList<String> attributes = new ArrayList<>();
        for (String attribute : select.getWildAttribList().getAttributes()) {
            if (manager.getDatabase().getTables().get(select.getTableName()).getAttributes().contains(attribute.toLowerCase())) {
                attributes.add(attribute);
            } else {
                throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
            }
        }
        return attributes;
    }
    private Set<Integer> processCondition(Condition condition, Table table) {
        Set<Integer> result = new HashSet<>();
        if (condition instanceof Condition.Expression) {
            Set<Integer> leftResult = processCondition(((Condition.Expression) condition).getLeftCondition(), table);
            Set<Integer> rightResult = processCondition(((Condition.Expression) condition).getRightCondition(), table);
            if (((Condition.Expression) condition).getBoolOperator() == Condition.BoolOperator.AND) {
                result.addAll(leftResult);
                result.retainAll(rightResult);
            } else { // Condition.BoolOperator.OR
                result.addAll(leftResult);
                result.addAll(rightResult);
            }
            return result;
        }
        // Implicitly is instanceof AttributeValueComparison --> evaluate/Find matching records:
        return evaluateCondition(table, condition);
    }
    private TokenType getTokenType(String value) {
        if (value.matches("[+-]?\\d+")) {
            return TokenType.INTEGER_LITERAL;
        } else if (value.matches("[+-]?\\d+(\\.\\d+)?")) {
            return TokenType.FLOAT_LITERAL;
        } else if (value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("true")) {
            return TokenType.BOOLEAN_LITERAL;
        } else if (value.equalsIgnoreCase("null")) {
            return TokenType.NULL_LITERAL;
        }
        return TokenType.STRING_LITERAL;
    }
    private TokenType getAttributeType(Table table, String attribute) {
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add(attribute.toLowerCase());
        TokenType type;
        for (HashMap<String, String> value : table.getRecords(tmp)) {
            if (getTokenType(value.get(attribute)) != TokenType.NULL_LITERAL) {
                return getTokenType(value.get(attribute));
            }
        }
        return TokenType.NULL_LITERAL;
    }
    private HashSet<Integer> evaluateCondition(Table table, Condition condition) {
        HashSet<Integer> result = new HashSet<>();
        TokenType attributeType = getAttributeType(table, ((Condition.AttributeValueComparison) condition).getAttributeName());

        String attribute = ((Condition.AttributeValueComparison) condition).getAttributeName();
        if (!table.getAttributes().contains(attribute)) {
            throw new DBException(ErrorType.INVALID_ATTRIBUTE_EXCEPTION);
        }
        for (Integer key : table.getRecords().keySet()) {
            String recordValue = table.getRecords().get(key).get(attribute);
            String comparisonValue = ((Condition.AttributeValueComparison) condition).getValue();
            switch (((Condition.AttributeValueComparison) condition).getComparator()) {
                case EQUAL:
                    // Integers, Floats, boolean, NULL
                    if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                            Float.parseFloat(recordValue) == Float.parseFloat(comparisonValue)) { result.add(key); break; }
                    // Strings
                    if (recordValue.equals(comparisonValue)) { result.add(key); break; }
                    // Boolean
                    if (attributeType == TokenType.BOOLEAN_LITERAL && recordValue.equalsIgnoreCase(comparisonValue)) {
                        result.add(key);
                    }
                    break;
                case NOT_EQUAL:
                    // Integers, Floats, boolean, NULL
                    if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                            Float.parseFloat(recordValue) != Float.parseFloat(comparisonValue)) { result.add(key); break; }
                    // Boolean
                    if (attributeType == TokenType.BOOLEAN_LITERAL && !recordValue.equalsIgnoreCase(comparisonValue)) {
                        result.add(key);
                        break;
                    }
                    // Strings
                    if (attributeType == TokenType.STRING_LITERAL && !recordValue.equals(comparisonValue)) { result.add(key); break; }

                    break;
                // integers, floats
                case LESS_THAN:
                    if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                            Float.parseFloat(recordValue) < Float.parseFloat(comparisonValue)) { result.add(key); }
                    break;
                case GREATER_THAN:
                        if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                                Float.parseFloat(recordValue) > Float.parseFloat(comparisonValue)) { result.add(key); }
                    break;
                case LESS_THAN_OR_EQUAL:
                    if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                            Float.parseFloat(recordValue) <= Float.parseFloat(comparisonValue)) { result.add(key); }
                    break;
                case GREATER_THAN_OR_EQUAL:
                    if (isNumeric(recordValue) && isNumeric(comparisonValue) &&
                            Float.parseFloat(recordValue) >= Float.parseFloat(comparisonValue)) { result.add(key); }
                    break;
                case LIKE:
                    if (recordValue.toLowerCase().contains(comparisonValue.toLowerCase())) { result.add(key); }
                    break;
            }
        }
        return result;
    }
    private boolean isNumeric(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch(NumberFormatException e ) {
            return false;
        }
    }
}
