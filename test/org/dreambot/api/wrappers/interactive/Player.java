package org.dreambot.api.wrappers.interactive;

import org.dreambot.api.methods.map.Tile;

public class Player implements Interactable {
    public Tile getTile() { return new Tile(0, 0, 0); }
    public boolean isInCombat() { return false; }
    public String getName() { return ""; }
    public Interactable getInteractingCharacter() { return null; }
    @Override
    public boolean interact(String action) { return false; }
    // Stub: identity-based equals/hashCode (real API may differ)
    @Override
    public boolean equals(Object o) { return this == o; }
    @Override
    public int hashCode() { return System.identityHashCode(this); }
}
