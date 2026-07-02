package com.nocrates.migrate;

import java.util.ArrayList;
import java.util.List;

/** What an importer managed to bring over. */
public final class ImportReport {

    private int crates;
    private int rewards;
    private final List<String> notes = new ArrayList<>();

    public void crateImported() {
        crates++;
    }

    public void rewardImported() {
        rewards++;
    }

    public void note(String note) {
        notes.add(note);
    }

    public int crates() {
        return crates;
    }

    public int rewards() {
        return rewards;
    }

    public List<String> notes() {
        return notes;
    }
}
