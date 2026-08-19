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
    public static final String TRAPDOOR = "deny-use-trapdoor";
    public static final String ALLOWED_COMMANDS = "allowed-commands";
    public static final String DENIED_COMMANDS = "denied-commands";

    /**
     * Todos los flags van Denied por defecto al crear una region nueva
     * (pedido explicito del usuario, 2026-08-18) -- una region nueva
     * nace totalmente cerrada, hay que abrir a mano lo que se quiera
     * permitir con /sundprotect flag <region> (menu) o por comando.
     *
     * Unica excepcion: ALLOWED_COMMANDS nace desactivado (false). Es una
     * lista blanca vacia por defecto -- si naciera activado bloquearia
     * literalmente todos los comandos de la region hasta que alguien
     * rellenara la lista a mano, un default demasiado agresivo comparado
     * con el resto de flags (que solo restringen una accion concreta).
     * DENIED_COMMANDS (lista negra) si nace activado porque una lista
     * vacia con el flag activo no bloquea nada, es inofensivo.
     */
    public static final List<FlagInfo> ALL_INFO = List.of(
            new FlagInfo(MOB_SPAWN, "Spawn de mobs", "Bloquea el spawn natural de mobs hostiles (creeper, zombie, skeleton, spider...). No afecta a NPCs de CustomNPCs.", true),
            new FlagInfo(BREAK, "Romper bloques", "Nadie (salvo OP) puede romper bloques en la region.", true),
            new FlagInfo(PLACE, "Colocar bloques", "Nadie (salvo OP) puede colocar bloques ni cubos de liquido (verter o recoger) en la region.", true),
            new FlagInfo(MOBGRIEF, "Griefing de explosiones", "Ninguna explosion (creeper, TNT, carga de viento...) destruye bloques en la region.", true),
            new FlagInfo(PVP, "PVP entre jugadores", "Impide que los jugadores se hagan dano entre si dentro de la region.", true),
            new FlagInfo(ANIMAL_SPAWN, "Spawn de animales", "Bloquea el spawn natural de animales (vaca, cerdo, oveja, gallina...).", true),
            new FlagInfo(ALL_SPAWN, "Spawn de TODO", "Bloquea el spawn natural de cualquier entidad (mobs, animales, aldeanos...), salvo jugadores y NPCs de CustomNPCs. Al activarla tambien activa spawn de mobs y de animales automaticamente (no al reves: desactivarla despues no las toca).", true),
            new FlagInfo(USE, "Usar mecanismos", "Nadie (salvo OP) puede usar puertas, trampillas, verjas, palancas, botones ni placas de presion en la region.", true),
            new FlagInfo(CONTAINER, "Abrir contenedores", "Nadie (salvo OP) puede abrir cofres, barriles, hornos, shulkers ni otros contenedores en la region.", true),
            new FlagInfo(ITEM_DROP, "Tirar items", "Nadie (salvo OP) puede tirar items (tecla Q) estando dentro de la region.", true),
            new FlagInfo(ITEM_PICKUP, "Recoger items", "Nadie (salvo OP) puede recoger items del suelo dentro de la region.", true),
            new FlagInfo(LEASH, "Atar con correa", "Nadie (salvo OP) puede atar entidades con correa dentro de la region.", true),
            new FlagInfo(TRAPDOOR, "Abrir trapdoors a mano", "Nadie (salvo OP) puede abrir/cerrar trapdoors haciendo click directamente sobre ellas. No afecta a puertas, verjas, palancas, botones ni placas de presion (esos siguen bajo el flag 'Usar mecanismos'), y no bloquea que una trapdoor se accione por redstone o por un boton/palanca conectados.", true),
            new FlagInfo(ALLOWED_COMMANDS, "Solo comandos permitidos", "Lista blanca: si esta activo, nadie (salvo OP) puede ejecutar ningun comando salvo los añadidos con /sundprotect command <region> allow add <comando>. Con la lista vacia y el flag activo, NINGUN comando se puede usar -- por eso nace desactivado por defecto, a diferencia del resto de flags.", false),
            new FlagInfo(DENIED_COMMANDS, "Comandos bloqueados", "Lista negra: si esta activo, nadie (salvo OP) puede ejecutar los comandos añadidos con /sundprotect command <region> deny add <comando>; el resto de comandos se permite. Con la lista vacia no bloquea nada. Independiente de 'Solo comandos permitidos' -- si ambos flags estan activos a la vez se aplican los dos filtros.", true)
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
