package org.dreambot.api.wrappers.items;

import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.wrappers.interactive.Interactable;

public class GroundItem implements Interactable {
    @Override
    public boolean interact(String action) { return false; }
    public String getName() { return ""; }
    public int getID() { return 0; }
    public int getAmount() { return 0; }
    public Tile getTile() { return new Tile(0, 0, 0); }
}
