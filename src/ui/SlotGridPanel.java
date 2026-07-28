package ui;

import exception.BookingNotFoundException;
import exception.InvalidVehicleException;
import exception.SlotOccupiedException;
import model.Floor;
import model.Slot;
import service.ParkingService;
import util.AppLogger;
import util.TimeUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

// Shows the live, colour-coded slot grid for one floor. Clicking a FREE slot
// opens the booking dialog; clicking an OCCUPIED slot offers checkout. All
// database work runs off the event thread inside SwingWorker.
public class SlotGridPanel extends JPanel {

    private static final int COLUMNS = 6;

    private final ParkingService parkingService;
    private final JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 10, 10));
    private final JLabel availabilityLabel = new JLabel();

    private Floor currentFloor;
    private List<Slot> currentSlots;

    public SlotGridPanel(ParkingService parkingService) {
        this.parkingService = parkingService;
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(4, 24, 4, 24));

        availabilityLabel.setFont(UITheme.FONT_HEADER);
        availabilityLabel.setForeground(UITheme.TEXT);

        JPanel card = UITheme.cardPanel(new BorderLayout(0, 12));
        card.add(availabilityLabel, BorderLayout.NORTH);

        grid.setBackground(UITheme.CARD);
        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UITheme.CARD);
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    // Switches the grid to a different floor and loads its slots.
    public void showFloor(Floor floor) {
        this.currentFloor = floor;
        reload();
    }

    // Reloads the current floor - called after a booking/checkout and by the
    // background ExpiryScheduler (via SwingUtilities.invokeLater).
    public void refreshCurrentFloor() {
        if (currentFloor != null) {
            reload();
        }
    }

    // Returns the first FREE slot on screen, used to pick a target for the
    // "Simulate 10 Concurrent Bookings" demo. May be null if the floor is full.
    public Slot getFirstFreeSlot() {
        if (currentSlots == null) {
            return null;
        }
        for (Slot slot : currentSlots) {
            if (slot.isFree()) {
                return slot;
            }
        }
        return null;
    }

    // Loads the slots for the current floor off the event thread, then renders.
    private void reload() {
        final int floorId = currentFloor.getId();
        new SwingWorker<List<Slot>, Void>() {
            @Override
            protected List<Slot> doInBackground() {
                return parkingService.getSlotsForFloor(floorId);
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (InterruptedException | ExecutionException ex) {
                    reportError("Could not load slots for this floor.", ex);
                }
            }
        }.execute();
    }

    private void render(List<Slot> slots) {
        this.currentSlots = slots;
        grid.removeAll();

        int freeCount = 0;
        for (Slot slot : slots) {
            if (slot.isFree()) {
                freeCount++;
            }
            UITheme.SlotButton button = new UITheme.SlotButton(slot.getCode(), slot.getStatus());
            if (slot.isFree()) {
                button.addActionListener(e -> onFreeSlotClicked(slot));
            } else if (slot.isOccupied()) {
                button.addActionListener(e -> onOccupiedSlotClicked(slot));
            }
            grid.add(button);
        }

        availabilityLabel.setText(currentFloor.getName() + "  -  "
                + freeCount + " of " + slots.size() + " slots free");

        grid.revalidate();
        grid.repaint();
    }

    // ---- Clicking a FREE slot: open the booking dialog ----
    private void onFreeSlotClicked(Slot slot) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        BookingDialog dialog = new BookingDialog(owner, slot);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        final String vehicleNo = dialog.getVehicleNo();
        final int hours = dialog.getExpectedHours();

        new SwingWorker<model.Booking, Void>() {
            @Override
            protected model.Booking doInBackground() throws Exception {
                return parkingService.bookSlot(slot.getId(), vehicleNo, hours);
            }

            @Override
            protected void done() {
                try {
                    model.Booking booking = get();
                    JOptionPane.showMessageDialog(SlotGridPanel.this,
                            "Booking confirmed!\n\nSlot: " + slot.getCode()
                                    + "\nVehicle: " + booking.getVehicleNo()
                                    + "\nExpected exit: " + TimeUtil.display(booking.getExpectedExit()),
                            "Booked", JOptionPane.INFORMATION_MESSAGE);
                    reload();
                } catch (ExecutionException ex) {
                    handleBookingFailure(ex.getCause());
                    reload();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // Turns the cause thrown on the worker thread into a friendly message.
    private void handleBookingFailure(Throwable cause) {
        if (cause instanceof SlotOccupiedException) {
            JOptionPane.showMessageDialog(this, cause.getMessage(),
                    "Slot Taken", JOptionPane.WARNING_MESSAGE);
        } else if (cause instanceof InvalidVehicleException) {
            JOptionPane.showMessageDialog(this, cause.getMessage(),
                    "Invalid Vehicle Number", JOptionPane.WARNING_MESSAGE);
        } else {
            reportError("The booking could not be completed.", cause);
        }
    }

    // ---- Clicking an OCCUPIED slot: offer checkout ----
    private void onOccupiedSlotClicked(Slot slot) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Check out the vehicle in slot " + slot.getCode() + "?",
                "Checkout", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<ParkingService.Receipt, Void>() {
            @Override
            protected ParkingService.Receipt doInBackground() throws Exception {
                return parkingService.checkoutSlot(slot.getId());
            }

            @Override
            protected void done() {
                try {
                    showReceipt(get());
                    reload();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BookingNotFoundException) {
                        JOptionPane.showMessageDialog(SlotGridPanel.this, cause.getMessage(),
                                "Nothing to Check Out", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        reportError("Checkout could not be completed.", cause);
                    }
                    reload();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showReceipt(ParkingService.Receipt receipt) {
        JOptionPane.showMessageDialog(this,
                "Checkout complete!\n\n"
                        + "Slot: " + receipt.getSlotCode() + "\n"
                        + "Vehicle: " + receipt.getVehicleNo() + "\n"
                        + "Entry: " + TimeUtil.display(receipt.getEntryTime()) + "\n"
                        + "Exit: " + TimeUtil.display(receipt.getExitTime()) + "\n"
                        + "Hours charged: " + receipt.getHours() + "\n"
                        + "Amount: " + UITheme.money(receipt.getAmount()),
                "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    // One place to show an unexpected error to the user and log the full trace.
    private void reportError(String message, Throwable cause) {
        AppLogger.error(message, cause);
        JOptionPane.showMessageDialog(this,
                message + "\n\nDetails: " + (cause == null ? "unknown" : cause.getMessage()),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
