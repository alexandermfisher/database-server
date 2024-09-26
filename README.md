# CW-DB: A Simplified SQL Database Server

This project implements a simplified SQL database server, allowing users to execute queries using a custom query language. The server is built from scratch using Java, Maven, and JUnit, with persistent data storage handled via the file system.

## Features

The custom query language supports the following core SQL-like operations:

| **Command** | **Functionality** |
|-------------|-------------------|
| **USE**     | Changes the database against which the following queries will be run. |
| **CREATE**  | Constructs a new database or table, depending on the provided parameters. |
| **INSERT**  | Adds a new record (row) to an existing table. |
| **SELECT**  | Searches for records that match the given condition. |
| **UPDATE**  | Modifies the existing data in a table. |
| **ALTER**   | Changes the structure (columns) of an existing table by adding or dropping columns. |
| **DELETE**  | Removes records that match the given condition from a table. |
| **DROP**    | Removes a specified table from a database, or removes the entire database. |
| **JOIN**    | Performs an inner join on two tables, returning all permutations of matching records. |

### Additional Rules and Constraints

- **Primary Keys**: The primary key for every table is always called `id` and is auto-generated by the server.
- **Case Sensitivity**:
  - **Table and Database Names**: These are case-sensitive when querying but will be saved as lowercase in the file system.
  - **SQL Keywords**: Keywords like `SELECT`, `INSERT`, etc., are case-insensitive.
  - **Column Names**: These are case-insensitive for querying, but the case provided by the user will be preserved in the database.
- **Foreign Keys**: There is no explicit support for foreign keys in the query language. It is the responsibility of the user to manage table relationships and maintain consistency.
- **Invalid Comparisons**: When performing queries, if the data being compared is not valid (e.g., mismatched data types), no data will be returned.

### Persistent Data Storage

The server uses the file system to persistently store the database and its tables. All data is stored in a structured format within the file system to ensure data is retained between executions.

### Query Language Grammar

The query language used by this database server is defined by a simplified grammar that mimics SQL syntax. This includes commands such as `CREATE`, `INSERT`, `SELECT`, and more, which follow a structured pattern as specified in the Backus-Naur Form (BNF) document.

The full grammar can be found in the [`BNF.txt`](./BNF.txt) file in the root of this repository.

### Components

The database server project consists of the following main components:

1. **Parser**: The parser takes raw query input and breaks it down into syntactic elements based on the grammar. This is the initial step in the query execution pipeline.
2. **Interpreter**: The interpreter processes the parsed query elements and executes the appropriate actions, such as inserting records or selecting data from tables.
3. **File System for Persistent Storage**: The file system is used to store databases and tables, ensuring that data persists between executions. Each database is stored in a directory, and each table is stored as a file within that directory.

### Testing

Unit tests have been written using **JUnit** to validate the functionality of the server, ensuring that each query type behaves as expected. Comprehensive tests have been written for each core command, including edge cases and error handling.

## Example Queries

Here are a few examples of the supported query types:

1. **Create a New Database**:
    ```sql
    CREATE DATABASE school;
    ```

2. **Create a New Table**:
    ```sql
    CREATE TABLE students (id INT, name TEXT, age INT);
    ```

3. **Insert a Record into the Table**:
    ```sql
    INSERT INTO students (id, name, age) VALUES (1, 'Alice', 20);
    ```

4. **Select Records from a Table**:
    ```sql
    SELECT * FROM students WHERE age > 18;
    ```

5. **Update a Record**:
    ```sql
    UPDATE students SET age = 21 WHERE name = 'Alice';
    ```

6. **Delete a Record**:
    ```sql
    DELETE FROM students WHERE id = 1;
    ```

7. **Drop a Table**:
    ```sql
    DROP TABLE students;
    ```

## Running the Project

### Prerequisites

- **Java 17**: Ensure that Java 17 is installed.
- **Maven**: Use the included Maven wrapper to handle the build and dependencies.

### Build and Run

1. **Clone the Repository**:
    ```bash
    git clone https://github.com/your-username/your-repo.git
    cd your-repo
    ```

2. **Build the Project**:
    Use the Maven wrapper to build the project:
    ```bash
    ./mvnw clean package
    ```

3. **Run the Server**:
    You can run the server using the Maven exec plugin:
    ```bash
    ./mvnw exec:java -Dexec.mainClass="edu.uob.DBServer"
    ```

4. **Run the Client**:
    To run the client for interacting with the server:
    ```bash
    ./mvnw exec:java -Dexec.mainClass="edu.uob.DBClient"
    ```

5. **Run Unit Tests**:
    Unit tests ensure that all components are functioning correctly:
    ```bash
    ./mvnw test
    ```

## Future Enhancements

Possible future improvements include:
- Expanding the supported SQL commands.
- Implementing more advanced query optimization.
- Supporting more complex JOIN operations (e.g., outer joins).

## Acknowledgments

This project is an assignment from the **Object-Oriented Programming with Java** course at the University of Bristol (2022), taught by **Dr. Simon Lock** and **Dr Sion Hannuna**. The project was built using a base Maven project template provided by the course.

Special thanks to Simon for guidance and support throughout the project. You can find more about his work and contributions on his [GitHub profile](https://github.com/drslock).

### Project Template

The project was built using a starter Maven project template, which includes a pre-configured `pom.xml` and basic project structure. If you'd like to use the same starting point, you can download the template from the repository's root as a `.zip` file: [Download the Maven Template](./maven-template.zip).
