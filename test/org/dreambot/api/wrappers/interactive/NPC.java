package org.dreambot.api.wrappers.interactive;

import org.dreambot.api.methods.map.Tile;

public class NPC implements Interactable {
    public int getID() { return 0; }
    public Tile getTile() { return new Tile(0, 0, 0); }
    public boolean isInCombat() { return false; }
    public Interactable getInteractingCharacter() { return null; }
    public boolean isInteractable() { return false; }
    public int getHealthPercent() { return 0; }
    public boolean exists() { return false; }
    public String getName() { return ""; }
    @Override
    public boolean interact(String action) { return false; }
}
