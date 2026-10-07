// Purpose: All of the grade math for the app. It has no screen or database code in it.
// It works out category scores, the current grade, and the score you need
// on the remaining work to reach a target grade.

package gradeplanner.logic;

import gradeplanner.logic.NeededScore.Status;
import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.model.Course;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GradeCalculator {

    // highest needed score we report before calling a target out of reach
    public static final double MAX_SCORE_PERCENT = 200.0;

    // small allowance so tiny decimal rounding does not give a wrong answer
    private static final double TOLERANCE = 1e-9;

    private GradeCalculator() {}

    // one assignment boiled down to the numbers the math needs
    private record Item(double earned, double possible, boolean extraCredit) {}

    /** Score of one category as a fraction like 0.85, or NaN if nothing counts yet */
    private static double scoreOf(List<Item> items, int dropLowest) {
        // extra credit adds points but is never dropped
        List<Item> regular = new ArrayList<>();
        double bonus = 0;
        for (Item item : items) {
            if (item.extraCredit) {
                bonus += item.earned;
            } else if (item.possible > 0) {
                regular.add(item);
            }
        }
        if (regular.isEmpty()) {
            return Double.NaN;
        }

        // lowest percent first, so the ones to drop are at the front
        regular.sort(Comparator.comparingDouble(item -> item.earned / item.possible));

        // never drop everything, at least one assignment has to stay
        int drop = Math.min(Math.max(dropLowest, 0), regular.size() - 1);

        double earned = bonus;
        double possible = 0;
        for (Item item : regular.subList(drop, regular.size())) {
            earned += item.earned;
            possible += item.possible;
        }
        return earned / possible;
    }

    /** Score of one category using only graded work, or NaN if none is graded */
    public static double categoryFraction(Category category) {
        List<Item> items = new ArrayList<>();
        for (Assignment assignment : category.getAssignments()) {
            if (assignment.isGraded()) {
                items.add(new Item(assignment.getEarned(), assignment.getPossible(),
                        assignment.isExtraCredit()));
            }
        }
        return scoreOf(items, category.getDropLowest());
    }

    /**
     * Finds the grade so far, using only categories that have graded work
     *
     * @param course the course to grade
     * @return the grade in percent, or NaN if nothing is graded yet
     */
    public static double currentGrade(Course course) {
        // the weights of graded categories are scaled so they add up to 100 again
        double weightSum = 0;
        double total = 0;
        for (Category category : course.getCategories()) {
            double score = categoryFraction(category);
            if (!Double.isNaN(score)) {
                weightSum += category.getWeight();
                total += category.getWeight() * score;
            }
        }
        if (weightSum == 0) {
            return Double.NaN;
        }
        return 100.0 * total / weightSum;
    }

    // graded work stays as is, and every ungraded assignment gets the guessed score
    private static List<Item> itemsWithGuess(Category category, double guessPercent) {
        List<Item> items = new ArrayList<>();
        for (Assignment assignment : category.getAssignments()) {
            if (assignment.isGraded()) {
                items.add(new Item(assignment.getEarned(), assignment.getPossible(),
                        assignment.isExtraCredit()));
            } else if (!assignment.isExtraCredit() && assignment.getPossible() > 0) {
                double guessedEarned = guessPercent / 100.0 * assignment.getPossible();
                items.add(new Item(guessedEarned, assignment.getPossible(), false));
            }
        }
        return items;
    }

    /** Final grade in percent if every remaining assignment gets the same score */
    public static double projectedGrade(Course course, double remainingPercent) {
        double weightSum = 0;
        double total = 0;
        for (Category category : course.getCategories()) {
            double score;
            if (category.getAssignments().isEmpty()) {
                // a category with no assignments yet counts as all remaining work
                score = remainingPercent / 100.0;
            } else {
                score = scoreOf(itemsWithGuess(category, remainingPercent), category.getDropLowest());
            }
            if (!Double.isNaN(score)) {
                weightSum += category.getWeight();
                total += category.getWeight() * score;
            }
        }
        if (weightSum == 0) {
            return Double.NaN;
        }
        return 100.0 * total / weightSum;
    }

    /** True if there is still ungraded work or a category with no assignments */
    public static boolean hasRemainingWork(Course course) {
        for (Category category : course.getCategories()) {
            if (category.getAssignments().isEmpty() && category.getWeight() > 0) {
                return true;
            }
            for (Assignment assignment : category.getAssignments()) {
                boolean ungraded = !assignment.isGraded() && !assignment.isExtraCredit();
                if (ungraded && assignment.getPossible() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the average score needed on all remaining work to reach a target grade
     *
     * @param course the course with its graded and ungraded work
     * @param target the final grade you want, in percent
     * @return the status and the needed percent (see NeededScore)
     */
    public static NeededScore neededScore(Course course, double target) {
        // 1. Check if the target is already safe with zeros on everything left
        double lowestPossible = projectedGrade(course, 0);
        if (Double.isNaN(lowestPossible)) {
            return new NeededScore(Status.UNREACHABLE, Double.NaN);
        }
        if (lowestPossible >= target - TOLERANCE) {
            return new NeededScore(Status.ALREADY_SECURED, 0);
        }

        // 2. Check that there is work left and that the target can be reached at all
        if (!hasRemainingWork(course)) {
            return new NeededScore(Status.UNREACHABLE, Double.NaN);
        }
        if (projectedGrade(course, MAX_SCORE_PERCENT) < target - TOLERANCE) {
            return new NeededScore(Status.UNREACHABLE, Double.NaN);
        }

        // 3. Search for the smallest score that reaches the target
        // the grade only goes up as the score goes up, so we keep cutting the range in half
        double low = 0;
        double high = MAX_SCORE_PERCENT;
        for (int i = 0; i < 80; i++) {
            double middle = (low + high) / 2;
            if (projectedGrade(course, middle) >= target) {
                high = middle;
            } else {
                low = middle;
            }
        }

        // 4. Report if the score is over 100%, which means extra credit is needed
        Status status = Status.ACHIEVABLE;
        if (high > 100.0 + 1e-7) {
            status = Status.NEEDS_EXTRA_CREDIT;
        }
        return new NeededScore(status, high);
    }

    /** True if the category weights add up to 100 */
    public static boolean weightsValid(Course course) {
        return Math.abs(totalWeight(course) - 100.0) < 1e-6;
    }

    public static double totalWeight(Course course) {
        double sum = 0;
        for (Category category : course.getCategories()) {
            sum += category.getWeight();
        }
        return sum;
    }

    /** Letter grade for a percent, like A or B+ */
    public static String letter(double percent) {
        if (Double.isNaN(percent)) return "-";
        if (percent >= 93) return "A";
        if (percent >= 90) return "A-";
        if (percent >= 87) return "B+";
        if (percent >= 83) return "B";
        if (percent >= 80) return "B-";
        if (percent >= 77) return "C+";
        if (percent >= 70) return "C";
        if (percent >= 60) return "D";
        return "F";
    }
}
