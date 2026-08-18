package es.sund.protect.flag;

import java.util.List;

public final class Flags {
    public static final String MOB_SPAWN = "deny-mob-spawn";
    public static final String BREAK = "deny-break";
    public static final String PLACE = "deny-place";
    public static final String MOBGRIEF = "deny-mobgrief";
    public static final String PVP = "deny-pvp";
    public static final String ANIMAL_SPAWN = "deny-animal-spawn";
    public static final String ALL_SPAWN = "deny-all-spawn";
    public static final String USE = "deny-use";
    public static final String CONTAINER = "deny-container";
    public static final String ITEM_DROP = "deny-item-drop";
    public static final String ITEM_PICKUP = "deny-item-pickup";
    public static final String LEASH = "deny-leash";

    /**
     * Todos los flags van Denied por defecto al crear una region nueva
     * (pedido explicito del usuario, 2026-08-18) -- una region nueva
     * nace totalmente cerrada, hay que abrir a mano lo que se quiera
     * permitir con /sundprotect flag <region> (menu) o por comando.
     *
     * El ultimo numero de cada FlagInfo es su iconIndex (ver javadoc de
     * FlagInfo) -- fijo para siempre una vez publicado un icono, no
     * reordenar ni reutilizar.
     */
    public static final List<FlagInfo> ALL_INFO = List.of(
            new FlagInfo(MOB_SPAWN, "Spawn de mobs", "Bloquea el spawn natural de mobs hostiles (creeper, zombie, skeleton, spider...). No afecta a NPCs de CustomNPCs.", true, 0),
            new FlagInfo(BREAK, "Romper bloques", "Nadie (salvo OP) puede romper bloques en la region.", true, 1),
            new FlagInfo(PLACE, "Colocar bloques", "Nadie (salvo OP) puede colocar bloques ni cubos de liquido (verter o recoger) en la region.", true, 2),
            new FlagInfo(MOBGRIEF, "Griefing de explosiones", "Ninguna explosion (creeper, TNT, carga de viento...) destruye bloques en la region.", true, 3),
            new FlagInfo(PVP, "PVP entre jugadores", "Impide que los jugadores se hagan dano entre si dentro de la region.", true, 4),
            new FlagInfo(ANIMAL_SPAWN, "Spawn de animales", "Bloquea el spawn natural de animales (vaca, cerdo, oveja, gallina...).", true, 5),
            new FlagInfo(ALL_SPAWN, "Spawn de TODO", "Bloquea el spawn natural de cualquier entidad (mobs, animales, aldeanos...), salvo jugadores y NPCs de CustomNPCs. Al activarla tambien activa spawn de mobs y de animales automaticamente (no al reves: desactivarla despues no las toca).", true, 6),
            new FlagInfo(USE, "Usar mecanismos", "Nadie (salvo OP) puede usar puertas, trampillas, verjas, palancas, botones ni placas de presion en la region.", true, 7),
            new FlagInfo(CONTAINER, "Abrir contenedores", "Nadie (salvo OP) puede abrir cofres, barriles, hornos, shulkers ni otros contenedores en la region.", true, 8),
            new FlagInfo(ITEM_DROP, "Tirar items", "Nadie (salvo OP) puede tirar items (tecla Q) estando dentro de la region.", true, 9),
            new FlagInfo(ITEM_PICKUP, "Recoger items", "Nadie (salvo OP) puede recoger items del suelo dentro de la region.", true, 10),
            new FlagInfo(LEASH, "Atar con correa", "Nadie (salvo OP) puede atar entidades con correa dentro de la region.", true, 11)
    );

    public static final List<String> ALL = ALL_INFO.stream().map(FlagInfo::id).toList();

    /**
     * Flags de spawn que se activan en cascada cuando ALL_SPAWN pasa a
     * true (ver ProtectRegion.setFlag) -- una sola direccion, activar
     * ALL_SPAWN fuerza estas dos a true, pero desactivar ALL_SPAWN
     * despues no las toca.
     */
    public static final List<String> SPAWN_CASCADE = List.of(MOB_SPAWN, ANIMAL_SPAWN);

    public static FlagInfo info(String id) {
        return ALL_INFO.stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
    }

    private Flags() {
    }
}
