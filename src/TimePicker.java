import javax.swing.*;
import java.awt.FlowLayout;
import java.time.LocalTime;

/**
 * A small reusable Swing component for choosing a time of day: two spinners
 * (hour 0-23 and minute 0-59) with a ":" between them.
 * It extends JPanel, so it can be placed in a layout like any other component.
 */
public class TimePicker extends JPanel {
    private final JSpinner hour;
    private final JSpinner minute;

    public TimePicker(LocalTime initial) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        hour = new JSpinner(new SpinnerNumberModel(initial.getHour(), 0, 23, 1));
        minute = new JSpinner(new SpinnerNumberModel(initial.getMinute(), 0, 59, 5));
        hour.setEditor(new JSpinner.NumberEditor(hour, "00"));     // show 07 instead of 7
        minute.setEditor(new JSpinner.NumberEditor(minute, "00"));
        add(hour);
        add(new JLabel(":"));
        add(minute);
    }

    public LocalTime getTime() {
        return LocalTime.of((Integer) hour.getValue(), (Integer) minute.getValue());
    }

    public void setTime(LocalTime t) {
        hour.setValue(t.getHour());
        minute.setValue(t.getMinute());
    }

    /** Runs 'action' whenever either spinner changes. */
    public void onChange(final Runnable action) {
        javax.swing.event.ChangeListener l = new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) { action.run(); }
        };
        hour.addChangeListener(l);
        minute.addChangeListener(l);
    }
}
