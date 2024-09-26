package edu.uob.parser;

import edu.uob.interpreter.CommandVisitor;

import java.io.IOException;

public abstract class Node {
    public abstract void accept(CommandVisitor visitor) throws IOException;
    public abstract void print(int indent);
}
