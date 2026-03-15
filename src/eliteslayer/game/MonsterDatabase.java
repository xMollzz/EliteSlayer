package eliteslayer.game;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of all 16 supported slayer monster definitions.
 */
public final class MonsterDatabase {

    private static final Map<String, MonsterDef> REGISTRY = new LinkedHashMap<>();

    static {
        register(new MonsterDef(
                "Abyssal Demons",
                new int[]{415},
                "Slayer Tower (Top Floor)",
                85, false, false, "", true,
                new int[]{3086, 3502, 0},
                new int[]{3418, 3563, 0}
        ));
        register(new MonsterDef(
                "Cave Kraken",
                new int[]{492},
                "Kraken Cove",
                87, false, false, "", false,
                new int[]{2278, 3311, 0},
                new int[]{2280, 10034, 0}
        ));
        register(new MonsterDef(
                "Gargoyles",
                new int[]{413},
                "Slayer Tower (Top Floor)",
                75, false, false, "", false,
                new int[]{3086, 3502, 0},
                new int[]{3440, 3542, 2}
        ));
        register(new MonsterDef(
                "Nechryael",
                new int[]{1613},
                "Slayer Tower (Top Floor)",
                80, false, false, "", false,
                new int[]{3086, 3502, 0},
                new int[]{3428, 3539, 2}
        ));
        register(new MonsterDef(
                "Bloodvelds",
                new int[]{1603},
                "Slayer Tower (Middle Floor)",
                50, true, false, "", true,
                new int[]{3086, 3502, 0},
                new int[]{3404, 3531, 1}
        ));
        register(new MonsterDef(
                "Turoth",
                new int[]{426},
                "Fremennik Slayer Dungeon",
                55, false, false, "", false,
                new int[]{2705, 3713, 0},
                new int[]{2715, 10003, 0}
        ));
        register(new MonsterDef(
                "Dust Devils",
                new int[]{1624},
                "Smoke Dungeon",
                65, true, false, "", true,
                new int[]{3086, 3502, 0},
                new int[]{3169, 9369, 0}
        ));
        register(new MonsterDef(
                "Black Demons",
                new int[]{172},
                "Taverley Dungeon",
                80, true, true, "PROTECT_FROM_MELEE", true,
                new int[]{2894, 3395, 0},
                new int[]{2879, 9825, 0}
        ));
        register(new MonsterDef(
                "Greater Demons",
                new int[]{370},
                "Karuulm Slayer Dungeon",
                0, true, true, "PROTECT_FROM_MELEE", true,
                new int[]{1311, 3789, 0},
                new int[]{1310, 10185, 0}
        ));
        register(new MonsterDef(
                "Fire Giants",
                new int[]{2453},
                "Waterfall Dungeon",
                0, true, false, "", true,
                new int[]{2512, 3649, 0},
                new int[]{2598, 9893, 0}
        ));
        register(new MonsterDef(
                "Hellhounds",
                new int[]{49},
                "Taverley Dungeon",
                0, false, false, "", true,
                new int[]{2894, 3395, 0},
                new int[]{2918, 9849, 0}
        ));
        register(new MonsterDef(
                "Wyrms",
                new int[]{8606},
                "Karuulm Slayer Dungeon",
                62, true, false, "", false,
                new int[]{1311, 3789, 0},
                new int[]{1290, 10201, 0}
        ));
        register(new MonsterDef(
                "Drakes",
                new int[]{8612},
                "Karuulm Slayer Dungeon",
                84, true, false, "", false,
                new int[]{1311, 3789, 0},
                new int[]{1272, 10216, 0}
        ));
        register(new MonsterDef(
                "Hydras",
                new int[]{8615},
                "Karuulm Slayer Dungeon",
                95, false, true, "PROTECT_FROM_MAGIC", false,
                new int[]{1311, 3789, 0},
                new int[]{1355, 10268, 0}
        ));
        register(new MonsterDef(
                "Kalphite Workers",
                new int[]{5305},
                "Kalphite Lair",
                0, true, false, "", true,
                new int[]{3289, 3166, 0},
                new int[]{3478, 9497, 0}
        ));
        register(new MonsterDef(
                "Trolls",
                new int[]{386},
                "Death Plateau",
                0, true, false, "", true,
                new int[]{2900, 3707, 0},
                new int[]{2892, 3672, 0}
        ));
    }

    private MonsterDatabase() {}

    private static void register(MonsterDef def) {
        REGISTRY.put(def.name, def);
    }

    public static MonsterDef get(String name) {
        return REGISTRY.get(name);
    }

    public static Collection<MonsterDef> all() {
        return REGISTRY.values();
    }

    public static String[] names() {
        return REGISTRY.keySet().toArray(new String[0]);
    }
}
