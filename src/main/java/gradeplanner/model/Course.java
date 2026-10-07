// Purpose: One course with its weighted categories and a target final grade.
// The target is a percent, like 90.

package gradeplanner.model;

import java.util.ArrayList;
import java.util.List;

public class Course {

    private long id;
    private String name;
    private double targetGrade;
    private final List<Category> categories = new ArrayList<>();

    public Course(String name, double targetGrade) {
        this.name = name;
        this.targetGrade = targetGrade;
    }

    // returns the course so calls can be chained
    public Course add(Category category) {
        categories.add(category);
        return this;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetGrade() { return targetGrade; }
    public void setTargetGrade(double targetGrade) { this.targetGrade = targetGrade; }

    public List<Category> getCategories() { return categories; }

    // the course list shows the name
    @Override
    public String toString() {
        return name;
    }
}
