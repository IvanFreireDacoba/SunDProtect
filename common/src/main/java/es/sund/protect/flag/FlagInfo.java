package es.sund.protect.flag;

/**
 * Metadatos de un flag para mostrarlo en el menu (/sundprotect flag <region>):
 * id interno, nombre corto para el titulo del item, descripcion para el
 * lore, y si debe quedar Denied por defecto al crear una region nueva.
 */
public record FlagInfo(String id, String displayName, String description, boolean defaultDenied) {
}
