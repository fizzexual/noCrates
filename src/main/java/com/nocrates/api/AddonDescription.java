package com.nocrates.api;

/** Parsed addon.yml (external addons) or built-in module metadata. */
public record AddonDescription(String name, String main, String version, String author, boolean builtin) {
}
