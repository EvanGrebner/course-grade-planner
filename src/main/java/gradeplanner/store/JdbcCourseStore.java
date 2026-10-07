// Purpose: Saves and loads courses in a PostgreSQL database using JDBC.
// JDBC is the standard Java way to send SQL to a database.
// The database settings come from three environment variables and default to
// the database in docker-compose.yml.

package gradeplanner.store;

import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.model.Course;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcCourseStore implements CourseStore {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gradeplanner";
    private static final String DEFAULT_USER = "gradeplanner";
    private static final String DEFAULT_PASSWORD = "gradeplanner";

    private final Connection connection;
    private final String url;

    public JdbcCourseStore(Connection connection, String url) {
        this.connection = connection;
        this.url = url;
    }

    /** Connects to the database and makes sure the tables exist */
    public static JdbcCourseStore connect() throws SQLException {
        String url = readSetting("GRADEPLANNER_DB_URL", DEFAULT_URL);
        String user = readSetting("GRADEPLANNER_DB_USER", DEFAULT_USER);
        String password = readSetting("GRADEPLANNER_DB_PASSWORD", DEFAULT_PASSWORD);

        // stop waiting after 5 seconds so the app does not hang when the database is off
        DriverManager.setLoginTimeout(5);
        Connection connection = DriverManager.getConnection(url, user, password);

        JdbcCourseStore store = new JdbcCourseStore(connection, url);
        store.createTables();
        return store;
    }

    // uses the fallback when the environment variable is missing or blank
    private static String readSetting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    // schema.sql only uses CREATE TABLE IF NOT EXISTS, so running it again is safe
    void createTables() throws SQLException {
        try (InputStream file = JdbcCourseStore.class.getResourceAsStream("/schema.sql")) {
            if (file == null) {
                throw new SQLException("schema.sql not found on the classpath");
            }
            String script = new String(file.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String sql : script.split(";")) {
                    String cleaned = removeComments(sql).trim();
                    if (!cleaned.isEmpty()) {
                        statement.execute(cleaned);
                    }
                }
            }
        } catch (IOException e) {
            throw new SQLException("could not read schema.sql", e);
        }
    }

    // takes out the -- comments so each piece of the script is only SQL
    private static String removeComments(String sql) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            int commentStart = line.indexOf("--");
            if (commentStart >= 0) {
                line = line.substring(0, commentStart);
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString();
    }

    // loads every course with its categories and assignments
    @Override
    public List<Course> listCourses() {
        try {
            Map<Long, Course> coursesById = new HashMap<>();
            List<Course> courses = new ArrayList<>();

            // 1. Read every course
            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(
                         "SELECT id, name, target_grade FROM courses ORDER BY id")) {
                while (results.next()) {
                    Course course = new Course(results.getString("name"), results.getDouble("target_grade"));
                    course.setId(results.getLong("id"));
                    coursesById.put(course.getId(), course);
                    courses.add(course);
                }
            }

            // 2. Read every category and attach it to its course
            Map<Long, Category> categoriesById = new HashMap<>();
            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(
                         "SELECT id, course_id, name, weight, drop_lowest FROM categories ORDER BY id")) {
                while (results.next()) {
                    Course course = coursesById.get(results.getLong("course_id"));
                    if (course == null) {
                        continue;
                    }
                    Category category = new Category(results.getString("name"),
                            results.getDouble("weight"), results.getInt("drop_lowest"));
                    category.setId(results.getLong("id"));
                    course.getCategories().add(category);
                    categoriesById.put(category.getId(), category);
                }
            }

            // 3. Read every assignment and attach it to its category
            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(
                         "SELECT id, category_id, name, earned, possible, extra_credit "
                                 + "FROM assignments ORDER BY id")) {
                while (results.next()) {
                    Category category = categoriesById.get(results.getLong("category_id"));
                    if (category == null) {
                        continue;
                    }

                    // earned can be empty in the database, which means not graded yet
                    double earnedValue = results.getDouble("earned");
                    Double earned = earnedValue;
                    if (results.wasNull()) {
                        earned = null;
                    }

                    Assignment assignment = new Assignment(results.getString("name"), earned,
                            results.getDouble("possible"), results.getBoolean("extra_credit"));
                    assignment.setId(results.getLong("id"));
                    category.getAssignments().add(assignment);
                }
            }
            return courses;
        } catch (SQLException e) {
            throw new StoreException("Could not load courses", e);
        }
    }

    // courses

    @Override
    public Course createCourse(String name, double targetGrade) {
        Course course = new Course(name, targetGrade);
        String sql = "INSERT INTO courses (name, target_grade) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setDouble(2, targetGrade);
            statement.executeUpdate();
            course.setId(newId(statement));
            return course;
        } catch (SQLException e) {
            throw new StoreException("Could not create course", e);
        }
    }

    @Override
    public void updateCourse(Course course) {
        String sql = "UPDATE courses SET name = ?, target_grade = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, course.getName());
            statement.setDouble(2, course.getTargetGrade());
            statement.setLong(3, course.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException("Could not save course", e);
        }
    }

    @Override
    public void deleteCourse(Course course) {
        deleteRow("DELETE FROM courses WHERE id = ?", course.getId(), "course");
    }

    // categories

    @Override
    public Category addCategory(Course course, String name, double weight, int dropLowest) {
        Category category = new Category(name, weight, dropLowest);
        String sql = "INSERT INTO categories (course_id, name, weight, drop_lowest) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, course.getId());
            statement.setString(2, name);
            statement.setDouble(3, weight);
            statement.setInt(4, dropLowest);
            statement.executeUpdate();
            category.setId(newId(statement));
        } catch (SQLException e) {
            throw new StoreException("Could not add category", e);
        }
        course.getCategories().add(category);
        return category;
    }

    @Override
    public void updateCategory(Category category) {
        String sql = "UPDATE categories SET name = ?, weight = ?, drop_lowest = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setDouble(2, category.getWeight());
            statement.setInt(3, category.getDropLowest());
            statement.setLong(4, category.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException("Could not save category", e);
        }
    }

    @Override
    public void deleteCategory(Course course, Category category) {
        deleteRow("DELETE FROM categories WHERE id = ?", category.getId(), "category");
        course.getCategories().remove(category);
    }

    // assignments

    @Override
    public Assignment addAssignment(Category category, String name, Double earned, double possible,
                                    boolean extraCredit) {
        Assignment assignment = new Assignment(name, earned, possible, extraCredit);
        String sql = "INSERT INTO assignments (category_id, name, earned, possible, extra_credit) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, category.getId());
            statement.setString(2, name);
            setEarned(statement, 3, earned);
            statement.setDouble(4, possible);
            statement.setBoolean(5, extraCredit);
            statement.executeUpdate();
            assignment.setId(newId(statement));
        } catch (SQLException e) {
            throw new StoreException("Could not add assignment", e);
        }
        category.getAssignments().add(assignment);
        return assignment;
    }

    @Override
    public void updateAssignment(Assignment assignment) {
        String sql = "UPDATE assignments SET name = ?, earned = ?, possible = ?, extra_credit = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, assignment.getName());
            setEarned(statement, 2, assignment.getEarned());
            statement.setDouble(3, assignment.getPossible());
            statement.setBoolean(4, assignment.isExtraCredit());
            statement.setLong(5, assignment.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException("Could not save assignment", e);
        }
    }

    @Override
    public void deleteAssignment(Category category, Assignment assignment) {
        deleteRow("DELETE FROM assignments WHERE id = ?", assignment.getId(), "assignment");
        category.getAssignments().remove(assignment);
    }

    // shown in the status bar so you can see where the data is saved
    @Override
    public String describe() {
        return "PostgreSQL (" + url + ")";
    }

    private void deleteRow(String sql, long id, String what) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException("Could not delete " + what, e);
        }
    }

    // a missing score is saved as an empty value in the database
    private static void setEarned(PreparedStatement statement, int position, Double earned) throws SQLException {
        if (earned == null) {
            statement.setNull(position, Types.NUMERIC);
        } else {
            statement.setDouble(position, earned);
        }
    }

    // gets the id that the database gave to the row we just added
    private static long newId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("No generated key returned");
        }
    }
}
