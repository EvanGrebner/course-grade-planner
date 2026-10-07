// Purpose: Keeps all the courses in memory only.
// The app uses this when the database is not running.
// Nothing is saved after the app closes.

package gradeplanner.store;

import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.model.Course;

import java.util.ArrayList;
import java.util.List;

public class InMemoryCourseStore implements CourseStore {

    private final List<Course> courses = new ArrayList<>();
    private long lastId = 0;

    // gives every course, category, and assignment its own number
    private long nextId() {
        lastId++;
        return lastId;
    }

    @Override
    public List<Course> listCourses() {
        return new ArrayList<>(courses);
    }

    @Override
    public Course createCourse(String name, double targetGrade) {
        Course course = new Course(name, targetGrade);
        course.setId(nextId());
        courses.add(course);
        return course;
    }

    // the objects are the storage here, so the update methods have nothing to do
    @Override
    public void updateCourse(Course course) {
    }

    @Override
    public void deleteCourse(Course course) {
        courses.remove(course);
    }

    @Override
    public Category addCategory(Course course, String name, double weight, int dropLowest) {
        Category category = new Category(name, weight, dropLowest);
        category.setId(nextId());
        course.getCategories().add(category);
        return category;
    }

    @Override
    public void updateCategory(Category category) {
    }

    @Override
    public void deleteCategory(Course course, Category category) {
        course.getCategories().remove(category);
    }

    @Override
    public Assignment addAssignment(Category category, String name, Double earned, double possible,
                                    boolean extraCredit) {
        Assignment assignment = new Assignment(name, earned, possible, extraCredit);
        assignment.setId(nextId());
        category.getAssignments().add(assignment);
        return assignment;
    }

    @Override
    public void updateAssignment(Assignment assignment) {
    }

    @Override
    public void deleteAssignment(Category category, Assignment assignment) {
        category.getAssignments().remove(assignment);
    }

    @Override
    public String describe() {
        return "in-memory (changes are NOT saved)";
    }
}
