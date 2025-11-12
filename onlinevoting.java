import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class onlinevoting extends JFrame {

    private static final String URL = "jdbc:mysql://localhost:3306/votingdb";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // change if needed

    private JTextField voterIdField, nameField;
    private JComboBox<String> candidateBox;
    private JTextArea resultArea;
    private JButton voteButton, viewButton, clearButton;

    public onlineVotingSystem() {
        setTitle("Online Voting System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);

        // === MAIN PANEL ===
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // === TITLE ===
        JLabel titleLabel = new JLabel("ONLINE VOTING SYSTEM", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // === INPUT PANEL ===
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Voter Details"));

        inputPanel.add(new JLabel("Voter ID:"));
        voterIdField = new JTextField();
        inputPanel.add(voterIdField);

        inputPanel.add(new JLabel("Voter Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Select Candidate:"));
        candidateBox = new JComboBox<>(new String[]{"Alice", "Bob", "Charlie"});
        inputPanel.add(candidateBox);

        mainPanel.add(inputPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // === BUTTON PANEL ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        voteButton = new JButton("Cast Vote");
        viewButton = new JButton("View Results");
        clearButton = new JButton("Clear Display");

        buttonPanel.add(voteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(clearButton);

        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // === RESULT AREA ===
        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Results"));
        mainPanel.add(scrollPane);

        // === BUTTON ACTIONS ===
        voteButton.addActionListener(e -> castVote());
        viewButton.addActionListener(e -> viewResults());
        clearButton.addActionListener(e -> {
            resultArea.setText("");
            voterIdField.setText("");
            nameField.setText("");
            candidateBox.setSelectedIndex(0);
        });

        add(mainPanel);
        setVisible(true);
    }

    private void castVote() {
        String voterId = voterIdField.getText().trim();
        String voterName = nameField.getText().trim();
        String candidate = (String) candidateBox.getSelectedItem();

        if (voterId.isEmpty() || voterName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠ Please fill in all fields!");
            return;
        }

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            PreparedStatement psCheck = con.prepareStatement("SELECT has_voted FROM voters WHERE voter_id = ?");
            psCheck.setString(1, voterId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                if (rs.getBoolean("has_voted")) {
                    JOptionPane.showMessageDialog(this, "❌ You have already voted!");
                    return;
                } else {
                    PreparedStatement psUpdate = con.prepareStatement("UPDATE voters SET has_voted = TRUE WHERE voter_id = ?");
                    psUpdate.setString(1, voterId);
                    psUpdate.executeUpdate();
                }
            } else {
                PreparedStatement psInsert = con.prepareStatement("INSERT INTO voters (voter_id, name, has_voted) VALUES (?, ?, TRUE)");
                psInsert.setString(1, voterId);
                psInsert.setString(2, voterName);
                psInsert.executeUpdate();
            }

            PreparedStatement psVote = con.prepareStatement("UPDATE candidates SET votes = votes + 1 WHERE name = ?");
            psVote.setString(1, candidate);
            psVote.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Vote cast successfully for " + candidate + "!");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error:\n" + ex.getMessage());
        }
    }

    private void viewResults() {
        resultArea.setText("");
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM candidates");

            resultArea.append(String.format("%-10s %-15s %-10s\n", "ID", "Candidate", "Votes"));
            resultArea.append("------------------------------------------\n");

            while (rs.next()) {
                resultArea.append(String.format("%-10d %-15s %-10d\n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("votes")));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Error fetching results:\n" + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!");
            return;
        }

        SwingUtilities.invokeLater(OnlineVotingSystem::new);
    }
}