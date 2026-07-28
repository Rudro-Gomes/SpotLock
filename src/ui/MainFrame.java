package ui;

import concurrent.ExpiryScheduler;
import exception.BookingNotFoundException;
import model.Floor;
import model.Slot;
import service.ParkingService;
import util.AppLogger;
import util.TimeUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

// Main ParkVault window: a floor selector, the live slot grid, a toolbar of
// actions (find vehicle, history, concurrency demo) and the background expiry
// scheduler, which is shut down cleanly when the window closes.
public class MainFrame extends JFrame {

    private final ParkingService parkingService;
    private final JComboBox<Floor> floorSelector = new JComboBox<>();
    private final SlotGridPanel gridPanel;
    private final ExpiryScheduler expiryScheduler;

    public MainFrame(ParkingService parkingService) {
        this.parkingService = parkingService;
        this.gridPanel = new SlotGridPanel(parkingService);

        setTitle("SpotLock - Smart Parking System");
        setSize(980, 660);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.headerPanel("SpotLock", "Smart Parking System"), BorderLayout.NORTH);
        top.add(buildControls(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        add(gridPanel, BorderLayout.CENTER);
        add(buildLegend(), BorderLayout.SOUTH);

        // Start the background auto-release thread; it refreshes the grid.
        this.expiryScheduler = new ExpiryScheduler(parkingService, gridPanel::refreshCurrentFloor);
        expiryScheduler.start();

        // Stop the scheduler cleanly before the application exits.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                expiryScheduler.stop();
                dispose();
                System.exit(0);
            }
        });

        loadFloors();
    }

    // ---- Toolbar: floor selector on the left, actions on the right ----
    private JPanel buildControls() {
        JPanel controls = new JPanel(new BorderLayout());
        controls.setBackground(UITheme.BACKGROUND);
        controls.setBorder(new EmptyBorder(16, 24, 4, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(UITheme.BACKGROUND);
        JLabel floorLabel = new JLabel("Floor:");
        floorLabel.setFont(UITheme.FONT_BODY_BOLD);
        left.add(floorLabel);
        floorSelector.addActionListener(e -> loadSelectedFloor());
        left.add(floorSelector);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(UITheme.BACKGROUND);
        JButton findBtn = UITheme.secondaryButton("Find My Vehicle");
        JButton historyBtn = UITheme.secondaryButton("History");
        JButton simulateBtn = UITheme.accentButton("Simulate 10 Concurrent Bookings");
        findBtn.addActionListener(e -> onFindVehicle());
        historyBtn.addActionListener(e -> onShowHistory());
        simulateBtn.addActionListener(e -> onSimulate());
        right.add(findBtn);
        right.add(historyBtn);
        right.add(simulateBtn);

        controls.add(left, BorderLayout.WEST);
        controls.add(right, BorderLayout.EAST);
        return controls;
    }

    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 12));
        legend.setBackground(UITheme.BACKGROUND);
        legend.add(legendItem(UITheme.SUCCESS, "Free"));
        legend.add(legendItem(UITheme.DANGER, "Occupied"));
        legend.add(legendItem(UITheme.DISABLED_GREY, "Disabled"));
        return legend;
    }

    private JPanel legendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        item.setBackground(UITheme.BACKGROUND);
        item.add(new UITheme.Swatch(color));
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT);
        item.add(label);
        return item;
    }

    // ---- Floors ----
    private void loadFloors() {
        new SwingWorker<List<Floor>, Void>() {
            @Override
            protected List<Floor> doInBackground() {
                return parkingService.getAllFloors();
            }

            @Override
            protected void done() {
                try {
                    for (Floor floor : get()) {
                        floorSelector.addItem(floor);
                    }
                    if (floorSelector.getItemCount() > 0) {
                        floorSelector.setSelectedIndex(0);
                        loadSelectedFloor();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    reportError("Could not load parking floors.", ex);
                }
            }
        }.execute();
    }

    private void loadSelectedFloor() {
        Floor floor = (Floor) floorSelector.getSelectedItem();
        if (floor != null) {
            gridPanel.showFloor(floor);
        }
    }

    // ---- Find my vehicle ----
    private void onFindVehicle() {
        String plate = JOptionPane.showInputDialog(this,
                "Enter your vehicle number:", "Find My Vehicle",
                JOptionPane.QUESTION_MESSAGE);
        if (plate == null || plate.trim().isEmpty()) {
            return;
        }

        new SwingWorker<ParkingService.VehicleLocation, Void>() {
            @Override
            protected ParkingService.VehicleLocation doInBackground() throws Exception {
                return parkingService.locateVehicle(plate);
            }

            @Override
            protected void done() {
                try {
                    ParkingService.VehicleLocation location = get();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Vehicle " + location.getVehicleNo() + " is parked at:\n\n"
                                    + "Floor: " + location.getFloorName() + "\n"
                                    + "Slot: " + location.getSlotCode() + "\n"
                                    + "Entered: " + TimeUtil.display(location.getEntryTime()) + "\n"
                                    + "Expected exit: " + TimeUtil.display(location.getExpectedExit()),
                            "Vehicle Found", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BookingNotFoundException) {
                        JOptionPane.showMessageDialog(MainFrame.this, cause.getMessage(),
                                "Not Found", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        reportError("Vehicle search failed.", cause);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ---- History ----
    private void onShowHistory() {
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                return parkingService.getHistoryRows();
            }

            @Override
            protected void done() {
                try {
                    new HistoryPanel(MainFrame.this, get()).setVisible(true);
                } catch (InterruptedException | ExecutionException ex) {
                    reportError("Could not load booking history.", ex);
                }
            }
        }.execute();
    }

    // ---- Concurrency demo: fire 10 threads at one slot ----
    private void onSimulate() {
        Slot freeSlot = gridPanel.getFirstFreeSlot();
        if (freeSlot == null) {
            JOptionPane.showMessageDialog(this,
                    "No free slot on this floor to run the demo on.",
                    "Demo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Fire 10 threads at slot " + freeSlot.getCode() + " at the same time?\n"
                        + "(The slot is reset to FREE first. Exactly one should win.)",
                "Simulate 10 Concurrent Bookings", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        new SwingWorker<ParkingService.SimulationResult, Void>() {
            @Override
            protected ParkingService.SimulationResult doInBackground() {
                return parkingService.simulateConcurrentBookings(freeSlot.getId(), 10);
            }

            @Override
            protected void done() {
                try {
                    showSimulationResult(freeSlot, get());
                    gridPanel.refreshCurrentFloor();
                } catch (InterruptedException | ExecutionException ex) {
                    reportError("The concurrency demo failed.", ex);
                }
            }
        }.execute();
    }

    private void showSimulationResult(Slot slot, ParkingService.SimulationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target slot: ").append(slot.getCode()).append("\n");
        sb.append("Successes: ").append(result.getSuccessCount())
          .append("   Rejections: ").append(result.getRejectedCount()).append("\n\n");
        for (String line : result.getLog()) {
            sb.append(line).append("\n");
        }
        boolean correct = result.getSuccessCount() == 1;
        sb.append("\n").append(correct
                ? "PASS - exactly one thread won the slot."
                : "UNEXPECTED - expected exactly one success.");

        JTextArea area = new JTextArea(sb.toString(), 16, 44);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);

        JOptionPane.showMessageDialog(this, scroll,
                "Concurrency Demo Result",
                correct ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    // One place to show an unexpected error and log its full stack trace.
    private void reportError(String message, Throwable cause) {
        AppLogger.error(message, cause);
        JOptionPane.showMessageDialog(this,
                message + "\n\nDetails: " + (cause == null ? "unknown" : cause.getMessage()),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
