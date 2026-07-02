package com.nocrates.action;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionsParseTest {

    @Test
    void splitsTypeAndArgs() {
        assertEquals("MESSAGE", Actions.split("[MESSAGE] hello <player>")[0]);
        assertEquals("hello <player>", Actions.split("[MESSAGE] hello <player>")[1]);
        assertEquals("TITLE", Actions.split("  [title] a;b;10;60;10")[0]);
        assertEquals("", Actions.split("[CLOSE_INVENTORY]")[1]);
    }

    @Test
    void rejectsNonActionLines() {
        assertNull(Actions.split("say hi"));
        assertNull(Actions.split("[] empty"));
        assertNull(Actions.split(null));
    }

    @Test
    void validateFlagsUnknownTypes() {
        Actions actions = new Actions(null);
        List<String> problems = actions.validate(List.of(
                "[MESSAGE] ok",
                "[NOPE] what",
                "plain text",
                "[DELAY] 2",
                "[COMMAND] console; say hi"
        ));
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).contains("NOPE"));
        assertTrue(problems.get(1).contains("plain text"));
    }
}
