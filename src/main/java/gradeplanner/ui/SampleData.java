// Purpose: Adds a made-up course so there is something to look at on the first launch.

package gradeplanner.ui;

import gradeplanner.model.Category;
import gradeplanner.model.Course;
import gradeplanner.store.CourseStore;

public final class SampleData {

    private SampleData() {}

    public static Course addSampleCourse(CourseStore store) {
        Course course = store.createCourse("Sample: Intro Statistics", 90);

        // homework drops the lowest score, and two homeworks are not graded yet
        Category homework = store.addCategory(course, "Homework", 25, 1);
        store.addAssignment(homework, "HW 1", 9.0, 10, false);
        store.addAssignment(homework, "HW 2", 6.0, 10, false);
        store.addAssignment(homework, "HW 3", 10.0, 10, false);
        store.addAssignment(homework, "HW 4", 8.0, 10, false);
        store.addAssignment(homework, "HW 5", null, 10, false);
        store.addAssignment(homework, "HW 6", null, 10, false);

        Category exams = store.addCategory(course, "Exams", 35, 0);
        store.addAssignment(exams, "Midterm 1", 82.0, 100, false);
        store.addAssignment(exams, "Midterm 2", null, 100, false);

        Category finalExam = store.addCategory(course, "Final exam", 40, 0);
        store.addAssignment(finalExam, "Final", null, 100, false);

        return course;
    }
}
