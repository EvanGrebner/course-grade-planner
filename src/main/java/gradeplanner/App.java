// Purpose: Starts the Course Grade Planner.
// It tries to connect to the PostgreSQL database first.
// If the database is not running, it opens the app in memory only.
// How to run it: mvn compile exec:java

package gradeplanner;

import gradeplanner.store.CourseStore;
import gradeplanner.store.InMemoryCourseStore;
import gradeplanner.store.JdbcCourseStore;
import gradeplanner.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {

    private App() {}

    public static void main(String[] args) {
        // use the look of the operating system, and keep the default look if that fails
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Using the default look and feel");
        }

        // use the database if we can reach it, otherwise keep everything in memory
        CourseStore store;
        String warning = null;
        try {
            store = JdbcCourseStore.connect();
        } catch (Exception e) {
            store = new InMemoryCourseStore();
            warning = "Couldn't connect to the PostgreSQL database, so this session is running in memory "
                    + "and your changes will be lost when you close the app.\n\n"
                    + "To save your data, start the database with:  docker compose up -d\n\n"
                    + "Reason: " + e.getMessage();
        }

        // the code inside invokeLater needs variables that are not changed after this point,
        // so these are copies
        CourseStore finalStore = store;
        String finalWarning = warning;
        SwingUtilities.invokeLater(() -> new MainFrame(finalStore, finalWarning).setVisible(true));
    }
}
