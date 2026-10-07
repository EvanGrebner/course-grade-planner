// Purpose: The table of categories for one course (name, weight, drop lowest, current %).
// When you edit a cell, the change is saved to the store and the screen updates.

package gradeplanner.ui;

import gradeplanner.logic.GradeCalculator;
import gradeplanner.model.Category;
import gradeplanner.model.Course;
import gradeplanner.store.CourseStore;

import javax.swing.table.AbstractTableModel;

class CategoryTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Category", "Weight %", "Drop lowest", "Current %"};

    private final CourseStore store;
    private final Course course;
    private final Runnable onChange;   // runs after every edit so the grades refresh

    CategoryTableModel(CourseStore store, Course course, Runnable onChange) {
        this.store = store;
        this.course = course;
        this.onChange = onChange;
    }

    Category categoryAt(int row) {
        return course.getCategories().get(row);
    }

    @Override
    public int getRowCount() {
        return course.getCategories().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    // the column type decides how a cell is shown and edited
    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 1 -> Double.class;
            case 2 -> Integer.class;
            default -> String.class;
        };
    }

    // the last column is calculated, so it cannot be edited
    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 3;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Category category = categoryAt(row);
        return switch (column) {
            case 0 -> category.getName();
            case 1 -> category.getWeight();
            case 2 -> category.getDropLowest();
            default -> {
                double score = GradeCalculator.categoryFraction(category);
                if (Double.isNaN(score)) {
                    yield "-";
                }
                yield String.format("%.1f%%", score * 100);
            }
        };
    }

    // bad input is ignored so the old value stays
    @Override
    public void setValueAt(Object value, int row, int column) {
        Category category = categoryAt(row);
        switch (column) {
            case 0 -> {
                String name = String.valueOf(value).trim();
                if (name.isEmpty()) {
                    return;
                }
                category.setName(name);
            }
            case 1 -> {
                double weight = ((Number) value).doubleValue();
                if (weight < 0 || weight > 100) {
                    return;
                }
                category.setWeight(weight);
            }
            case 2 -> {
                int dropLowest = ((Number) value).intValue();
                if (dropLowest < 0) {
                    return;
                }
                category.setDropLowest(dropLowest);
            }
            default -> {
                return;
            }
        }
        store.updateCategory(category);
        fireTableRowsUpdated(row, row);
        onChange.run();
    }
}
