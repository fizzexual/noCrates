package com.nocrates.core;

import java.util.ArrayList;
import java.util.List;

/** Runs registered {@link Reloadable}s in registration order on /crates reload. */
public final class ReloadManager {

    private final List<Reloadable> order = new ArrayList<>();

    public void register(Reloadable r) {
        order.add(r);
    }

    public void reloadAll() {
        for (Reloadable r : order) r.reload();
    }
}
