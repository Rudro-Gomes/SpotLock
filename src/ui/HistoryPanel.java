package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

// Modal window that shows every booking (past and present) in a read-only table.
public class HistoryPanel extends JDialog {

    private static final String[] COLUMNS = {
            "ID", "Vehicle", "Floor", "Slot", "Entry",
            "Expected Exit", "Actual Exit", "Amount", "Status"
    };

    public HistoryPanel(Window owner, List<String[]> rows) {
        super(owner, "Booking History", ModalityType.APPLICATION_MODAL);

        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        add(UITheme.headerPanel("Booking History",
                rows.size() + " booking(s) recorded"), BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   // the history table is read-only
            }
        };
        for (String[] row : rows) {
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(new EmptyBorder(16, 20, 8, 20));
        content.add(scrollPane, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JButton closeBtn = UITheme.primaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(UITheme.BACKGROUND);
        buttons.setBorder(new EmptyBorder(0, 20, 16, 20));
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        setSize(880, 460);
        setLocationRelativeTo(owner);
    }
}
