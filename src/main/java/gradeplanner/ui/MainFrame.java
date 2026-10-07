// Purpose: The main window of the app.
// The course list is on the left and the selected course is on the right.
// The bottom bar shows where the data is saved.

package gradeplanner.ui;

import gradeplanner.model.Course;
import gradeplanner.store.CourseStore;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public class MainFrame extends JFrame {

    private final CourseStore store;
    private final DefaultListModel<Course> listModel = new DefaultListModel<>();
    private final JList<Course> courseList = new JList<>(listModel);
    private final JPanel detail = new JPanel(new BorderLayout());

    // startupWarning is shown in a popup, or it is null when there is nothing to warn about
    public MainFrame(CourseStore store, String startupWarning) {
        super("Course Grade Planner");
        this.store = store;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 640));

        // picking a course shows it on the right
        courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseList.setFixedCellHeight(26);
        courseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedCourse();
            }
        });

        JButton newCourse = new JButton("New");
        newCourse.addActionListener(e -> newCourse());
        JButton renameCourse = new JButton("Rename");
        renameCourse.addActionListener(e -> renameCourse());
        JButton deleteCourse = new JButton("Delete");
        deleteCourse.addActionListener(e -> deleteCourse());
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        listButtons.add(newCourse);
        listButtons.add(renameCourse);
        listButtons.add(deleteCourse);

        JLabel coursesTitle = new JLabel("Courses");
        coursesTitle.setFont(coursesTitle.getFont().deriveFont(Font.BOLD, 14f));
        coursesTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JPanel left = new JPanel(new BorderLayout());
        left.add(coursesTitle, BorderLayout.NORTH);
        left.add(new JScrollPane(courseList), BorderLayout.CENTER);
        left.add(listButtons, BorderLayout.SOUTH);
        left.setMinimumSize(new Dimension(210, 100));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detail);
        split.setDividerLocation(230);
        split.setBorder(null);

        JLabel status = new JLabel("  Storage: " + store.describe());
        status.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        reloadCourses();
        pack();
        setLocationRelativeTo(null);

        // shown after the window opens so the popup sits on top of it
        if (startupWarning != null) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, startupWarning, "Running without a database",
                            JOptionPane.WARNING_MESSAGE));
        }
    }

    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New course...");
        newItem.addActionListener(e -> newCourse());

        JMenuItem sample = new JMenuItem("Add sample course");
        sample.addActionListener(e -> {
            Course course = SampleData.addSampleCourse(store);
            listModel.addElement(course);
            courseList.setSelectedValue(course, true);
        });

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());

        file.add(newItem);
        file.add(sample);
        file.addSeparator();
        file.add(exit);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        return bar;
    }

    // loads every course from the store into the list
    private void reloadCourses() {
        listModel.clear();
        for (Course course : store.listCourses()) {
            listModel.addElement(course);
        }
        if (!listModel.isEmpty()) {
            courseList.setSelectedIndex(0);
        } else {
            showSelectedCourse();
        }
    }

    // swaps the right side to the selected course, or to a hint if there are none
    private void showSelectedCourse() {
        detail.removeAll();
        Course course = courseList.getSelectedValue();
        if (course == null) {
            JLabel empty = new JLabel("Create a course with \"New\" (or File > Add sample course).",
                    JLabel.CENTER);
            detail.add(empty, BorderLayout.CENTER);
        } else {
            detail.add(new CoursePanel(store, course), BorderLayout.CENTER);
        }
        detail.revalidate();
        detail.repaint();
    }

    private void newCourse() {
        String name = JOptionPane.showInputDialog(this, "Course name:", "New course", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        Course course = store.createCourse(name.trim(), 90);
        listModel.addElement(course);
        courseList.setSelectedValue(course, true);
    }

    private void renameCourse() {
        Course course = courseList.getSelectedValue();
        if (course == null) {
            return;
        }
        String name = (String) JOptionPane.showInputDialog(this, "Course name:", "Rename course",
                JOptionPane.PLAIN_MESSAGE, null, null, course.getName());
        if (name == null || name.isBlank()) {
            return;
        }
        course.setName(name.trim());
        store.updateCourse(course);
        courseList.repaint();
        showSelectedCourse();
    }

    private void deleteCourse() {
        Course course = courseList.getSelectedValue();
        if (course == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + course.getName() + "\" and everything in it?", "Delete course",
                JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        store.deleteCourse(course);
        listModel.removeElement(course);
        if (!listModel.isEmpty()) {
            courseList.setSelectedIndex(0);
        } else {
            showSelectedCourse();
        }
    }
}
