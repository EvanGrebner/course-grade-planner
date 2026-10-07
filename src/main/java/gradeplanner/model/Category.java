// Purpose: A group of assignments that share a weight, like Homework or Exams.
// The weight is a percent of the course grade, so 25 means 25%.
// It can also drop the lowest few scores.

package gradeplanner.model;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private long id;
    private String name;
    private double weight;
    private int dropLowest;
    private final List<Assignment> assignments = new ArrayList<>();

    public Category(String name, double weight) {
        this(name, weight, 0);
    }

    public Category(String name, double weight, int dropLowest) {
        this.name = name;
        this.weight = weight;
        this.dropLowest = dropLowest;
    }

    // returns the category so calls can be chained
    public Category add(Assignment assignment) {
        assignments.add(assignment);
        return this;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public int getDropLowest() { return dropLowest; }
    public void setDropLowest(int dropLowest) { this.dropLowest = dropLowest; }

    public List<Assignment> getAssignments() { return assignments; }
}
