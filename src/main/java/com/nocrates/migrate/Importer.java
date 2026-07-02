package com.nocrates.migrate;

/** Best-effort config importer from another crates plugin. */
public interface Importer {

    /** Id used in /crates migrate &lt;id&gt;, e.g. "crazycrates". */
    String id();

    /** Whether the source plugin's files are present on this server. */
    boolean detect();

    ImportReport run();
}
