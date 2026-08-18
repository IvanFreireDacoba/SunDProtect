package es.sund.protect.flag;

import net.minecraft.world.item.Item;

/**
 * Metadatos de un flag para mostrarlo en el menu (/sundprotect flag <region>):
 * id interno, nombre corto para el titulo del item, descripcion para el
 * lore, si debe quedar Denied por defecto al crear una region nueva, y el
 * item vanilla que lo representa en el menu (zombie_head para spawn de
 * mobs, TNT para griefing...). Solo staff (permiso de operador) abre este
 * menu, y son items 100% vanilla -- no hace falta ningun resourcepack.
 */
public record FlagInfo(String id, String displayName, String description, boolean defaultDenied, Item icon) {
}
