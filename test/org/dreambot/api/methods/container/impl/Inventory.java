package org.dreambot.api.methods.container.impl;

import org.dreambot.api.wrappers.items.Item;
import java.util.function.Predicate;

public class Inventory {
    public static boolean isFull() { return false; }
    public static Item get(int id) { return null; }
    public static Item get(String name) { return null; }
    public static Item get(Predicate<Item> filter) { return null; }
    public static boolean contains(int id) { return false; }
    public static int count() { return 0; }
    public static int count(String name) { return 0; }
}
