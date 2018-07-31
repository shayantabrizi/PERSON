/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.social_textual;

/**
 *
 * @author shayan (Based on a code provided by Ali Khodaei to us)
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class MySQLConnector {
    private static final String serverAddress = "localhost:3306";
    private static final String username = "root";
    private static final String password = "";

    static Connection conn = null;
    protected Scanner fileScanner;

    public static Connection connect(String database_name) {

        try {
// Load the JDBC driver
            String driverName = "com.mysql.jdbc.Driver"; // MySQL MM JDBC driver
            //String driverName = "com.mysql.jdbc.Driver";
            Class.forName(driverName);

// Create a connection to the database
            String url = "jdbc:mysql://" + serverAddress + "/" + database_name + "?verifyServerCertificate=false&useSSL=true"; // a JDBC url
            conn = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            System.out.println("class not found exception:");
            e.printStackTrace();
            throw new RuntimeException();
// Could not find the database driver
        } catch (SQLException e) {
            System.out.println("SQL exception (could not find database");
            e.printStackTrace();
            throw new RuntimeException();
// Could not connect to the database
        }
        return conn;

    }//connect

    public static void disconnect() {

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException sqlEx) {
                // ignore
            }
        }
    }//disconnect

    public static void testQuery() {

        String query = "select * from small_tag limit 10";
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            String value;
            while (rs.next()) {
                value = rs.getString(2);
                System.out.println(value);
            } //end while
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            e.printStackTrace();
            throw new RuntimeException();
        }

    }//testQuery

    private MySQLConnector() {
    }
}
