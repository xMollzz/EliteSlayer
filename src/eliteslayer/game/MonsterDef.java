package eliteslayer.game;

/**
 * Immutable record for a monster definition.
 */
public final class MonsterDef {

    public final String name;
    public final int[] ids;
    public final String location;
    public final int slayerLevel;
    public final boolean usesCannon;
    public final boolean usesPrayer;
    public final String protection;   // e.g. "PROTECT_FROM_MELEE"
    public final boolean isMulti;
    public final int[] bankTile;      // {x, y, plane}
    public final int[] fightTile;     // {x, y, plane}

    public MonsterDef(String name, int[] ids, String location,
                      int slayerLevel, boolean usesCannon, boolean usesPrayer,
                      String protection, boolean isMulti,
                      int[] bankTile, int[] fightTile) {
        this.name = name;
        this.ids = ids;
        this.location = location;
        this.slayerLevel = slayerLevel;
        this.usesCannon = usesCannon;
        this.usesPrayer = usesPrayer;
        this.protection = protection;
        this.isMulti = isMulti;
        this.bankTile = bankTile;
        this.fightTile = fightTile;
    }

    @Override
    public String toString() {
        return name;
    }
}
