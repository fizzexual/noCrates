package com.nocrates.key;

import java.util.Locale;

/** How a crate's key is held: virtual balance, a physical item, or either. */
public enum KeyType {

    VIRTUAL,
    PHYSICAL,
    BOTH;

    public static KeyType from(String value) {
        if (value == null) {
            return VIRTUAL;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return VIRTUAL;
        }
    }

    public boolean allowsVirtual() {
        return this == VIRTUAL || this == BOTH;
    }

    public boolean allowsPhysical() {
        return this == PHYSICAL || this == BOTH;
    }
}
