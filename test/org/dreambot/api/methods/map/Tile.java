package org.dreambot.api.methods.map;

public class Tile {
    private final int x, y, z;

    public Tile(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public double distance(Tile other) { return 0.0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tile)) return false;
        Tile t = (Tile) o;
        return x == t.x && y == t.y && z == t.z;
    }

    @Override
    public int hashCode() { return 31 * (31 * x + y) + z; }
}
