// Purpose: The list of things the app needs from storage.
// One version saves to PostgreSQL and another keeps everything in memory.
// Each one also keeps the course, category, and assignment objects up to date.

package gradeplanner.store;

import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.model.Course;

import java.util.List;

public interface CourseStore {

    /** All courses with their categories and assignments */
    List<Course> listCourses();

    Course createCourse(String name, double targetGrade);

    /** Saves the course name and target grade */
    void updateCourse(Course course);

    void deleteCourse(Course course);

    Category addCategory(Course course, String name, double weight, int dropLowest);

    /** Saves the category name, weight, and drop lowest count */
    void updateCategory(Category category);

    void deleteCategory(Course course, Category category);

    Assignment addAssignment(Category category, String name, Double earned, double possible, boolean extraCredit);

    /** Saves the assignment name, scores, and extra credit flag */
    void updateAssignment(Assignment assignment);

    void deleteAssignment(Category category, Assignment assignment);

    /** Short text for the status bar, like where the data is saved */
    String describe();
}
