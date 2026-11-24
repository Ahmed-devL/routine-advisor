import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Saves check-ins in a plain text file (history.csv) so the history is still
 * there the next time the app is opened. One line = one day. A CSV file was
 * chosen (instead of Java serialization) because it can be opened in Notepad
 * or Excel, which makes it easy to inspect and to demonstrate.
 *
 * Only ONE entry is kept per date: checking in twice on the same day replaces
 * the earlier entry.
 */
public class EntryStore {
    private static final String HEADER = "date,wake,bed,water,exercise,screen,stress,goal";

    private final File file;
    private final List<DailyEntry> entries = new ArrayList<>();

    public EntryStore(File file) {
        this.file = file;
        load();
    }

    public EntryStore() { this(new File("history.csv")); }

    /** All entries, newest first. */
    public List<DailyEntry> getAll() { return Collections.unmodifiableList(entries); }

    /** The most recent entry, or null if there is none. */
    public DailyEntry getLatest() { return entries.isEmpty() ? null : entries.get(0); }

    public void save(DailyEntry entry) {
        remove(entry.getDate());
        entries.add(entry);
        sortAndWrite();
    }

    public void delete(LocalDate date) {
        remove(date);
        sortAndWrite();
    }

    private void remove(LocalDate date) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getDate().equals(date)) entries.remove(i);
        }
    }

    private void sortAndWrite() {
        Collections.sort(entries, new Comparator<DailyEntry>() {
            @Override
            public int compare(DailyEntry a, DailyEntry b) { return b.getDate().compareTo(a.getDate()); }
        });
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            out.write(HEADER);
            out.newLine();
            for (DailyEntry e : entries) {
                out.write(e.toCsv());
                out.newLine();
            }
        } catch (IOException ex) {
            System.err.println("Could not save history: " + ex.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) return;
        try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line = in.readLine();               // skip header
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    entries.add(DailyEntry.fromCsv(line));
                } catch (RuntimeException bad) {
                    // one damaged line must not stop the whole file from loading
                    System.err.println("Skipping bad history line: " + line);
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not read history: " + ex.getMessage());
        }
        Collections.sort(entries, new Comparator<DailyEntry>() {
            @Override
            public int compare(DailyEntry a, DailyEntry b) { return b.getDate().compareTo(a.getDate()); }
        });
    }
}
