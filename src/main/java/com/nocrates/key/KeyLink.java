package com.nocrates.key;

/**
 * A crate's requirement on a key: which key, how many, and the consumption priority
 * (lower priority is consumed first when several linked keys are held).
 */
public record KeyLink(String keyId, int amount, int priority) {

    public KeyLink {
        amount = Math.max(1, amount);
    }
}
