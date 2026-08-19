package es.sund.protect.flag;

/**
 * Metadatos de un flag para mostrarlo en el menu (/sundprotect flag <region>):
 * id interno, nombre corto para el titulo del item, descripcion para el
 * lore, si debe quedar Denied por defecto al crear una region nueva, y la
 * categoria -- el menu agrupa los flags por categoria en columnas (ver
 * FlagGridLayout), una por dominio (spawn, mecanismos, griefing...) para
 * que sea facil ver de un vistazo que controla cada uno y detectar
 * flags que puedan chocar entre si dentro del mismo dominio.
 */
public record FlagInfo(String id, String displayName, String description, boolean defaultDenied, Category category) {

    public enum Category {
        SPAWN("Spawn de entidades"),
        MECHANISMS("Mecanismos y puertas"),
        GRIEFING("Construir / destruir / explosiones"),
        CONTAINERS("Contenedores"),
        ITEMS("Items"),
        COMBAT("Combate y entidades"),
        COMMANDS("Comandos");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
