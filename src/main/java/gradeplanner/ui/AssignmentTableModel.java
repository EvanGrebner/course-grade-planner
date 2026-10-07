// Purpose: The table of assignments in the selected category.
// The Earned column is text so it can be left empty, which means not graded yet.
// When you edit a cell, the change is saved to the store and the screen updates.

package gradeplanner.ui;

import gradeplanner.model.Assignment;
import gradeplanner.model.Category;
import gradeplanner.store.CourseStore;

import javax.swing.table.AbstractTableModel;

class AssignmentTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Assignment", "Earned", "Possible", "Extra credit", "Score"};

    private final CourseStore store;
    private final Runnable onChange;   // runs after every edit so the grades refresh
    private Category category;

    AssignmentTableModel(CourseStore store, Runnable onChange) {
        this.store = store;
        this.onChange = onChange;
    }

    void setCategory(Category category) {
        this.category = category;
        fireTableDataChanged();
    }

    Category getCategory() {
        return category;
    }

    Assignment assignmentAt(int row) {
        return category.getAssignments().get(row);
    }

    @Override
    public int getRowCount() {
        if (category == null) {
            return 0;
        }
        return category.getAssignments().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 2 -> Double.class;
            case 3 -> Boolean.class;
            default -> String.class;
        };
    }

    // the last column is calculated, so it cannot be edited
    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 4;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Assignment assignment = assignmentAt(row);
        return switch (column) {
            case 0 -> assignment.getName();
            case 1 -> {
                if (assignment.getEarned() == null) {
                    yield "";
                }
                yield showNumber(assignment.getEarned());
            }
            case 2 -> assignment.getPossible();
            case 3 -> assignment.isExtraCredit();
            default -> scoreText(assignment);
        };
    }

    // the text for the Score column
    private static String scoreText(Assignment assignment) {
        if (!assignment.isGraded()) {
            return "not graded";
        }
        if (assignment.isExtraCredit()) {
            return "+" + showNumber(assignment.getEarned()) + " pts";
        }
        if (assignment.getPossible() <= 0) {
            return "-";
        }
        return String.format("%.1f%%", assignment.getEarned() / assignment.getPossible() * 100);
    }

    // bad input is ignored so the old value stays
    @Override
    public void setValueAt(Object value, int row, int column) {
        Assignment assignment = assignmentAt(row);
        switch (column) {
            case 0 -> {
                String name = String.valueOf(value).trim();
                if (name.isEmpty()) {
                    return;
                }
                assignment.setName(name);
            }
            case 1 -> {
                String text = String.valueOf(value).trim();
                if (text.isEmpty()) {
                    assignment.setEarned(null);
                } else {
                    try {
                        double earned = Double.parseDouble(text);
                        if (earned < 0) {
                            return;
                        }
                        assignment.setEarned(earned);
                    } catch (NumberFormatException e) {
                        return;   // text that is not a number is ignored
                    }
                }
            }
            case 2 -> {
                double possible = ((Number) value).doubleValue();
                if (possible < 0) {
                    return;
                }
                assignment.setPossible(possible);
            }
            case 3 -> assignment.setExtraCredit((Boolean) value);
            default -> {
                return;
            }
        }
        store.updateAssignment(assignment);
        fireTableRowsUpdated(row, row);
        onChange.run();
    }

    // shows 10 instead of 10.0 for whole numbers
    private static String showNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
