package org.dreambot.api.wrappers.interactive;

public class GameObject implements Interactable {
    @Override
    public boolean interact(String action) { return false; }
    public String getName() { return ""; }
}
