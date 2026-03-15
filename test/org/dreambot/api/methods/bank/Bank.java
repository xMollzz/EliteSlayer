package org.dreambot.api.methods.bank;

import java.util.function.Predicate;

public class Bank {
    public static boolean isOpen() { return false; }
    public static boolean open() { return false; }
    public static boolean close() { return false; }
    public static boolean contains(int id) { return false; }
    public static boolean contains(String name) { return false; }
    public static int count(int id) { return 0; }
    public static boolean withdraw(int id, int amount) { return false; }
    public static boolean withdraw(String name, int amount) { return false; }
    public static boolean depositAllExcept(Predicate<org.dreambot.api.wrappers.items.Item> filter) { return false; }
}
