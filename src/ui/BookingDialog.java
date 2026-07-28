package ui;

import model.Slot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

// Small modal form for booking one slot: asks for the vehicle number and how
// many hours the driver expects to park. Returns nothing itself - the caller
// reads isConfirmed(), getVehicleNo() and getExpectedHours() afterwards.
public class BookingDialog extends JDialog {

    private final JTextField vehicleField = new JTextField(16);
    private final JSpinner hoursSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 24, 1));
    private boolean confirmed = false;

    public BookingDialog(Window owner, Slot slot) {
        super(owner, "Book Slot " + slot.getCode(), ModalityType.APPLICATION_MODAL);

        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        add(UITheme.headerPanel("Book Slot " + slot.getCode(),
                slot.getVehicleType() + " slot"), BorderLayout.NORTH);

        JPanel form = UITheme.cardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(UITheme.sectionLabel("Vehicle Number"), gbc);
        gbc.gridx = 1;
        form.add(vehicleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(UITheme.sectionLabel("Expected Hours"), gbc);
        gbc.gridx = 1;
        form.add(hoursSpinner, gbc);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(new EmptyBorder(16, 20, 8, 20));
        content.add(form, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JButton bookBtn = UITheme.successButton("Book");
        JButton cancelBtn = UITheme.secondaryButton("Cancel");
        bookBtn.addActionListener(e -> confirmAndClose());
        cancelBtn.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(UITheme.BACKGROUND);
        buttons.setBorder(new EmptyBorder(0, 20, 16, 20));
        buttons.add(cancelBtn);
        buttons.add(bookBtn);
        add(buttons, BorderLayout.SOUTH);

        // Enter confirms, Escape cancels.
        getRootPane().setDefaultButton(bookBtn);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(owner);
    }

    private void confirmAndClose() {
        if (vehicleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a vehicle number.",
                    "Vehicle Number Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }

    public String getVehicleNo() { return vehicleField.getText().trim(); }

    public int getExpectedHours() { return (Integer) hoursSpinner.getValue(); }
}
