package org.dreambot.api.methods.interactive;

import org.dreambot.api.wrappers.interactive.Player;
import java.util.ArrayList;
import java.util.List;

public class Players {
    public static Player localPlayer() { return new Player(); }
    public static List<Player> all() { return new ArrayList<>(); }
}
