package es.sund.protect.flag;

/**
 * Metadatos de un flag para mostrarlo en el menu (/sundprotect flag <region>):
 * id interno, nombre corto para el titulo del item, descripcion para el
 * lore, si debe quedar Denied por defecto al crear una region nueva, y el
 * indice fijo de icono (independiente del orden en Flags.ALL_INFO) que usa
 * el resourcepack para calcular el custom_model_data: 2000 + iconIndex*2
 * para el estado false, +1 para el estado true. Ese offset base y el
 * propio iconIndex de cada flag estan fijados aqui y en el generador del
 * resourcepack (icon_gen/generate_icons.py) -- si se anade un flag nuevo,
 * dale el siguiente iconIndex libre y nunca reutilices uno ya asignado,
 * o los packs ya distribuidos mostrarian el icono equivocado.
 */
public record FlagInfo(String id, String displayName, String description, boolean defaultDenied, int iconIndex) {
}
