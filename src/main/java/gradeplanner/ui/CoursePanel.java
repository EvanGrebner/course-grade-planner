// Purpose: The screen for one course.
// It shows the target grade, the category table, the assignment table, and the
// "What do I need?" box. Every edit refreshes the numbers on the screen.

package gradeplanner.ui;

import gradeplanner.logic.GradeCalculator;
import gradeplanner.logic.NeededScore;
import gradeplanner.model.Category;
import gradeplanner.model.Course;
import gradeplanner.store.CourseStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

public class CoursePanel extends JPanel {

    // the grades shown in the quick table at the bottom (A, A-, B+, B, B-, C)
    private static final double[] COMMON_TARGETS = {93, 90, 87, 83, 80, 70};

    private final CourseStore store;
    private final Course course;

    private final CategoryTableModel categoryModel;
    private final AssignmentTableModel assignmentModel;
    private final JTable categoryTable;
    private final JTable assignmentTable;

    private final JLabel currentLabel = new JLabel();
    private final JLabel weightLabel = new JLabel();
    private final JLabel neededLabel = new JLabel();
    private final JLabel thresholdsLabel = new JLabel();
    private final JLabel assignmentsTitle = new JLabel(" ");

    public CoursePanel(CourseStore store, Course course) {
        super(new BorderLayout(8, 8));
        this.store = store;
        this.course = course;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // both tables call recalculate() after an edit so the grades stay current
        categoryModel = new CategoryTableModel(store, course, this::recalculate);
        assignmentModel = new AssignmentTableModel(store, this::recalculate);
        categoryTable = new JTable(categoryModel);
        assignmentTable = new JTable(assignmentModel);

        // settings that both tables share
        for (JTable table : new JTable[]{categoryTable, assignmentTable}) {
            table.setRowHeight(24);
            table.setFillsViewportHeight(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);   // saves a cell when you click away
        }
        setColumnWidths(categoryTable, 320, 90, 100, 100);
        setColumnWidths(assignmentTable, 320, 90, 90, 100, 120);

        // picking a category shows its assignments
        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedCategory();
            }
        });

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTables(), BorderLayout.CENTER);
        add(buildNeededPanel(), BorderLayout.SOUTH);

        if (!course.getCategories().isEmpty()) {
            categoryTable.setRowSelectionInterval(0, 0);
        }
        recalculate();
    }

    private static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // layout

    // course name, target grade box, current grade, and the weight check
    private JPanel buildHeader() {
        JLabel title = new JLabel(course.getName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        // saving the new target each time the number changes
        SpinnerNumberModel targetModel = new SpinnerNumberModel(course.getTargetGrade(), 0.0, 200.0, 0.5);
        JSpinner targetSpinner = new JSpinner(targetModel);
        targetSpinner.addChangeListener(e -> {
            course.setTargetGrade(((Number) targetSpinner.getValue()).doubleValue());
            store.updateCourse(course);
            recalculate();
        });

        JPanel targetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        targetRow.add(new JLabel("Target final grade (%):"));
        targetRow.add(targetSpinner);
        targetRow.add(Box.createHorizontalStrut(16));
        targetRow.add(currentLabel);
        targetRow.add(Box.createHorizontalStrut(16));
        targetRow.add(weightLabel);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        targetRow.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(targetRow);
        return header;
    }

    // category table on top and assignment table below it
    private JSplitPane buildTables() {
        JButton addCategory = new JButton("Add category");
        addCategory.addActionListener(e -> addCategory());
        JButton deleteCategory = new JButton("Delete category");
        deleteCategory.addActionListener(e -> deleteCategory());
        JPanel categoryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        categoryButtons.add(addCategory);
        categoryButtons.add(deleteCategory);

        JPanel categoryPanel = new JPanel(new BorderLayout());
        JLabel categoriesTitle = new JLabel("Categories and weights");
        categoriesTitle.setFont(categoriesTitle.getFont().deriveFont(Font.BOLD));
        categoryPanel.add(categoriesTitle, BorderLayout.NORTH);
        categoryPanel.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        categoryPanel.add(categoryButtons, BorderLayout.SOUTH);

        JButton addAssignment = new JButton("Add assignment");
        addAssignment.addActionListener(e -> addAssignment());
        JButton deleteAssignment = new JButton("Delete assignment");
        deleteAssignment.addActionListener(e -> deleteAssignment());
        JPanel assignmentButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        assignmentButtons.add(addAssignment);
        assignmentButtons.add(deleteAssignment);
        assignmentButtons.add(new JLabel("Leave Earned blank until it's graded."));

        JPanel assignmentPanel = new JPanel(new BorderLayout());
        assignmentsTitle.setFont(assignmentsTitle.getFont().deriveFont(Font.BOLD));
        assignmentPanel.add(assignmentsTitle, BorderLayout.NORTH);
        assignmentPanel.add(new JScrollPane(assignmentTable), BorderLayout.CENTER);
        assignmentPanel.add(assignmentButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, categoryPanel, assignmentPanel);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        return split;
    }

    // the box at the bottom with the needed score and the quick table
    private JPanel buildNeededPanel() {
        JLabel heading = new JLabel("What do I need?");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        neededLabel.setAlignmentX(LEFT_ALIGNMENT);
        thresholdsLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(4));
        panel.add(neededLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(thresholdsLabel);
        return panel;
    }

    // button actions

    // finishes any cell that is still being typed in so the value is not lost
    private void stopEditing() {
        for (JTable table : new JTable[]{categoryTable, assignmentTable}) {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
    }

    private void addCategory() {
        stopEditing();
        String name = JOptionPane.showInputDialog(this, "Category name (e.g. Homework, Exams):", "Add category",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }

        // the new category starts with whatever weight is left over
        double remaining = Math.max(0, 100 - GradeCalculator.totalWeight(course));
        store.addCategory(course, name.trim(), remaining, 0);
        categoryModel.fireTableDataChanged();
        int last = course.getCategories().size() - 1;
        categoryTable.setRowSelectionInterval(last, last);
        recalculate();
    }

    private void deleteCategory() {
        stopEditing();
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Category category = categoryModel.categoryAt(row);
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + category.getName() + "\" and its assignments?", "Delete category",
                JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        store.deleteCategory(course, category);
        categoryModel.fireTableDataChanged();
        if (!course.getCategories().isEmpty()) {
            categoryTable.setRowSelectionInterval(0, 0);
        } else {
            showSelectedCategory();
        }
        recalculate();
    }

    private void addAssignment() {
        stopEditing();
        Category category = assignmentModel.getCategory();
        if (category == null) {
            JOptionPane.showMessageDialog(this, "Select or add a category first.");
            return;
        }
        String name = "Assignment " + (category.getAssignments().size() + 1);
        store.addAssignment(category, name, null, 100, false);
        assignmentModel.fireTableDataChanged();
        int last = category.getAssignments().size() - 1;
        assignmentTable.setRowSelectionInterval(last, last);
        recalculate();
    }

    private void deleteAssignment() {
        stopEditing();
        Category category = assignmentModel.getCategory();
        int row = assignmentTable.getSelectedRow();
        if (category == null || row < 0) {
            return;
        }
        store.deleteAssignment(category, assignmentModel.assignmentAt(row));
        assignmentModel.fireTableDataChanged();
        recalculate();
    }

    // fills the assignment table with the category that is selected
    private void showSelectedCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0 || row >= course.getCategories().size()) {
            assignmentModel.setCategory(null);
            assignmentsTitle.setText("Select a category to see its assignments");
            return;
        }
        Category category = categoryModel.categoryAt(row);
        assignmentModel.setCategory(category);

        String dropText = "";
        if (category.getDropLowest() > 0) {
            dropText = " (dropping lowest " + category.getDropLowest() + ")";
        }
        assignmentsTitle.setText("Assignments in " + category.getName() + dropText);
    }

    // calculations

    // refreshes every number on the screen from the current data
    void recalculate() {
        double current = GradeCalculator.currentGrade(course);
        if (Double.isNaN(current)) {
            currentLabel.setText("Current grade: nothing graded yet");
        } else {
            currentLabel.setText(String.format("Current grade: %.2f%% (%s)",
                    current, GradeCalculator.letter(current)));
        }

        // green when the weights add up to 100, red when they do not
        double total = GradeCalculator.totalWeight(course);
        if (GradeCalculator.weightsValid(course)) {
            weightLabel.setForeground(new Color(0x2E7D32));
            weightLabel.setText("Weights total 100%");
        } else {
            weightLabel.setForeground(new Color(0xC62828));
            weightLabel.setText(String.format("Weights total %.1f%% - should be 100%%", total));
        }

        double target = course.getTargetGrade();
        if (course.getCategories().isEmpty()) {
            neededLabel.setText("<html>Add categories and weights to get started.</html>");
            thresholdsLabel.setText(" ");
        } else {
            NeededScore needed = GradeCalculator.neededScore(course, target);
            neededLabel.setText("<html>" + needed.describe(target) + "</html>");
            thresholdsLabel.setText(thresholdsHtml());
        }

        // redraw the table rows so their calculated columns update
        if (categoryModel.getRowCount() > 0) {
            categoryModel.fireTableRowsUpdated(0, categoryModel.getRowCount() - 1);
        }
        if (assignmentModel.getRowCount() > 0) {
            assignmentModel.fireTableRowsUpdated(0, assignmentModel.getRowCount() - 1);
        }
    }

    // a small table of what you need for each common letter grade
    private String thresholdsHtml() {
        StringBuilder gradeRow = new StringBuilder("<html><table cellpadding='2'><tr><td><b>To finish with</b></td>");
        StringBuilder needRow = new StringBuilder("<tr><td><b>You need</b></td>");
        for (double target : COMMON_TARGETS) {
            gradeRow.append(String.format("<td align='center'><b>%s (%.0f%%)</b></td>",
                    GradeCalculator.letter(target), target));
            needRow.append("<td align='center'>")
                    .append(shortAnswer(GradeCalculator.neededScore(course, target)))
                    .append("</td>");
        }
        gradeRow.append("</tr>").append(needRow).append("</tr></table></html>");
        return gradeRow.toString();
    }

    private static String shortAnswer(NeededScore needed) {
        return switch (needed.status()) {
            case ALREADY_SECURED -> "secured";
            case ACHIEVABLE -> String.format("%.1f%%", needed.percent());
            case NEEDS_EXTRA_CREDIT -> String.format("%.1f%% (extra credit)", needed.percent());
            case UNREACHABLE -> "out of reach";
        };
    }
}
