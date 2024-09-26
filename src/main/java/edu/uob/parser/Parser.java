package edu.uob.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import edu.uob.tokenizer.Tokenizer;
import edu.uob.tokenizer.TokenType;
import edu.uob.utils.DBException;

import static edu.uob.utils.ErrorType.*;

public class Parser {
    private ArrayList<Tokenizer.Token> tokens;
    private int currentTokenIndex;
    private int parenthesisCount;
    public Parser() {
        tokens = new ArrayList<>();
        currentTokenIndex = 0;
    }
    public Command parse(ArrayList<Tokenizer.Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        return parseCommand();
    }
    private Command parseCommand() {
        // <Command> ::= <CommandType> ";"
        Command command = parseCommandType();
        consumeToken(TokenType.SEMICOLON);
        return command;
    }
    private Command parseCommandType() {
        // <CommandType> ::= <Use> | <Create> | <Drop> | <Alter> | <Insert> | <Select> | <Update> | <Delete> | <Join>
        TokenType currTokenType = tokens.get(currentTokenIndex).getType();
        return switch (currTokenType) {
            case USE_KEYWORD -> parseUse();
            case CREATE_KEYWORD -> parseCreate();
            case DROP_KEYWORD -> parseDrop();
            case ALTER_KEYWORD -> parseAlter();
            case INSERT_KEYWORD -> parseInsert();
            case SELECT_KEYWORD -> parseSelect();
            case DELETE_KEYWORD -> parseDelete();
            case UPDATE_KEYWORD -> parseUpdate();
            case JOIN_KEYWORD -> parseJoin();
            default -> throw new DBException(INVALID_QUERY_EXCEPTION);
        };
    }
    private Command.Use parseUse() {
        // <Use> ::= "USE " [DatabaseName]
        consumeToken(TokenType.USE_KEYWORD);
        Tokenizer.Token indentifierToken = consumeToken(TokenType.IDENTIFIER);
        return new Command.Use(indentifierToken);
    }
    private Command parseCreate() {
        // <Create> ::= <CreateDatabase> | <CreateTable>
        consumeToken(TokenType.CREATE_KEYWORD);
        Tokenizer.Token createToken = consumeToken(new TokenType[]{TokenType.DATABASE_KEYWORD, TokenType.TABLE_KEYWORD});
        Tokenizer.Token identifierToken = consumeToken(TokenType.IDENTIFIER);

        // "CREATE " "DATABASE " [DatabaseName]
        if (createToken.getType() == TokenType.DATABASE_KEYWORD) return new Command.CreateDatabase(identifierToken);

        // "CREATE " "TABLE " [TableName]
        if (tokens.get(currentTokenIndex).getType() == TokenType.SEMICOLON)
            return new Command.CreateTable(identifierToken);

        // "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
        Command.CreateTable command = new Command.CreateTable(identifierToken);
        consumeToken(TokenType.LEFT_PAREN);
        parseList(command.getAttributes(), tokenType -> tokenType == TokenType.IDENTIFIER, TokenType.RIGHT_PAREN);
        if (command.getAttributes().getAttributes().isEmpty()) throw new DBException(INVALID_LIST_EXCEPTION);
        return command;
    }
    private Command parseDrop() {
        // "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        consumeToken(TokenType.DROP_KEYWORD);
        Tokenizer.Token dropType = consumeToken(new TokenType[]{TokenType.DATABASE_KEYWORD, TokenType.TABLE_KEYWORD});
        Tokenizer.Token identifierToken = consumeToken(TokenType.IDENTIFIER);

        // "DROP " "DATABASE " [DatabaseName]
        if (dropType.getType() == TokenType.DATABASE_KEYWORD) return new Command.DropDatabase(identifierToken);

        // "DROP " "TABLE " [TableName]
        return new Command.DropTable(identifierToken);
    }
    // <Alter> ::=  "ALTER " "TABLE " [TableName] " " <"ADD" | "DROP"> " " [AttributeName]
    private Command parseAlter() {
        consumeToken(TokenType.ALTER_KEYWORD);
        consumeToken(TokenType.TABLE_KEYWORD);
        Tokenizer.Token tableName = consumeToken(TokenType.IDENTIFIER);
        Tokenizer.Token alterType = consumeToken(new TokenType[]{TokenType.ADD_KEYWORD, TokenType.DROP_KEYWORD});
        Tokenizer.Token attributeName = consumeToken(TokenType.IDENTIFIER);
        // ADD Attribute:
        if (alterType.getType() == TokenType.ADD_KEYWORD) { return new Command.AddAttribute(tableName, attributeName); }
        // Drop Attribute:
        return new Command.DropAttribute(tableName, attributeName);
    }
    //    <Insert> ::=  "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
    private Command parseInsert() {
        consumeToken(TokenType.INSERT_KEYWORD);
        consumeToken(TokenType.INTO_KEYWORD);
        Tokenizer.Token tableName = consumeToken(TokenType.IDENTIFIER);
        consumeToken(TokenType.VALUES_KEYWORD);
        consumeToken(TokenType.LEFT_PAREN);
        Command.Insert command = new Command.Insert(tableName);

        parseList(command.getValues(), tokenType -> tokenType == TokenType.STRING_LITERAL
                || tokenType == TokenType.BOOLEAN_LITERAL || tokenType == TokenType.FLOAT_LITERAL
                || tokenType == TokenType.INTEGER_LITERAL || tokenType == TokenType.NULL_LITERAL, TokenType.RIGHT_PAREN);

        if (command.getValues().getValues().isEmpty()) throw new DBException(INVALID_LIST_EXCEPTION);
        return command;
    }
    private Command parseSelect() {
        // <Select> ::=  "SELECT " <WildAttribList> " FROM " [TableName] |
        consumeToken(TokenType.SELECT_KEYWORD);

        boolean selectAll = false;
        List.WildAttributeList attributesList = new List.WildAttributeList();
        if (tokens.get(currentTokenIndex).getType() == TokenType.ASTRIX) {
            consumeToken(TokenType.ASTRIX);
            selectAll = true;
        } else {
            parseList(attributesList, tokenType -> tokenType == TokenType.IDENTIFIER, TokenType.FROM_KEYWORD);
            if (attributesList.getAttributes().isEmpty()) throw new DBException(INVALID_LIST_EXCEPTION);
            currentTokenIndex--;
        }

        consumeToken(TokenType.FROM_KEYWORD);
        Tokenizer.Token tableNameToken = consumeToken(TokenType.IDENTIFIER);

        if (tokens.get(currentTokenIndex).getType() == TokenType.SEMICOLON)
            return new Command.Select(attributesList, tableNameToken, null, selectAll);

        // "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        consumeToken(TokenType.WHERE_KEYWORD);
        Condition condition = parseCondition();
        //condition.print(5);
        if (parenthesisCount != 0) throw new DBException(INVALID_QUERY_EXCEPTION);

        return new Command.Select(attributesList, tableNameToken, condition, selectAll);
    }
    private Command parseDelete() {
        // <Delete> ::= "DELETE " "FROM " [TableName] " WHERE " <Condition>
        consumeToken(TokenType.DELETE_KEYWORD);
        consumeToken(TokenType.FROM_KEYWORD);
        Tokenizer.Token tableNameToken = consumeToken(TokenType.IDENTIFIER);

        consumeToken(TokenType.WHERE_KEYWORD);
        Condition condition = parseCondition();
        if (parenthesisCount != 0) throw new DBException(INVALID_QUERY_EXCEPTION);

        return new Command.Delete(tableNameToken, condition);
    }
    private Command parseUpdate() {
        // <Update> ::= "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>
        consumeToken(TokenType.UPDATE_KEYWORD);
        Tokenizer.Token tableNameToken = consumeToken(TokenType.IDENTIFIER);

        consumeToken(TokenType.SET_KEYWORD);
        HashMap<String, String> nameValueList = new HashMap<>();
        parseNameValueList(nameValueList);

        Condition condition = parseCondition();
        if (parenthesisCount != 0) throw new DBException(INVALID_QUERY_EXCEPTION);

        return new Command.Update(tableNameToken, nameValueList, condition);
    }
    private Command parseJoin() {
        consumeToken(TokenType.JOIN_KEYWORD);

        Tokenizer.Token firstTableNameToken = consumeToken(TokenType.IDENTIFIER);
        consumeToken(TokenType.AND_KEYWORD);
        Tokenizer.Token secondTableNameToken = consumeToken(TokenType.IDENTIFIER);
        consumeToken(TokenType.ON_KEYWORD);

        Tokenizer.Token firstAttributeNameToken = consumeToken(TokenType.IDENTIFIER);
        consumeToken(TokenType.AND_KEYWORD);

        // Parse second attribute name
        Tokenizer.Token secondAttributeNameToken = consumeToken(TokenType.IDENTIFIER);

        return new Command.Join(firstTableNameToken, secondTableNameToken,
            firstAttributeNameToken, secondAttributeNameToken
        );
    }
    private void parseNameValueList(HashMap<String, String> nameValueList) {
        Tokenizer.Token currToken;
        while ((currToken = consumeToken()).getType() != TokenType.EOF) {
            if (currToken.getType() == TokenType.WHERE_KEYWORD) return;

            if (currToken.getType() != TokenType.IDENTIFIER) throw new DBException(INVALID_LIST_EXCEPTION);
            String attributeName = currToken.getValue().toLowerCase();
            consumeToken(TokenType.ASSIGN_KEYWORD);
            Tokenizer.Token valueToken = consumeToken(new TokenType[] {TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL,
                                         TokenType.FLOAT_LITERAL, TokenType.INTEGER_LITERAL,
                                         TokenType.NULL_LITERAL});
            String attributeValue = valueToken.getValue();

            // Add name-value pair to the list
            nameValueList.put(attributeName, attributeValue);

            // check for comma between attributes, but not at end:
            if (tokens.get(currentTokenIndex).getType() != TokenType.WHERE_KEYWORD) { consumeToken(TokenType.COMMA); }
        }
        throw new DBException(INVALID_LIST_EXCEPTION);
    }
    public Condition parseCondition() {
        java.util.List<Condition> conditions = new ArrayList<>();
        java.util.List<Condition.BoolOperator> boolOperators = new ArrayList<>();

        while (true) {
            if (tokens.get(currentTokenIndex).getType() == TokenType.LEFT_PAREN) {
                consumeToken(TokenType.LEFT_PAREN);
                parenthesisCount++;
                Condition nestedCondition = parseCondition();
                conditions.add(nestedCondition);
            } else {
                Condition leftAttributeComparison = parseAttributeComparison();
                conditions.add(leftAttributeComparison);
            }

            if (tokens.get(currentTokenIndex).getType() != TokenType.AND_KEYWORD &&
                     tokens.get(currentTokenIndex).getType() != TokenType.OR_KEYWORD) {
                break; // No more boolean operators or closing parenthesis, exit the loop
            }
            Condition.BoolOperator boolOperator = parseBoolOperator();
            boolOperators.add(boolOperator);
        }

        if (tokens.get(currentTokenIndex).getType() == TokenType.RIGHT_PAREN) {
            consumeToken(TokenType.RIGHT_PAREN);
            parenthesisCount--;
        }

        // Combine conditions and boolean operators into a single expression
        Condition finalCondition = conditions.get(0);
        for (int i = 0; i < boolOperators.size(); i++) {
            Condition.BoolOperator boolOperator = boolOperators.get(i);
            Condition rightCondition = conditions.get(i + 1);
            finalCondition = new Condition.Expression(finalCondition, boolOperator, rightCondition);
        }
        return finalCondition;
    }
    private Condition parseAttributeComparison() {
        // <AttributeComparison> ::= [AttributeName] <Comparator> [Value]
        Tokenizer.Token attributeNameToken = consumeToken(TokenType.IDENTIFIER);
        Condition.Comparator comparator = parseComparator();
        Tokenizer.Token valueToken = consumeToken(new TokenType[] {TokenType.STRING_LITERAL, TokenType.BOOLEAN_LITERAL,
                                         TokenType.FLOAT_LITERAL, TokenType.INTEGER_LITERAL,
                                         TokenType.NULL_LITERAL});
        if (valueToken.getType() == TokenType.STRING_LITERAL) {
            valueToken.setValue(valueToken.getValue().substring(1, valueToken.getValue().length() - 1).strip());
        }

        return new Condition.AttributeValueComparison(attributeNameToken.getValue(), comparator, valueToken.getValue());
    }
    private Condition.BoolOperator parseBoolOperator() {
        TokenType currentTokenType = tokens.get(currentTokenIndex).getType();
        return switch (currentTokenType) {
            case AND_KEYWORD -> {
                consumeToken(TokenType.AND_KEYWORD);
                yield Condition.BoolOperator.AND;
            }
            case OR_KEYWORD -> {
                consumeToken(TokenType.OR_KEYWORD);
                yield Condition.BoolOperator.OR;
            }
            default ->
                    throw new DBException(INVALID_QUERY_EXCEPTION, "Expected boolean operator but found: " + currentTokenType);
        };
    }
    private Condition.Comparator parseComparator() {
        TokenType currentTokenType = tokens.get(currentTokenIndex).getType();
        return switch (currentTokenType) {
            case EQUALS_OPERATOR -> {
                consumeToken(TokenType.EQUALS_OPERATOR);
                yield Condition.Comparator.EQUAL;
            }
            case NOT_EQUALS_OPERATOR -> {
                consumeToken(TokenType.NOT_EQUALS_OPERATOR);
                yield Condition.Comparator.NOT_EQUAL;
            }
            case LESS_THAN_OPERATOR -> {
                consumeToken(TokenType.LESS_THAN_OPERATOR);
                yield Condition.Comparator.LESS_THAN;
            }
            case LESS_THAN_OR_EQUAL_OPERATOR -> {
                consumeToken(TokenType.LESS_THAN_OR_EQUAL_OPERATOR);
                yield Condition.Comparator.LESS_THAN_OR_EQUAL;
            }
            case GREATER_THAN_OPERATOR -> {
                consumeToken(TokenType.GREATER_THAN_OPERATOR);
                yield Condition.Comparator.GREATER_THAN;
            }
            case GREATER_THAN_OR_EQUAL_OPERATOR -> {
                consumeToken(TokenType.GREATER_THAN_OR_EQUAL_OPERATOR);
                yield Condition.Comparator.GREATER_THAN_OR_EQUAL;
            }
            case LIKE_OPERATOR -> {
                consumeToken(TokenType.LIKE_OPERATOR);
                yield Condition.Comparator.LIKE;
            }
            default -> throw new DBException(INVALID_QUERY_EXCEPTION, "Expected comparator but found: " + currentTokenType);
        };
    }
    @FunctionalInterface interface TokenCondition { boolean test(TokenType tokenType); }
    private void parseList(List.IdentifierList list, TokenCondition isValid, TokenType endingToken) {
        Tokenizer.Token currToken;
        while ((currToken = consumeToken()).getType() != TokenType.EOF) {
            if (currToken.getType() == endingToken) return;

            // Attribute expected --> add to list:
            if (isValid.test(currToken.getType())) {
                list.addElement(currToken);
                // check for comma between attributes, but not at end:
                if (tokens.get(currentTokenIndex).getType() != endingToken) { consumeToken(TokenType.COMMA); }
            } else {
                throw new DBException(INVALID_LIST_EXCEPTION);
            }
        }
        throw new DBException(INVALID_LIST_EXCEPTION);
    }
    private Tokenizer.Token consumeToken() { return tokens.get(currentTokenIndex++); }
    private Tokenizer.Token consumeToken(TokenType expectedTokenType) {
        Tokenizer.Token currentToken = tokens.get(currentTokenIndex);
        if (currentToken.getType() == expectedTokenType) {
            currentTokenIndex++;
            return currentToken;
        }
        throw new DBException(INVALID_QUERY_EXCEPTION,
                "Syntax error: Expected " + expectedTokenType + " but found " + currentToken.getType());
    }
    private Tokenizer.Token consumeToken(TokenType[] expectedTokenType) {
        Tokenizer.Token currentToken = tokens.get(currentTokenIndex);
        for (TokenType expected : expectedTokenType) {
            if (currentToken.getType() == expected) {
                currentTokenIndex++;
                return currentToken;
            }
        }
        throw new DBException(INVALID_QUERY_EXCEPTION,
                "Syntax error: Expected " + Arrays.toString(expectedTokenType) + " but found " + currentToken.getType().getTokenType());
    }
}
