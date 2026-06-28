package com.nocrates.crate;

import com.nocrates.key.KeyType;
import com.nocrates.reward.DisplaySpec;

/** A crate's key configuration: how it's held, its identifier, and the physical item. */
public final class CrateKeySpec {

    private final KeyType type;
    private final String keyId;
    private final DisplaySpec item;

    public CrateKeySpec(KeyType type, String keyId, DisplaySpec item) {
        this.type = type == null ? KeyType.VIRTUAL : type;
        this.keyId = keyId;
        this.item = item;
    }

    public KeyType type() {
        return type;
    }

    public String keyId() {
        return keyId;
    }

    /** Physical key display, or {@code null} for virtual-only crates. */
    public DisplaySpec item() {
        return item;
    }
}
