import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.stream.IntStream;

import static spark.Spark.*;

import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;

import static spark.Spark.get;

import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {

    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/public");

        get("/hello", (req, res) -> "Hello , PINT team, you are awesome");

        get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("message", "Hello PINT team!");

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

        get("/db", (req, res) -> {
            Connection connection = null;
            Map<String, Object> attributes = new HashMap<>();
            try {
                connection = DatabaseUrl.extract().getConnection();

                ArrayList<String> output = new ArrayList<String>();
                String line;

                output.add("Creating DB");

                Statement stmt = connection.createStatement();
                line = "CREATE TABLE IF NOT EXISTS account ( id VARCHAR(40) PRIMARY KEY," +
                        "name VARCHAR(100)," +
                        "created_at timestamp," +
                        "created_by VARCHAR(20))"
                ;
                execOutput(stmt, output, line);
                output.add("");

                IntStream.range(0, 5).forEach(i ->
                {
                    try {

                        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                        final String lineIn = String.format("INSERT INTO account(id, name, created_at, created_by) " +
                                "VALUES ('%s', '%s', now(), 'xuehai');", uuid, "name_" + Integer.toString(i));

                        execOutput(stmt, output, lineIn);
                    } catch (SQLException e) {
                        output.add(e.toString());
                        e.printStackTrace();
                    }
                });


                ResultSet rs = stmt.executeQuery("SELECT name, id, created_at, created_by FROM account;");
                output.add("");
                output.add("Reading from DB");
                output.add("id, name, created_at, created_by");

                while (rs.next()) {
                    output.add(String.format("%s,%s,%s,%s", rs.getString("id"),
                            rs.getString("name"),
                            rs.getTimestamp("created_at"),
                            rs.getString("created_by")));
                }

                attributes.put("results", output);
                return new ModelAndView(attributes, "db.ftl");
            } catch (Exception e) {
                attributes.put("message", "There was an error: " + e);
                return new ModelAndView(attributes, "error.ftl");
            } finally {
                if (connection != null) try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }, new FreeMarkerEngine());

    }

    private static void execOutput(Statement statement, ArrayList<String> output, String line) throws SQLException {
        output.add(line);
        statement.executeUpdate(line);

    }

}
