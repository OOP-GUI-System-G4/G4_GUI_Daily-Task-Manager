import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class DTM extends JFrame {

    // Colors used in the design
    private static final Color PURPLE = new Color(124, 108, 255);
    private static final Color DARK_BG = new Color(30, 31, 43);
    private static final Color PANEL_BG = new Color(37, 38, 54);
    private static final Color FIELD_BG = new Color(46, 48, 68);
    private static final Color GREEN = new Color(61, 240, 138);
    private static final Color RED = new Color(255, 92, 92);
    private static final Color LIGHT_TEXT = new Color(230, 230, 240);
    private static final Color GRAY_TEXT = new Color(150, 150, 170);

    // Simple class to hold one task
    static class Task {
        String name;
        String description;
        boolean completed;

        Task(String name, String description, boolean completed) {
            this.name = name;
            this.description = description;
            this.completed = completed;
        }
    }

    private String username;  // the user who is logged in
    private String fileName;  // each user has their own task file
    private List<Task> tasks = new ArrayList<>();

    private JTextField nameField;
    private JTextField descField;
    private JTable table;
    private DefaultTableModel model;
    private JLabel statsLabel;

    // Constructor: its name must match the class name (DTM)
    public DTM(String username) {
        this.username = username;
        this.fileName = "tasks_" + username + ".txt";

        setTitle("Daily Task Manager");
        setSize(780, 540);
        setMinimumSize(new Dimension(650, 420));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout());

        add(createHeader("PIT Project - Logged in as: " + username), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);

        loadTasks();
        refreshTable();
    }

    // Purple header at the top (used by both screens)
    private static JPanel createHeader(String subtitleText) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(PURPLE);
        header.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        JLabel title = new JLabel("\uD83D\uDCDD  TO-DO-LIST MANAGER");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(225, 225, 255));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    // Middle part: input area, buttons, and table
    private JPanel createCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(DARK_BG);
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(DARK_BG);
        top.add(createInputPanel());
        top.add(Box.createVerticalStrut(8));
        top.add(createButtonPanel());
        top.add(Box.createVerticalStrut(8));

        center.add(top, BorderLayout.NORTH);
        center.add(createTable(), BorderLayout.CENTER);
        return center;
    }

    // Task name, description, and Add Task button
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);

        JLabel nameLabel = createLabel("Task Name:");
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        panel.add(nameLabel, c);

        nameField = createField();
        c.gridx = 1; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(nameField, c);

        JLabel descLabel = createLabel("Description:");
        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        panel.add(descLabel, c);

        descField = createField();
        c.gridx = 1; c.gridy = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(descField, c);

        JButton addButton = createButton("+  Add Task", PURPLE, Color.WHITE);
        c.gridx = 2; c.gridy = 0; c.gridheight = 2; c.weightx = 0;
        c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.CENTER;
        panel.add(addButton, c);

        // Add task when the button is clicked
        addButton.addActionListener(e -> addTask());
        // Add task when Enter is pressed in the text fields
        nameField.addActionListener(e -> addTask());
        descField.addActionListener(e -> addTask());

        return panel;
    }

    // Mark Completed, Delete Task, Logout, and the totals
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_BG);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(DARK_BG);

        JButton doneButton = createButton("\u2714  Mark Completed", GREEN, new Color(20, 40, 30));
        JButton deleteButton = createButton("Delete Task", RED, Color.WHITE);
        JButton logoutButton = createButton("Logout", new Color(70, 72, 98), Color.WHITE);
        left.add(doneButton);
        left.add(Box.createHorizontalStrut(10));
        left.add(deleteButton);
        left.add(Box.createHorizontalStrut(10));
        left.add(logoutButton);

        statsLabel = new JLabel("Total: 0  |  Completed: 0  |  Pending: 0");
        statsLabel.setForeground(GRAY_TEXT);
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        panel.add(left, BorderLayout.WEST);
        panel.add(statsLabel, BorderLayout.EAST);

        doneButton.addActionListener(e -> markCompleted());
        deleteButton.addActionListener(e -> deleteTask());
        logoutButton.addActionListener(e -> logout());

        return panel;
    }

    // Table inside a scroll pane, so the user can scroll when there are many tasks
    private JScrollPane createTable() {
        String[] columns = {"#", "Task Name", "Status", "Description"};
        model = new DefaultTableModel(columns, 0) {
            // The user cannot edit the cells directly
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setBackground(DARK_BG);
        table.setForeground(LIGHT_TEXT);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setGridColor(new Color(50, 52, 72));
        table.setSelectionBackground(new Color(90, 80, 190));
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Column sizes
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(350);
        table.getColumnModel().getColumn(0).setMaxWidth(60);

        // Purple header with white bold text
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(PURPLE);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setOpaque(true);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Cell renderer: center some columns and color the status
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (col == 0 || col == 2) {
                    setHorizontalAlignment(JLabel.CENTER);
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                if (!selected) {
                    setBackground(DARK_BG);
                    setForeground(LIGHT_TEXT);
                    if (col == 2) {
                        if ("Completed".equals(value)) {
                            setForeground(GREEN);
                        } else {
                            setForeground(new Color(255, 190, 90));
                        }
                    }
                }
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(DARK_BG);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 72)));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // Make a light text label
    private static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(LIGHT_TEXT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    // Give a text field (or password field) the dark design
    private static void styleField(JTextField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(300, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(110, 112, 140)),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
    }

    // Make a dark text field
    private static JTextField createField() {
        JTextField field = new JTextField();
        styleField(field);
        return field;
    }

    // Make a flat colored button
    private static JButton createButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // Add a new task
    private void addTask() {
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task name.",
                    "Missing Task Name", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        tasks.add(new Task(name, desc, false));
        saveTasks();
        refreshTable();

        nameField.setText("");
        descField.setText("");
        nameField.requestFocus();

        // Scroll down to the new task
        SwingUtilities.invokeLater(() ->
                table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true)));
    }

    // Mark the selected task as completed
    private void markCompleted() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task first.",
                    "No Task Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Task task = tasks.get(row);
        if (task.completed) {
            JOptionPane.showMessageDialog(this, "This task is already completed.",
                    "Already Completed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        task.completed = true;
        saveTasks();
        refreshTable();
        table.setRowSelectionInterval(row, row);
    }

    // Delete the selected task
    private void deleteTask() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task first.",
                    "No Task Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this task?",
                "Delete Task", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            tasks.remove(row);
            saveTasks();
            refreshTable();
        }
    }

    // Logout: close this window and go back to the login screen
    private void logout() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Do you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    // Show all tasks in the table and update the totals
    private void refreshTable() {
        model.setRowCount(0);
        int done = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.completed) {
                done++;
            }
            model.addRow(new Object[]{
                    i + 1, t.name, t.completed ? "Completed" : "Pending", t.description});
        }
        int total = tasks.size();
        statsLabel.setText("Total: " + total + "  |  Completed: " + done
                + "  |  Pending: " + (total - done));
    }

    // Save tasks of this user to their file (one task per line, parts split by a tab)
    private void saveTasks() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (Task t : tasks) {
                writer.println(clean(t.name) + "\t" + t.completed + "\t" + clean(t.description));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save the tasks.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load the tasks of this user after login
    private void loadTasks() {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", -1);
                if (parts.length >= 3) {
                    tasks.add(new Task(parts[0], parts[2], Boolean.parseBoolean(parts[1])));
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not load the tasks.",
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Remove tabs and new lines so the file format stays correct
    private String clean(String text) {
        return text.replace("\t", " ").replace("\n", " ").replace("\r", " ");
    }

    // ---------------------------------------------------------------
    // Login and Register screen (the first screen of the program)
    // ---------------------------------------------------------------
    static class LoginFrame extends JFrame {

        private static final String USERS_FILE = "users.txt";

        private JTextField userField = createField();
        private JPasswordField passField = new JPasswordField();

        LoginFrame() {
            setTitle("Daily Task Manager - Login");
            setSize(480, 400);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setResizable(false);
            getContentPane().setBackground(DARK_BG);
            setLayout(new BorderLayout());

            styleField(passField);
            add(createHeader("Please login to continue"), BorderLayout.NORTH);
            add(createForm(), BorderLayout.CENTER);
        }

        // Form with username, password, Login and Register buttons
        private JPanel createForm() {
            JPanel outer = new JPanel(new GridBagLayout());
            outer.setBackground(DARK_BG);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(PANEL_BG);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.anchor = GridBagConstraints.WEST;

            userField.setPreferredSize(new Dimension(200, 30));
            passField.setPreferredSize(new Dimension(200, 30));

            c.gridx = 0; c.gridy = 0;
            panel.add(createLabel("Username:"), c);
            c.gridx = 1;
            panel.add(userField, c);
            c.gridx = 0; c.gridy = 1;
            panel.add(createLabel("Password:"), c);
            c.gridx = 1;
            panel.add(passField, c);

            JButton loginButton = createButton("Login", PURPLE, Color.WHITE);
            JButton registerButton = createButton("Register", GREEN, new Color(20, 40, 30));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttons.setBackground(PANEL_BG);
            buttons.add(loginButton);
            buttons.add(registerButton);
            c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(16, 6, 6, 6);
            panel.add(buttons, c);

            loginButton.addActionListener(e -> login());
            registerButton.addActionListener(e -> register());
            passField.addActionListener(e -> login());                  // Enter in password = login
            userField.addActionListener(e -> passField.requestFocus()); // Enter in username = go to password

            outer.add(panel);
            return outer;
        }

        // Check the username and password, then open the task manager
        private void login() {
            String user = userField.getText().trim().toLowerCase();
            String pass = new String(passField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                message("Please enter your username and password.");
                return;
            }
            String saved = findHash(user);
            if (saved == null || !saved.equals(hash(pass))) {
                message("Wrong username or password.");
                passField.setText("");
                return;
            }
            dispose();
            new DTM(user).setVisible(true);   // opens the main window
        }

        // Create a new account
        private void register() {
            String user = userField.getText().trim().toLowerCase();
            String pass = new String(passField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                message("Please enter a username and a password.");
            } else if (!user.matches("[a-z0-9]+")) {
                message("Username can only have letters and numbers.");
            } else if (pass.length() < 4) {
                message("Password must be at least 4 characters.");
            } else if (findHash(user) != null) {
                message("That username is already taken.");
            } else {
                try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE, true))) {
                    writer.println(user + "\t" + hash(pass));
                    message("Account created. You can login now.");
                    passField.setText("");
                } catch (IOException e) {
                    message("Could not save the account.");
                }
            }
        }

        private void message(String text) {
            JOptionPane.showMessageDialog(this, text);
        }

        // Find the saved password hash of a user (null if the user does not exist)
        private static String findHash(String user) {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (parts.length == 2 && parts[0].equals(user)) {
                        return parts[1];
                    }
                }
            } catch (IOException e) {
                // If the file cannot be read, treat it as no user found
            }
            return null;
        }

        // The password is saved as a SHA-256 hash, not as plain text
        private static String hash(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                return password;
            }
        }
    }

    public static void main(String[] args) {
        // The program starts at the login screen
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}