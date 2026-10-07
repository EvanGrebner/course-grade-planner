// Purpose: Tests for the grade math in GradeCalculator.
// Each test uses numbers that can be checked by hand.
// How to run it: mvn test

package gradeplanner.logic;

import gradeplanner.logic.NeededScore.Status;
import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.model.Course;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GradeCalculatorTest {

    // how close two decimal numbers have to be to count as equal
    private static final double TOLERANCE = 1e-6;

    // homework (25%) and exams (35%) are both at 85%, and the final (40%) has nothing entered
    // that means 60% of the course is graded and 51 of those 60 points are earned
    private static Course midSemesterCourse() {
        return new Course("Stat 380", 90)
                .add(new Category("Homework", 25).add(Assignment.graded("HW avg", 85, 100)))
                .add(new Category("Exams", 35).add(Assignment.graded("Midterms", 85, 100)))
                .add(new Category("Final", 40));
    }

    @Test
    void currentGradeIsWeightedAverageOfGradedCategories() {
        // 51 earned out of 60 graded is 85%
        assertEquals(85.0, GradeCalculator.currentGrade(midSemesterCourse()), TOLERANCE);
    }

    @Test
    void currentGradeWithUnevenWeights() {
        Course course = new Course("X", 90)
                .add(new Category("HW", 30).add(Assignment.graded("a", 80, 100)))
                .add(new Category("Exams", 40).add(Assignment.graded("b", 90, 100)))
                .add(new Category("Final", 30));
        // (30 * 0.8 + 40 * 0.9) / 70 = 60 / 70
        assertEquals(60.0 / 70.0 * 100, GradeCalculator.currentGrade(course), TOLERANCE);
    }

    @Test
    void currentGradeIsNaNWhenNothingGraded() {
        Course course = new Course("X", 90).add(new Category("HW", 100).add(Assignment.pending("a", 10)));
        assertTrue(Double.isNaN(GradeCalculator.currentGrade(course)));
    }

    @Test
    void neededScoreMatchesHandCalculation() {
        // (90 - 51) / 40 = 97.5
        NeededScore needed = GradeCalculator.neededScore(midSemesterCourse(), 90);
        assertEquals(Status.ACHIEVABLE, needed.status());
        assertEquals(97.5, needed.percent(), 1e-5);
    }

    @Test
    void neededScoreForLowerTarget() {
        // (80 - 51) / 40 = 72.5
        NeededScore needed = GradeCalculator.neededScore(midSemesterCourse(), 80);
        assertEquals(Status.ACHIEVABLE, needed.status());
        assertEquals(72.5, needed.percent(), 1e-5);
    }

    @Test
    void targetAlreadySecuredWhenZerosStillReachIt() {
        // 51 points are locked in, so a 50% target is safe even with a zero on the final
        NeededScore needed = GradeCalculator.neededScore(midSemesterCourse(), 50);
        assertEquals(Status.ALREADY_SECURED, needed.status());
    }

    @Test
    void targetAboveOneHundredPercentNeedsExtraCredit() {
        // (95 - 51) / 40 = 110
        NeededScore needed = GradeCalculator.neededScore(midSemesterCourse(), 95);
        assertEquals(Status.NEEDS_EXTRA_CREDIT, needed.status());
        assertEquals(110.0, needed.percent(), 1e-5);
    }

    @Test
    void absurdTargetIsUnreachable() {
        // (140 - 51) / 40 = 222.5, which is over the 200% limit
        NeededScore needed = GradeCalculator.neededScore(midSemesterCourse(), 140);
        assertEquals(Status.UNREACHABLE, needed.status());
    }

    @Test
    void noRemainingWorkAndTargetMissedIsUnreachable() {
        Course course = new Course("X", 95)
                .add(new Category("All", 100).add(Assignment.graded("a", 90, 100)));
        assertEquals(Status.UNREACHABLE, GradeCalculator.neededScore(course, 95).status());
        assertEquals(Status.ALREADY_SECURED, GradeCalculator.neededScore(course, 90).status());
    }

    @Test
    void dropLowestRemovesWorstScore() {
        Category homework = new Category("HW", 100, 1)
                .add(Assignment.graded("a", 50, 100))
                .add(Assignment.graded("b", 90, 100))
                .add(Assignment.graded("c", 100, 100));
        // the 50 is dropped, so the score is (90 + 100) / 200
        assertEquals(0.95, GradeCalculator.categoryFraction(homework), TOLERANCE);
    }

    @Test
    void dropLowestUsesPercentNotRawPoints() {
        Category quizzes = new Category("Quizzes", 100, 1)
                .add(Assignment.graded("small bad", 1, 10))   // 10%
                .add(Assignment.graded("big ok", 60, 100));   // 60%
        // the lowest percent is the 1 out of 10, so only the 60 out of 100 is left
        assertEquals(0.6, GradeCalculator.categoryFraction(quizzes), TOLERANCE);
    }

    @Test
    void dropLowestNeverDropsEverything() {
        Category homework = new Category("HW", 100, 5)
                .add(Assignment.graded("a", 50, 100))
                .add(Assignment.graded("b", 80, 100));
        assertEquals(0.8, GradeCalculator.categoryFraction(homework), TOLERANCE);
    }

    @Test
    void extraCreditAddsToPointsEarnedOnly() {
        Category quizzes = new Category("Quizzes", 100)
                .add(Assignment.graded("q1", 8, 10))
                .add(Assignment.graded("q2", 9, 10))
                .add(Assignment.extraCredit("bonus", 1));
        // (8 + 9 + 1) / 20
        assertEquals(0.9, GradeCalculator.categoryFraction(quizzes), TOLERANCE);
    }

    @Test
    void partiallyGradedCategoryCountsRemainingAssignmentsInsideIt() {
        // one category with a midterm graded 80 out of 100 and a final of 100 points still to come
        Course course = new Course("X", 90).add(new Category("Exams", 100)
                .add(Assignment.graded("Midterm", 80, 100))
                .add(Assignment.pending("Final", 100)));

        // for a 90: (80 + x) / 200 = 0.90, so x = 100 and you need 100% on the final
        NeededScore for90 = GradeCalculator.neededScore(course, 90);
        assertEquals(Status.ACHIEVABLE, for90.status());
        assertEquals(100.0, for90.percent(), 1e-5);

        // for an 85: (80 + x) / 200 = 0.85, so x = 90
        NeededScore for85 = GradeCalculator.neededScore(course, 85);
        assertEquals(90.0, for85.percent(), 1e-5);
    }

    @Test
    void dropLowestInteractsCorrectlyWithNeededScore() {
        // quizzes drop the lowest, q1 is 40 out of 100, and q2 (100 points) is still to come
        Course course = new Course("X", 80).add(new Category("Quizzes", 100, 1)
                .add(Assignment.graded("q1", 40, 100))
                .add(Assignment.pending("q2", 100)));
        // any q2 score above 40% makes q1 the dropped one, so the grade is just q2
        // that means q2 needs to be 80%
        NeededScore needed = GradeCalculator.neededScore(course, 80);
        assertEquals(Status.ACHIEVABLE, needed.status());
        assertEquals(80.0, needed.percent(), 1e-4);
    }

    @Test
    void weightsMustSumToOneHundred() {
        assertTrue(GradeCalculator.weightsValid(midSemesterCourse()));

        Course bad = new Course("X", 90).add(new Category("HW", 30)).add(new Category("Exams", 50));
        assertFalse(GradeCalculator.weightsValid(bad));
        assertEquals(80.0, GradeCalculator.totalWeight(bad), TOLERANCE);
    }

    @Test
    void letterGrades() {
        assertEquals("A", GradeCalculator.letter(95));
        assertEquals("A-", GradeCalculator.letter(90));
        assertEquals("B+", GradeCalculator.letter(88.5));
        assertEquals("C", GradeCalculator.letter(75));
        assertEquals("F", GradeCalculator.letter(42));
        assertEquals("-", GradeCalculator.letter(Double.NaN));
    }
}
