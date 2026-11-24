import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Swing user interface. Three tabs:
 *   1. Daily Check-in : the form where the user enters wake time, sleep, water ...
 *   2. Your Routine   : score, findings and the recommended preset routine as a timetable
 *   3. History        : every saved check-in, with its score and recommended routine
 *
 * The GUI contains NO health rules. It only collects input, calls
 * RoutineAdvisor.analyze() and displays the AnalysisResult it gets back.
 */
public class RoutineAdvisorGUI extends JFrame {

    private static final Color DARK_GREEN = new Color(34, 91, 69);
    private static final Color PALE_GREEN = new Color(235, 246, 237);
    private static final Color ACCENT = new Color(92, 151, 113);
    private static final Color TEXT = new Color(40, 50, 45);
    private static final Color MUTED = new Color(100, 115, 106);
    private static final int CONTENT_WIDTH = 640;   // pixel width used for wrapped HTML text

    private final RoutineAdvisor advisor = new RoutineAdvisor();
    private final EntryStore store = new EntryStore();

    private final JTabbedPane tabs = new JTabbedPane();

    // check-in form widgets
    private final TimePicker wakePicker = new TimePicker(LocalTime.of(7, 0));
    private final TimePicker bedPicker = new TimePicker(LocalTime.of(23, 30));
    private final JLabel sleepLabel = new JLabel();
    private final JSlider waterSlider = new JSlider(0, 15, 5);
    private final JSlider exerciseSlider = new JSlider(0, 180, 15);
    private final JSlider screenSlider = new JSlider(0, 14, 4);
    private final JSlider stressSlider = new JSlider(1, 5, 3);
    private final JComboBox<Goal> goalBox = new JComboBox<>(Goal.values());

    // results tab
    private final JPanel resultsPanel = new JPanel(new BorderLayout());
    private final JScrollPane resultsScroll = new JScrollPane(resultsPanel);
    private AnalysisResult currentResult;

    // history tab
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Date", "Sleep (h)", "Water", "Exercise", "Screen (h)", "Stress", "Score", "Recommended routine"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable historyTable = new JTable(historyModel);

    public RoutineAdvisorGUI() {
        setTitle("Daily Routine Advisor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 760);
        setMinimumSize(new Dimension(760, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(PALE_GREEN);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.addTab("1. Daily Check-in", buildCheckInTab());
        resultsPanel.setBackground(PALE_GREEN);
        resultsScroll.setBorder(null);
        resultsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        resultsScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab("2. Your Routine", resultsScroll);
        tabs.addTab("3. History", buildHistoryTab());
        add(tabs, BorderLayout.CENTER);

        showPlaceholder();
        loadLatestIntoForm();
        refreshHistory();
        updateSleepLabel();
    }

    // =====================================================================
    //  HEADER
    // =====================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_GREEN);
        header.setBorder(new EmptyBorder(18, 24, 16, 24));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Daily Routine Advisor");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        JLabel subtitle = new JLabel("Tell us about your day. We'll suggest the routine that fits you best.");
        subtitle.setForeground(new Color(220, 239, 224));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);
        header.add(titles, BorderLayout.WEST);

        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")));
        date.setForeground(Color.WHITE);
        date.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.add(date, BorderLayout.EAST);
        return header;
    }

    // =====================================================================
    //  TAB 1: CHECK-IN FORM
    // =====================================================================
    private JComponent buildCheckInTab() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(PALE_GREEN);
        page.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel card = card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(9, 6, 9, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(card, c, row++, "Wake-up time", wakePicker, null);
        addRow(card, c, row++, "Bedtime (last night)", bedPicker, null);

        sleepLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        addRow(card, c, row++, "Sleep duration", sleepLabel, null);

        JLabel waterValue = new JLabel();
        bindSlider(waterSlider, waterValue, "glasses");
        addRow(card, c, row++, "Water drunk", waterSlider, waterValue);

        JLabel exerciseValue = new JLabel();
        bindSlider(exerciseSlider, exerciseValue, "min");
        addRow(card, c, row++, "Exercise / activity", exerciseSlider, exerciseValue);

        JLabel screenValue = new JLabel();
        bindSlider(screenSlider, screenValue, "hours");
        addRow(card, c, row++, "Leisure screen time", screenSlider, screenValue);

        JLabel stressValue = new JLabel();
        stressSlider.setMajorTickSpacing(1);
        stressSlider.setPaintTicks(true);
        stressSlider.setSnapToTicks(true);
        stressSlider.addChangeListener(e -> stressValue.setText(stressText(stressSlider.getValue())));
        stressValue.setText(stressText(stressSlider.getValue()));
        addRow(card, c, row++, "Stress level", stressSlider, stressValue);

        addRow(card, c, row++, "Main goal", goalBox, null);

        wakePicker.onChange(this::updateSleepLabel);
        bedPicker.onChange(this::updateSleepLabel);

        JLabel hint = new JLabel("<html><body style='width:520px'>"
                + "Screen time means leisure screens only (social media, videos, games), not study or work. "
                + "Enter bedtime as the time you went to sleep last night.</body></html>");
        hint.setForeground(MUTED);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        c.gridx = 0; c.gridy = row++; c.gridwidth = 3;
        card.add(hint, c);

        JButton analyze = new JButton("Analyze & Get My Routine");
        styleButton(analyze);
        analyze.addActionListener(e -> analyzeAndShow());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        bottom.setOpaque(false);
        bottom.add(analyze);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.NORTH);
        wrap.add(bottom, BorderLayout.CENTER);
        page.add(wrap, BorderLayout.NORTH);
        return page;
    }

