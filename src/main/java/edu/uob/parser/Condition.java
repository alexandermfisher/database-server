package edu.uob.parser;

import edu.uob.interpreter.CommandVisitor;
import java.io.IOException;

//<Condition> ::= "(" <Expression> ")" | <Expression>
//<Expression> ::= <AttributeComparison> | <Condition> <BoolOperator> <Condition>
//<AttributeComparison> ::= "[" AttributeName "]" <Comparator> "[" Value "]"

public abstract class Condition extends Node {
    public enum BoolOperator { AND, OR }
    public enum Comparator { EQUAL, NOT_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LIKE, }

    //<Condition> <BoolOperator> <Condition>
    public static class Expression extends Condition {
        private final Condition leftCondition;
        private final BoolOperator boolOperator;
        private final Condition rightCondition;
        public Expression(Condition leftCondition, BoolOperator boolOperator, Condition rightCondition) {
            this.leftCondition = leftCondition;
            this.boolOperator = boolOperator;
            this.rightCondition = rightCondition;
        }
        public Condition getLeftCondition() { return leftCondition; }
        public BoolOperator getBoolOperator() { return boolOperator; }
        public Condition getRightCondition() { return rightCondition; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException {}
        @Override
        public void print(int indent) {
            leftCondition.print(indent);
            System.out.println(" ".repeat(indent) + "Bool Operator: " + boolOperator);
            rightCondition.print(indent);
        }
    }
    //<AttributeComparison> ::= "[" AttributeName "]" <Comparator> "[" Value "]"
    public static class AttributeValueComparison extends Condition {
        private final String attributeName;
        private final Comparator comparator;
        private final String value;
        public AttributeValueComparison(String attributeName, Comparator comparator, String value) {
            this.attributeName = attributeName.toLowerCase();
            this.comparator = comparator;
            this.value = value;
        }
        public String getAttributeName() { return attributeName; }
        public Comparator getComparator() { return comparator; }
        public String getValue() { return value; }
        @Override
        public void accept(CommandVisitor visitor) throws IOException {}
        @Override
        public void print(int indent) {
            System.out.print(" ".repeat(indent));
            System.out.println("Condition: AttributeValueComparison");
            System.out.println(" ".repeat(indent + 2) + "Attribute Name: " + attributeName);
            System.out.println(" ".repeat(indent + 2) + "Comparator: " + comparator);
            System.out.println(" ".repeat(indent + 2) + "Value: " + value);
        }
    }
}
