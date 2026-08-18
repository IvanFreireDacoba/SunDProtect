package es.sund.protect.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
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

    public String name;
    public String dimension; // ResourceKey<Level>.location().toString()
    public int minX, minY, minZ, maxX, maxY, maxZ;
    public Map<String, Boolean> flags = new HashMap<>();

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
     */
    public void setFlag(String flag, boolean denied) {
        flags.put(flag, denied);
    }
}
