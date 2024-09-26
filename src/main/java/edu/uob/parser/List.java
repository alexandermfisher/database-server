package edu.uob.parser;


// <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>
// <NameValuePair>   ::=  [AttributeName] "=" [Value]
// <WildAttribList>  ::=  <AttributeList> | "*"

import edu.uob.interpreter.CommandVisitor;
import edu.uob.tokenizer.TokenType;
import edu.uob.tokenizer.Tokenizer;

import java.io.IOException;
import java.util.ArrayList;

public abstract class List extends Node {
    @Override
    public void accept(CommandVisitor visitor) throws IOException {}
    public static abstract class IdentifierList extends List {
        private final ArrayList<String> list;
        protected IdentifierList() {
            list = new ArrayList<>();
        }
        protected void addElement(Tokenizer.Token token) {
            list.add(token.getValue());
        }
        protected ArrayList<String> getElements() {
            return list;
        }
        @Override
        public void print(int indent) {
            // Print the list and its children recursively
            for (String attribute : getElements()) {
                System.out.print(" ".repeat(indent)); // Indentation
                System.out.println(attribute);
            }
        }
    }
    // <AttributeList> ::=  [AttributeName] | [AttributeName] "," <AttributeList>
    public static class AttributeList extends IdentifierList {
        public AttributeList() { super(); }
        public ArrayList<String> getAttributes() { return super.getElements(); }
    }
    // <ValueList> ::=  [Value] | [Value] "," <ValueList>
    public static class ValueList extends IdentifierList {
        public ValueList() {
            super();
        }
        public ArrayList<String> getValues() {
            return super.getElements();
        }
        public void addElement(Tokenizer.Token token) {
            if (token.getType() == TokenType.STRING_LITERAL) {
                token.setValue(token.getValue().substring(1, token.getValue().length() - 1).strip());
            }
            super.addElement(token);
        }
    }
    // <WildAttribList>  ::=  <AttributeList> | "*"
    public static class WildAttributeList extends AttributeList {
        public WildAttributeList() {
            super();
        }
        public void addElement(Tokenizer.Token token) {
            token.setValue(token.getValue());
            super.addElement(token);
        }
    }
}


