package com.nocrates.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistence backend for {@link PlayerData}. Implementations: YAML, MySQL. */
public interface DataStore {

    CompletableFuture<PlayerData> load(UUID id);

    void save(PlayerData data);

    void close();
}
