package edu.uob.parser;

import edu.uob.interpreter.CommandVisitor;
import edu.uob.tokenizer.Tokenizer.Token;

import java.io.IOException;
import java.util.HashMap;

public abstract class Command extends Node {
    @Override
    public void print(int indent) {
        System.out.print(" ".repeat(indent));
        System.out.println("Command: " + this.getClass().getSimpleName());
    }
    // <Use> ::=  "USE " [DatabaseName]
    public static class Use extends Command {
        private final String databaseName;
        public Use(Token token) { databaseName = token.getValue().toLowerCase(); }
        public String getDatabaseName() { return databaseName;}
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this);
        }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent*2));
            System.out.println("Database: " + this.databaseName);
        }
    }
    // <CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
    public static class CreateDatabase extends Command {
        private final String databaseName;
        public CreateDatabase(Token token) { databaseName = token.getValue().toLowerCase(); }
        public String getDatabaseName() { return databaseName; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent*2)); // Indentation
            System.out.println("Database: " + this.databaseName);
        }
    }
    // <CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
    public static class CreateTable extends Command {
        private final String tableName;
        private final List.AttributeList attributes;
        public CreateTable(Token token) {
            tableName = token.getValue();
            attributes = new List.AttributeList();
        }
        public List.AttributeList getAttributes() {return attributes; }
        public String getTableName() { return tableName; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent + 2));
            System.out.println("Attributes:");
            attributes.print(indent + 4);
        }
    }
    // <Drop> ::=  "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
    public static class Drop extends Command {
        private final String name;
        private final DropType dropType;
        public Drop(Token token, DropType dropType) {
            this.name = token.getValue().toLowerCase();
            this.dropType = dropType;
        }
        public String getName() { return name; }
        public DropType getDropType() { return dropType; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
        public enum DropType { DATABASE, TABLE }
    }
    public static class DropDatabase extends Drop {
        public DropDatabase(Token token) { super(token, DropType.DATABASE); }
        public String getDatabaseName() { return super.getName(); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent*2));
            System.out.println("Database: " + this.getName());
        }
    }
    public static class DropTable extends Drop {
        public DropTable(Token token) {
            super(token, DropType.TABLE);
        }
        public String getTableName() {return super.getName(); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent*2)); // Indentation
            System.out.println("Table: " + this.getName());
        }
    }
    // <Alter> ::=  "ALTER " "TABLE " [TableName] " " <"ADD" | "DROP"> " " [AttributeName]
    public static class Alter extends Command {
        private final String tableName;
        private final String attributeName;
        private final AlterationType alterationType;
        public Alter(Token tableNameToken, AlterationType alterationType, Token attributeNameToken) {
            this.tableName = tableNameToken.getValue().toLowerCase();
            this.attributeName = attributeNameToken.getValue();
            this.alterationType = alterationType;
        }
        public String getTableName() { return tableName; }
        public String getAttributeName() { return attributeName; }
        public AlterationType getAlterationType() { return alterationType; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.print(" ".repeat(indent*2));
            System.out.println("Table: " + this.tableName);
            System.out.print(" ".repeat(indent*3));
            System.out.println("Attribute: " + this.attributeName);
            System.out.print(" ".repeat(indent*4));
            System.out.println("Type: " + this.alterationType);
        }
        public enum AlterationType { ADD, DROP }
    }
    public static class AddAttribute extends Alter {
        public AddAttribute(Token tableNameToken, Token attributeNameToken) {
            super(tableNameToken, AlterationType.ADD, attributeNameToken);
        }
    }
    public static class DropAttribute extends Alter {
        public DropAttribute(Token tableNameToken, Token attributeNameToken) {
            super(tableNameToken, AlterationType.DROP, attributeNameToken);
        }
    }
    // <Insert> ::=  "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
    public static class Insert extends Command {
        private final String tableName;
        private final List.ValueList values;
        public Insert(Token tableNameToken) {
            this.tableName = tableNameToken.getValue().toLowerCase();
            this.values = new List.ValueList();
        }
        public String getTableName() { return tableName; }
        public List.ValueList getValues() { return values; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
    }
    public static class Select extends Command {
        private final List.WildAttributeList wildAttribList;
        private final String tableName;
        private final Condition condition;
        private final boolean selectAll;

        public Select(List.WildAttributeList wildAttribList, Token tableNameToken, Condition condition, boolean selectAll) {
            this.wildAttribList = wildAttribList;
            this.tableName = tableNameToken.getValue().toLowerCase();
            this.condition = condition;
            this.selectAll = selectAll;
        }
        public List.WildAttributeList getWildAttribList() { return wildAttribList; }
        public String getTableName() { return tableName; }
        public Condition getCondition() { return condition; }
        public boolean isSelectAll() { return selectAll; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
        @Override
        public void print(int indent) {
            super.print(indent);
            System.out.println(" ".repeat(indent + 2) + "Wild Attribute List: " + wildAttribList);
            System.out.println(" ".repeat(indent + 2) + "Table Name: " + tableName);
            if (condition != null) {
                System.out.println(" ".repeat(indent + 2) + "Condition:");
                condition.print(indent + 4);
            }
        }
    }

    public static class Delete extends Command {
        private final String tableName;
        private final Condition condition;

        public Delete(Token tableNameToken, Condition condition) {
            this.tableName = tableNameToken.getValue().toLowerCase();
            this.condition = condition;
        }
        public String getTableName() { return tableName; }
        public Condition getCondition() { return condition; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
    }
    public static class Update extends Command {
        private final String tableName;
        private final HashMap<String, String> nameValueList;
        private final Condition condition;
        public Update(Token tableNameToken, HashMap<String, String> nameValueList, Condition condition) {
            this.tableName = tableNameToken.getValue().toLowerCase();
            this.nameValueList = nameValueList;
            this.condition = condition;
        }
        public String getTableName() { return tableName; }
        public HashMap<String, String> getNameValueList() { return nameValueList; }
        public Condition getCondition() { return condition; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
    }
    public static class Join extends Command {
        private final String firstTableName;
        private final String secondTableName;
        private final String firstAttributeName;
        private final String secondAttributeName;
        public Join(Token firstTableNameToken, Token secondTableNameToken,
                    Token firstAttributeNameToken, Token secondAttributeNameToken) {
            this.firstTableName = firstTableNameToken.getValue().toLowerCase();
            this.secondTableName = secondTableNameToken.getValue().toLowerCase();
            this.firstAttributeName = firstAttributeNameToken.getValue();
            this.secondAttributeName = secondAttributeNameToken.getValue();
        }
        public String getFirstTableName() {
            return firstTableName;
        }
        public String getSecondTableName() {
            return secondTableName;
        }
        public String getFirstAttributeName() {
            return firstAttributeName;
        }
        public String getSecondAttributeName() {
            return secondAttributeName;
        }
        @Override
        public void accept(CommandVisitor visitor) throws IOException { visitor.visit(this); }
    }
}