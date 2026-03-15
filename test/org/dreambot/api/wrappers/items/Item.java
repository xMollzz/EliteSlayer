package org.dreambot.api.wrappers.items;

import org.dreambot.api.wrappers.interactive.Interactable;

public class Item implements Interactable {
    @Override
    public boolean interact(String action) { return false; }
    public String getName() { return ""; }
    public int getID() { return 0; }
    public int getAmount() { return 0; }
}