    private void addRow(JPanel card, GridBagConstraints c, int row, String label, JComponent input, JLabel value) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(TEXT);
        c.gridy = row; c.gridwidth = 1;
        c.gridx = 0; c.weightx = 0;
        card.add(l, c);
        c.gridx = 1; c.weightx = 1;
        card.add(input, c);
        c.gridx = 2; c.weightx = 0;
        if (value != null) {
            value.setFont(new Font("SansSerif", Font.BOLD, 13));
            value.setForeground(DARK_GREEN);
            value.setPreferredSize(new Dimension(150, 20));
            card.add(value, c);
        } else {
            card.add(Box.createHorizontalStrut(150), c);
        }
    }

    /** Keeps a label showing the slider's current value, e.g. "6 glasses". */
    private void bindSlider(final JSlider slider, final JLabel label, final String unit) {
        slider.setOpaque(false);
        label.setText(slider.getValue() + " " + unit);
        slider.addChangeListener(e -> label.setText(slider.getValue() + " " + unit));
    }

    private static String stressText(int level) {
        String[] names = {"", "1 - Very calm", "2 - Calm", "3 - Moderate", "4 - Stressed", "5 - Very stressed"};
        return names[level];
    }

    /** Recalculates "Sleep duration" live whenever a time spinner changes. */
    private void updateSleepLabel() {
        double h = DailyEntry.calculateSleepHours(bedPicker.getTime(), wakePicker.getTime());
        sleepLabel.setText(String.format("%.1f hours", h));
        sleepLabel.setForeground(h >= 7 && h <= 9 ? ScoreBar.GOOD : (h >= 6 && h < 7 ? ScoreBar.FAIR : ScoreBar.POOR));
    }

    private void loadLatestIntoForm() {
        DailyEntry last = store.getLatest();
        if (last != null) loadIntoForm(last);
    }

    private void loadIntoForm(DailyEntry e) {
        wakePicker.setTime(e.getWakeTime());
        bedPicker.setTime(e.getBedTime());
        waterSlider.setValue(Math.min(e.getWaterGlasses(), waterSlider.getMaximum()));
        exerciseSlider.setValue(Math.min(e.getExerciseMinutes(), exerciseSlider.getMaximum()));
        screenSlider.setValue(Math.min(e.getScreenHours(), screenSlider.getMaximum()));
        stressSlider.setValue(e.getStressLevel());
        goalBox.setSelectedItem(e.getGoal());
        updateSleepLabel();
    }

    /** Reads the form, validates it, runs the rule engine, saves, and shows the result. */
    void analyzeAndShow() {
        try {
            DailyEntry entry = new DailyEntry(LocalDate.now(), wakePicker.getTime(), bedPicker.getTime(),
                    waterSlider.getValue(), exerciseSlider.getValue(), screenSlider.getValue(),
                    stressSlider.getValue(), (Goal) goalBox.getSelectedItem());
            currentResult = advisor.analyze(entry);
            store.save(entry);
            showResult(currentResult);
            refreshHistory();
            tabs.setSelectedIndex(1);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Please check your input", JOptionPane.WARNING_MESSAGE);
        }
    }

    // =====================================================================
    //  TAB 2: RESULTS
    // =====================================================================
    private void showPlaceholder() {
        resultsPanel.removeAll();
        JLabel l = new JLabel("Fill in the Daily Check-in and press \"Analyze & Get My Routine\".", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.PLAIN, 15));
        l.setForeground(MUTED);
        l.setBorder(new EmptyBorder(60, 20, 20, 20));
        resultsPanel.add(l, BorderLayout.NORTH);
    }

    private void showResult(AnalysisResult r) {
        resultsPanel.removeAll();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(14, 24, 20, 24));

        content.add(buildScoreCard(r));
        content.add(Box.createVerticalStrut(12));
        content.add(buildFindingsCard(r));
        content.add(Box.createVerticalStrut(12));
        content.add(buildRoutineCard(r));

        resultsPanel.add(content, BorderLayout.NORTH);
        resultsPanel.revalidate();
        resultsPanel.repaint();
        SwingUtilities.invokeLater(() -> resultsScroll.getVerticalScrollBar().setValue(0));
    }

    private JPanel buildScoreCard(AnalysisResult r) {
        JPanel card = card();
        card.setLayout(new BorderLayout(24, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel("Your routine score");
        caption.setFont(new Font("SansSerif", Font.BOLD, 14));
        caption.setForeground(MUTED);
        JLabel big = new JLabel(r.getOverallScore() + " / 100");
        big.setFont(new Font("SansSerif", Font.BOLD, 38));
        big.setForeground(ScoreBar.colorFor(r.getOverallScore()));
        JLabel verdict = new JLabel(r.getVerdict());
        verdict.setFont(new Font("SansSerif", Font.PLAIN, 13));
        verdict.setForeground(TEXT);
        left.add(caption);
        left.add(big);
        left.add(verdict);
        card.add(left, BorderLayout.WEST);

        JPanel bars = new JPanel(new GridBagLayout());
        bars.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridy = 0;
        for (Metric m : Metric.values()) {
            int score = r.getScores().get(m);
            c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST;
            JLabel name = new JLabel(m.getLabel());
            name.setPreferredSize(new Dimension(130, 18));
            name.setForeground(TEXT);
            bars.add(name, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            bars.add(new ScoreBar(score), c);
            c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            JLabel num = new JLabel(score + "  (" + r.getStatuses().get(m).getLabel() + ")");
            num.setPreferredSize(new Dimension(95, 18));
            num.setForeground(MUTED);
            bars.add(num, c);
            c.gridy++;
        }
        card.add(bars, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFindingsCard(AnalysisResult r) {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(heading("What we found"));
        for (AnalysisResult.Finding f : r.getFindings()) {
            Color dot = f.isGood() ? ScoreBar.GOOD : ScoreBar.POOR;
            String hex = String.format("#%02x%02x%02x", dot.getRed(), dot.getGreen(), dot.getBlue());
            JLabel l = new JLabel("<html><body style='width:" + CONTENT_WIDTH + "px'><span style='color:" + hex
                    + "'>&#9679;</span>&nbsp; " + esc(f.getText()) + "</body></html>");
            l.setFont(new Font("SansSerif", Font.PLAIN, 13));
            l.setForeground(TEXT);
            l.setBorder(new EmptyBorder(3, 0, 3, 0));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(l);
        }
        return card;
    }

    /** The recommended routine plus a drop-down to look at the other presets. */
    private JPanel buildRoutineCard(final AnalysisResult r) {
        final JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(heading("Recommended routine"));

        // drop-down listing every preset in ranked order
        final List<RoutineMatch> ranking = r.getRanking();
        final JComboBox<String> chooser = new JComboBox<>();
        for (int i = 0; i < ranking.size(); i++) {
            RoutineMatch m = ranking.get(i);
            String prefix = (i == 0) ? "Best match: " : "#" + (i + 1) + ": ";
            chooser.addItem(prefix + m.getRoutine().getName() + "  (" + m.getPoints() + " pts)");
        }
        chooser.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooser.setMaximumSize(new Dimension(CONTENT_WIDTH, 28));

        final JPanel detail = new JPanel();
        detail.setOpaque(false);
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        chooser.addActionListener(e -> {
            fillRoutineDetail(detail, ranking.get(chooser.getSelectedIndex()), r, chooser.getSelectedIndex() == 0);
            detail.revalidate();
            detail.repaint();
        });
        fillRoutineDetail(detail, ranking.get(0), r, true);

        JLabel pick = new JLabel("Compare with other routines:");
        pick.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pick.setForeground(MUTED);
        pick.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pick);
        card.add(Box.createVerticalStrut(3));
        card.add(chooser);
        card.add(Box.createVerticalStrut(10));
        card.add(detail);
        return card;
    }

    private void fillRoutineDetail(JPanel detail, RoutineMatch match, AnalysisResult r, boolean isBest) {
        detail.removeAll();
        Routine routine = match.getRoutine();

        JLabel name = new JLabel(routine.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 22));
        name.setForeground(DARK_GREEN);
        addLeft(detail, name);

        JLabel tag = new JLabel(routine.getTagline());
        tag.setFont(new Font("SansSerif", Font.ITALIC, 13));
        tag.setForeground(MUTED);
        addLeft(detail, tag);
        detail.add(Box.createVerticalStrut(8));

        addLeft(detail, html(esc(routine.getDescription()), 13, TEXT));
        detail.add(Box.createVerticalStrut(8));

        // WHY this routine: the satisfied rules
        String why = isBest ? "Why we picked this" : "How well it fits you";
        addLeft(detail, subHeading(why + "  (" + match.getPoints() + " of " + match.getMaxPoints() + " rule points)"));
        if (match.getReasons().isEmpty()) {
            addLeft(detail, html("None of this routine's rules apply to today's numbers.", 13, MUTED));
        } else {
            for (String reason : match.getReasons()) {
                addLeft(detail, html("&#8226;&nbsp; " + esc(reason), 13, TEXT));
            }
        }
        detail.add(Box.createVerticalStrut(10));

        // timetable built from the user's own wake-up time
        LocalTime wake = r.getEntry().getWakeTime();
        addLeft(detail, subHeading("Your timetable (built around your " + wake + " wake-up)"));
        detail.add(Box.createVerticalStrut(4));
        addLeft(detail, buildTimetable(routine, wake));
        detail.add(Box.createVerticalStrut(10));

        addLeft(detail, subHeading("Tips"));
        List<String> tips = new ArrayList<>(routine.getTips());
        tips.add("For your goal (" + r.getEntry().getGoal().getLabel() + "): " + r.getEntry().getGoal().getTip());
        for (String t : tips) {
            addLeft(detail, html("&#8226;&nbsp; " + esc(t), 13, TEXT));
        }
    }

    private JComponent buildTimetable(Routine routine, LocalTime wake) {
        List<Routine.TimedStep> steps = routine.buildTimetable(wake);
        DefaultTableModel model = new DefaultTableModel(new String[]{"Time", "What to do"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (Routine.TimedStep s : steps) {
            model.addRow(new Object[]{s.getTime().format(fmt), s.getActivity()});
        }
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(244, 249, 245));
                l.setForeground(col == 0 ? DARK_GREEN : TEXT);
                l.setFont(l.getFont().deriveFont(col == 0 ? Font.BOLD : Font.PLAIN));
                l.setToolTipText(String.valueOf(v));
                return l;
            }
        });
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createLineBorder(new Color(210, 224, 214)));
        box.add(table.getTableHeader(), BorderLayout.NORTH);
        box.add(table, BorderLayout.CENTER);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28 + 26 * steps.size()));
        return box;
    }

    // =====================================================================
    //  TAB 3: HISTORY
    // =====================================================================
    private JComponent buildHistoryTab() {
        JPanel page = new JPanel(new BorderLayout(0, 10));
        page.setBackground(PALE_GREEN);
        page.setBorder(new EmptyBorder(16, 24, 16, 24));

        historyTable.setRowHeight(26);
        historyTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        historyTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getColumnModel().getColumn(7).setPreferredWidth(220);
        page.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JButton load = new JButton("Load into form");
        JButton view = new JButton("View routine");
        JButton delete = new JButton("Delete selected");
        for (JButton b : new JButton[]{view, load, delete}) styleSmallButton(b);
        view.addActionListener(e -> withSelectedEntry(entry -> {
            currentResult = advisor.analyze(entry);
            showResult(currentResult);
            tabs.setSelectedIndex(1);
        }));
        load.addActionListener(e -> withSelectedEntry(entry -> {
            loadIntoForm(entry);
            tabs.setSelectedIndex(0);
        }));
        delete.addActionListener(e -> withSelectedEntry(entry -> {
            int ok = JOptionPane.showConfirmDialog(this, "Delete the entry for " + entry.getDate() + "?",
                    "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                store.delete(entry.getDate());
                refreshHistory();
            }
        }));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(view);
        buttons.add(load);
        buttons.add(delete);
        page.add(buttons, BorderLayout.SOUTH);
        return page;
    }

    private interface EntryAction { void run(DailyEntry entry); }

    private void withSelectedEntry(EntryAction action) {
        int row = historyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row in the table first.");
            return;
        }
        action.run(store.getAll().get(row));   // table rows are in the same order as the store
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (DailyEntry e : store.getAll()) {
            AnalysisResult r = advisor.analyze(e);
            historyModel.addRow(new Object[]{e.getDate(), String.format("%.1f", e.getSleepHours()), e.getWaterGlasses(),
                    e.getExerciseMinutes(), e.getScreenHours(), e.getStressLevel(), r.getOverallScore(),
                    r.getBestMatch().getRoutine().getName()});
        }
    }

    // =====================================================================
    //  SMALL HELPERS
    // =====================================================================
    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 224, 214)), new EmptyBorder(14, 18, 14, 18)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        l.setForeground(DARK_GREEN);
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel subHeading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(TEXT);
        l.setBorder(new EmptyBorder(0, 0, 3, 0));
        return l;
    }

    private JLabel html(String body, int size, Color color) {
        JLabel l = new JLabel("<html><body style='width:" + CONTENT_WIDTH + "px'>" + body + "</body></html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, size));
        l.setForeground(color);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    private void addLeft(JPanel p, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
    }

    /** Escapes characters that have a special meaning in HTML (labels render HTML). */
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void styleButton(JButton b) {
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(11, 26, 11, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSmallButton(JButton b) {
        styleButton(b);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
    }

    // =====================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RoutineAdvisorGUI().setVisible(true));
    }
}
