package es.sund.protect.flag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reparte Flags.ALL_INFO en una rejilla de COLUMNS x ROWS para el menu de
 * /sundprotect flag: una columna por categoria (orden de primera
 * aparicion en ALL_INFO), los flags de esa categoria apilados en filas
 * dentro de su columna -- asi es facil ver de un vistazo que flags
 * pertenecen al mismo dominio (spawn, mecanismos, griefing...) y se
 * pueden chocar entre si. Logica pura sin ninguna clase de Minecraft,
 * compartida por los dos SundProtectMenu especificos de version
 * (1.20.1/1.21.1 solo divergen en como pintan cada item, no en donde va
 * cada uno).
 *
 * ROWS = 6 (chest grande, GENERIC_9x6) a proposito: la categoria SPAWN
 * puede llegar a 6 flags (mob/animal/all/custom + los dos de Cobblemon si
 * esta cargado), la mas numerosa de todas.
 */
public final class FlagGridLayout {

    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int SIZE = COLUMNS * ROWS;

    private final FlagInfo[] bySlot = new FlagInfo[SIZE];

    public FlagGridLayout() {
        Map<FlagInfo.Category, List<FlagInfo>> byCategory = new LinkedHashMap<>();
        for (FlagInfo info : Flags.ALL_INFO) {
            byCategory.computeIfAbsent(info.category(), c -> new ArrayList<>()).add(info);
        }
        int col = 0;
        for (List<FlagInfo> flags : byCategory.values()) {
            if (col >= COLUMNS) {
                break; // no deberia pasar con las categorias actuales, pero no revienta si se añaden mas de 9
            }
            for (int row = 0; row < flags.size() && row < ROWS; row++) {
                bySlot[row * COLUMNS + col] = flags.get(row);
            }
            col++;
        }
    }

    /**
     * El FlagInfo que ocupa ese slot del contenedor, o null si es un
     * hueco de relleno o el slot cae fuera del rango del contenedor
     * (p.ej. un slot del inventario del jugador).
     */
    public FlagInfo at(int slot) {
        return slot >= 0 && slot < SIZE ? bySlot[slot] : null;
    }
}
