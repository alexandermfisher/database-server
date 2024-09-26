package edu.uob.tokenizer;

import java.util.ArrayList;
import java.util.Arrays;

public class Tokenizer {
    private static final String[] specialCharacters = {"(", ")", ",", ";"};
    public static ArrayList<Token> tokenize(String query) {
        ArrayList<String> preprocessedTokens = new ArrayList<String>();
        query = query.trim();
        String[] fragments = query.split("'");
        for (int i = 0; i < fragments.length; i++) {
            if (i % 2 != 0) preprocessedTokens.add("'" + fragments[i] + "'");
                //
            else {
                String[] nextBatchOfTokens = tokenizeFragments(fragments[i]);
                preprocessedTokens.addAll(Arrays.asList(nextBatchOfTokens));
            }
        }

        // generate List<Tokens>
        ArrayList<Token> tokens = new ArrayList<>();
        for (String token : preprocessedTokens) { tokens.add(new Token(token)); }
        tokens.add(new Token("EOF"));
        //printTokens(tokens);
        return tokens;
    }
    private static String[] tokenizeFragments(String input) {
        for (String specialCharacter : specialCharacters) {
            input = input.replace(specialCharacter, " " + specialCharacter + " ");
            input = input.replaceAll("(==|>=|<=|>|<|!=|=)", " $1 ");
            input = input.replaceAll("\t", " ");
        }

        while (input.contains("  ")) input = input.replaceAll("  ", " ");
        input = input.trim();
        return input.split(" ");
    }

    public static void printTokens(ArrayList<Token> tokens) {for (Token token : tokens) { System.out.println(token.getType() + ": " + token.getValue()); }};

    public static class Token {
        private TokenType type;
        private String token;
        public Token(String token) {
            this.token = token;
            setType();
        }
        public TokenType getType() { return type; }
        public String getValue() { return token; }
        public void setValue(String value) { this.token = value; }
            private void setType() {
            switch (token.toUpperCase()) {
                // Command Keywords:
                case "USE":
                    type = TokenType.USE_KEYWORD;
                    break;
                case "CREATE":
                    type = TokenType.CREATE_KEYWORD;
                    break;
                case "DROP":
                    type = TokenType.DROP_KEYWORD;
                    break;
                case "ALTER":
                    type = TokenType.ALTER_KEYWORD;
                    break;
                case "INSERT":
                    type = TokenType.INSERT_KEYWORD;
                    break;
                case "SELECT":
                    type = TokenType.SELECT_KEYWORD;
                    break;
                case "UPDATE":
                    type = TokenType.UPDATE_KEYWORD;
                    break;
                case "DELETE":
                    type = TokenType.DELETE_KEYWORD;
                    break;
                case "JOIN":
                    type = TokenType.JOIN_KEYWORD;
                    break;

                // Language Keywords:
                case "DATABASE":
                    type = TokenType.DATABASE_KEYWORD;
                    break;
                case "TABLE":
                    type = TokenType.TABLE_KEYWORD;
                    break;
                case "INTO":
                    type = TokenType.INTO_KEYWORD;
                    break;
                case "VALUES":
                    type = TokenType.VALUES_KEYWORD;
                    break;
                case "FROM":
                    type = TokenType.FROM_KEYWORD;
                    break;
                case "WHERE":
                    type = TokenType.WHERE_KEYWORD;
                    break;
                case "SET":
                    type = TokenType.SET_KEYWORD;
                    break;
                case "AND":
                    type = TokenType.AND_KEYWORD;
                    break;
                case "OR":
                    type = TokenType.OR_KEYWORD;
                    break;
                case "ADD":
                    type = TokenType.ADD_KEYWORD;
                    break;
                case "ON":
                    type = TokenType.ON_KEYWORD;
                    break;

                // Literals:
                case "TRUE":
                case "FALSE":
                    type = TokenType.BOOLEAN_LITERAL;
                    break;
                case "NULL":
                    type = TokenType.NULL_LITERAL;
                    break;
                case "EOF":
                    type = TokenType.EOF;
                    break;

                // Operators:
                case "==":
                    type = TokenType.EQUALS_OPERATOR;
                    break;
                case "!=":
                    type = TokenType.NOT_EQUALS_OPERATOR;
                    break;
                case "<":
                    type = TokenType.LESS_THAN_OPERATOR;
                    break;
                case "<=":
                    type = TokenType.LESS_THAN_OR_EQUAL_OPERATOR;
                    break;
                case ">":
                    type = TokenType.GREATER_THAN_OPERATOR;
                    break;
                case ">=":
                    type = TokenType.GREATER_THAN_OR_EQUAL_OPERATOR;
                    break;
                case "LIKE":
                    type = TokenType.LIKE_OPERATOR;
                    break;

                // Delimiters and Punctuation:
                case ";":
                    type = TokenType.SEMICOLON;
                    break;
                case ",":
                    type = TokenType.COMMA;
                    break;
                case "(":
                    type = TokenType.LEFT_PAREN;
                    break;
                case ")":
                    type = TokenType.RIGHT_PAREN;
                    break;
                case "{":
                    type = TokenType.LEFT_BRACE;
                    break;
                case "}":
                    type = TokenType.RIGHT_BRACE;
                    break;
                case "[":
                    type = TokenType.LEFT_BRACKET;
                    break;
                case "]":
                    type = TokenType.RIGHT_BRACKET;
                    break;
                case "*":
                    type = TokenType.ASTRIX;
                    break;
                case  "=":
                    type = TokenType.ASSIGN_KEYWORD;
                    break;

                default:
                    if (token.matches("[+-]?\\d+")) {
                        type = TokenType.INTEGER_LITERAL;
                    } else if (token.matches("[+-]?\\d+(\\.\\d+)?")) {
                        type = TokenType.FLOAT_LITERAL;
                    } else if (token.startsWith("'") && token.endsWith("'")) {
                        type = TokenType.STRING_LITERAL;
                    } else if (token.matches("[a-zA-Z0-9]+")) {
                        type = TokenType.IDENTIFIER;
                    } else {
                        type = TokenType.INVALID_TOKEN;
                        break;
                    }
            }
        }
    }
}