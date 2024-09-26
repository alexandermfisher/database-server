package edu.uob.interpreter;

import edu.uob.parser.Command;

import java.io.IOException;

public interface CommandVisitor {
    void visit(Command.Use useCommand) throws IOException;
    void visit(Command.CreateDatabase createDatabaseCommand) throws IOException;
    void visit(Command.CreateTable createTableCommand) throws IOException;
    void visit(Command.Drop drop) throws IOException;
    void visit(Command.Alter alter) throws IOException;
    void visit(Command.Insert insert) throws IOException;
    void visit(Command.Select select) throws IOException;
    void visit(Command.Delete delete) throws IOException;
    void visit(Command.Update update) throws IOException;
    void visit(Command.Join join) throws IOException;
}