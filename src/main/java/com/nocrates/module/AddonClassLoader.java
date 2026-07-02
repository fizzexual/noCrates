package com.nocrates.module;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Class loader for one external addon jar. Parent-first against the plugin's own
 * loader, so addons share the plugin's API classes and the server's Bukkit classes.
 */
final class AddonClassLoader extends URLClassLoader {

    AddonClassLoader(URL jar, ClassLoader parent) {
        super(new URL[]{jar}, parent);
    }
}
