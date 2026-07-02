package com.nocrates.action;

/** One executable action kind, e.g. [MESSAGE] or [SOUND]. Addons may register more. */
public interface ActionType {

    /** Upper-case id as written between brackets, e.g. "MESSAGE". */
    String id();

    void run(ActionContext ctx, String args);
}
