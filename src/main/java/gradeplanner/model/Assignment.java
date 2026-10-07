// Purpose: One piece of work in a category, like a homework or an exam.
// An empty earned score (null) means it has not been graded yet.
// Extra credit adds to the points earned but not to the points possible.

package gradeplanner.model;

public class Assignment {

    private long id;
    private String name;
    private Double earned;   // null means not graded yet
    private double possible;
    private boolean extraCredit;

    public Assignment(String name, Double earned, double possible, boolean extraCredit) {
        this.name = name;
        this.earned = earned;
        this.possible = possible;
        this.extraCredit = extraCredit;
    }

    /** A regular assignment that has a score */
    public static Assignment graded(String name, double earned, double possible) {
        return new Assignment(name, earned, possible, false);
    }

    /** A regular assignment that has no score yet */
    public static Assignment pending(String name, double possible) {
        return new Assignment(name, null, possible, false);
    }

    /** Extra credit with a score, which only adds to points earned */
    public static Assignment extraCredit(String name, double earned) {
        return new Assignment(name, earned, 0, true);
    }

    public boolean isGraded() {
        return earned != null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getEarned() { return earned; }
    public void setEarned(Double earned) { this.earned = earned; }

    public double getPossible() { return possible; }
    public void setPossible(double possible) { this.possible = possible; }

    public boolean isExtraCredit() { return extraCredit; }
    public void setExtraCredit(boolean extraCredit) { this.extraCredit = extraCredit; }
}
