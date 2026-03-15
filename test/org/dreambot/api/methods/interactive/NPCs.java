package org.dreambot.api.methods.interactive;

import org.dreambot.api.wrappers.interactive.NPC;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class NPCs {
    public static List<NPC> all(Predicate<NPC> filter) { return new ArrayList<>(); }
}
