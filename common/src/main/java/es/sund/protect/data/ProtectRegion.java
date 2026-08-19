package es.sund.protect.data;

import es.sund.protect.flag.Flags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Region cuboide (siempre eje-alineada). Se guarda por coordenadas puras
 * -- no depende de que el chunk este cargado ni de ninguna entidad del
 * mundo, se puede crear/consultar aunque nadie haya visitado la zona.
 *
 * Convencion de flags, sin ambiguedad posible: el valor guardado ES
 * directamente "denied". true = regla activa, bloquea/impide la accion.
 * false = regla inactiva, la accion se permite. Flag no puesto = false
 * (permitido), igual de conservador que el default de siempre.
 */
public class ProtectRegion {

    public static final int DEFAULT_PRIORITY = 99;

    public String name;
    public String dimension; // ResourceKey<Level>.location().toString()
    public int minX, minY, minZ, maxX, maxY, maxZ;
    // 1 = maxima prioridad (se aplica primero si hay solape), 99 = minima
    // (valor por defecto de una region nueva, se aplica en ultimo lugar).
    public int priority = DEFAULT_PRIORITY;
    public Map<String, Boolean> flags = new HashMap<>();
    // Listas de nombres de comando (sin "/", sin argumentos -- solo la
    // palabra base, p.ej. "gamemode") para los flags ALLOWED_COMMANDS y
    // DENIED_COMMANDS. Vacias por defecto; se gestionan con
    // /sundprotect command <region> allow|deny add|remove <comando>.
    public List<String> allowedCommandsList = new ArrayList<>();
    public List<String> deniedCommandsList = new ArrayList<>();

    public ProtectRegion() {
    }

    public ProtectRegion(String name, ResourceKey<Level> dimension, BlockPos p1, BlockPos p2) {
        this.name = name;
        this.dimension = dimension.location().toString();
        this.minX = Math.min(p1.getX(), p2.getX());
        this.minY = Math.min(p1.getY(), p2.getY());
        this.minZ = Math.min(p1.getZ(), p2.getZ());
        this.maxX = Math.max(p1.getX(), p2.getX());
        this.maxY = Math.max(p1.getY(), p2.getY());
        this.maxZ = Math.max(p1.getZ(), p2.getZ());
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean overlaps(ProtectRegion other) {
        return dimension.equals(other.dimension)
                && minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public boolean contains(ResourceKey<Level> dim, BlockPos pos) {
        if (!this.dimension.equals(dim.location().toString())) {
            return false;
        }
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean isFlagDenied(String flag) {
        return Boolean.TRUE.equals(flags.get(flag));
    }

    /**
     * @param denied true = activa la regla (bloquea), false = la desactiva (permite)
     *
     * Cascada de un solo sentido: activar deny-all-spawn tambien activa
     * deny-mob-spawn y deny-animal-spawn (pedido explicito del usuario,
     * 2026-08-18) -- desactivar deny-all-spawn despues NO las toca, para
     * no deshacer un ajuste fino que alguien hubiera puesto a mano.
     */
    public void setFlag(String flag, boolean denied) {
        flags.put(flag, denied);
        if (Flags.ALL_SPAWN.equals(flag) && denied) {
            for (String cascaded : Flags.SPAWN_CASCADE) {
                flags.put(cascaded, true);
            }
        }
    }
}
