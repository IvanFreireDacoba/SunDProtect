package es.sund.protect.flag;

import es.sund.protect.compat.CobblemonSupport;
import es.sund.protect.flag.FlagInfo.Category;

import java.util.ArrayList;
import java.util.List;

public final class Flags {
    public static final String MOB_SPAWN = "deny-mob-spawn";
    public static final String ANIMAL_SPAWN = "deny-animal-spawn";
    public static final String ALL_SPAWN = "deny-all-spawn";
    public static final String CUSTOM_SPAWN = "custom-spawn";
    public static final String SPAWN_POKEMONS = "spawn-pokemons";
    public static final String SPAWN_CUSTOM_POKEMONS = "spawn-custom-pokemons";
    public static final String USE = "deny-use";
    public static final String TRAPDOOR = "deny-use-trapdoor";
    public static final String BREAK = "deny-break";
    public static final String PLACE = "deny-place";
    public static final String MOBGRIEF = "deny-mobgrief";
    public static final String CONTAINER = "deny-container";
    public static final String ITEM_DROP = "deny-item-drop";
    public static final String ITEM_PICKUP = "deny-item-pickup";
    public static final String PVP = "deny-pvp";
    public static final String LEASH = "deny-leash";
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
     * DENIED_COMMANDS y CUSTOM_SPAWN (listas negras) si nacen activados
     * porque una lista vacia con el flag activo no bloquea nada, es
     * inofensivo.
     *
     * Orden deliberado: los flags del mismo Category quedan adyacentes
     * aqui para que sea facil leer el fichero por dominio -- pero el
     * agrupamiento real en el menu (columnas) lo decide el campo
     * category de cada uno, no la posicion en esta lista (ver
     * FlagGridLayout).
     *
     * spawn-pokemons/spawn-custom-pokemons solo se añaden a la lista si
     * Cobblemon esta cargado en ESTE servidor (ver CobblemonSupport) --
     * en SunD Origins (sin Cobblemon) ni existen, ni aparecen en el menu
     * ni se pueden usar con /sundprotect flag. PokemonEntity hereda de
     * Animal (ver CobblemonSupport), asi que NaturalSpawnMixin excluye
     * los Pokemon del check generico de deny-animal-spawn -- solo estos
     * dos flags nuevos los controlan, para que activar "Spawn de
     * animales" no bloquee Pokemon por sorpresa (y viceversa).
     */
    public static final List<FlagInfo> ALL_INFO = buildAllInfo();

    private static List<FlagInfo> buildAllInfo() {
        List<FlagInfo> list = new ArrayList<>(List.of(
            new FlagInfo(MOB_SPAWN, "Spawn de mobs", "Bloquea el spawn natural de mobs hostiles (creeper, zombie, skeleton, spider...). No afecta a NPCs de CustomNPCs.", true, Category.SPAWN),
            new FlagInfo(ANIMAL_SPAWN, "Spawn de animales", "Bloquea el spawn natural de animales (vaca, cerdo, oveja, gallina...). No afecta a Pokemon de Cobblemon aunque tecnicamente tambien son Animal -- esos los controlan spawn-pokemons/spawn-custom-pokemons por separado, para que este flag no bloquee Pokemon por sorpresa.", true, Category.SPAWN),
            new FlagInfo(ALL_SPAWN, "Spawn de TODO", "Bloquea el spawn natural de cualquier entidad (mobs, animales, aldeanos, Pokemon si Cobblemon esta instalado...), salvo jugadores, items sueltos, orbes de experiencia y NPCs de CustomNPCs -- esos nunca los bloquea ningun flag de spawn. Al activarla tambien activa spawn de mobs y de animales automaticamente (no al reves: desactivarla despues no las toca).", true, Category.SPAWN),
            new FlagInfo(CUSTOM_SPAWN, "Spawn de entidades concretas", "Lista negra de tipos de entidad (vanilla o de mods): las que esten en la lista no aparecen por spawn natural en la region, el resto no se ve afectado por este flag. Con la lista vacia no bloquea nada. Click izquierdo activa/desactiva el flag, click derecho abre el menu para elegir entidades. Igual que el resto de flags de spawn, nunca incluye jugadores, items sueltos, orbes de experiencia ni NPCs de CustomNPCs -- no aparecen como opcion en el menu porque bloquearlos no haria nada.", true, Category.SPAWN)
        ));
        if (CobblemonSupport.isLoaded()) {
            list.add(new FlagInfo(SPAWN_POKEMONS, "Spawn de Pokemon", "Bloquea el spawn natural de Pokemon de especie oficial de Cobblemon. No afecta a especies custom (fakemon de datapack) -- esas las controla spawn-custom-pokemons por separado. Independiente de deny-animal-spawn (ver esa descripcion).", true, Category.SPAWN));
            list.add(new FlagInfo(SPAWN_CUSTOM_POKEMONS, "Spawn de Pokemon custom", "Bloquea el spawn natural de Pokemon de especie custom (fakemon añadidos por datapack, namespace de la especie distinto de 'cobblemon'). No afecta a especies oficiales -- esas las controla spawn-pokemons por separado.", true, Category.SPAWN));
        }
        list.add(new FlagInfo(USE, "Usar mecanismos", "Nadie (salvo OP) puede usar puertas, verjas, palancas, botones ni placas de presion en la region.", true, Category.MECHANISMS));
        list.add(new FlagInfo(TRAPDOOR, "Abrir trapdoors a mano", "Nadie (salvo OP) puede abrir/cerrar trapdoors haciendo click directamente sobre ellas. No afecta a puertas, verjas, palancas, botones ni placas de presion (esos siguen bajo el flag 'Usar mecanismos'), y no bloquea que una trapdoor se accione por redstone o por un boton/palanca conectados.", true, Category.MECHANISMS));
        list.add(new FlagInfo(BREAK, "Romper bloques", "Nadie (salvo OP) puede romper bloques en la region.", true, Category.GRIEFING));
        list.add(new FlagInfo(PLACE, "Colocar bloques", "Nadie (salvo OP) puede colocar bloques ni cubos de liquido (verter o recoger) en la region.", true, Category.GRIEFING));
        list.add(new FlagInfo(MOBGRIEF, "Griefing de explosiones", "Ninguna explosion (creeper, TNT, carga de viento...) destruye bloques en la region.", true, Category.GRIEFING));
        list.add(new FlagInfo(CONTAINER, "Abrir contenedores", "Nadie (salvo OP) puede abrir cofres, barriles, hornos, shulkers ni otros contenedores en la region.", true, Category.CONTAINERS));
        list.add(new FlagInfo(ITEM_DROP, "Tirar items", "Nadie (salvo OP) puede tirar items (tecla Q) estando dentro de la region.", true, Category.ITEMS));
        list.add(new FlagInfo(ITEM_PICKUP, "Recoger items", "Nadie (salvo OP) puede recoger items del suelo dentro de la region.", true, Category.ITEMS));
        list.add(new FlagInfo(PVP, "PVP entre jugadores", "Impide que los jugadores se hagan dano entre si dentro de la region.", true, Category.COMBAT));
        list.add(new FlagInfo(LEASH, "Atar con correa", "Nadie (salvo OP) puede atar entidades con correa dentro de la region.", true, Category.COMBAT));
        list.add(new FlagInfo(ALLOWED_COMMANDS, "Solo comandos permitidos", "Lista blanca: si esta activo, nadie (salvo OP) puede ejecutar ningun comando salvo los añadidos con /sundprotect command <region> allow add <comando>. Con la lista vacia y el flag activo, NINGUN comando se puede usar -- por eso nace desactivado por defecto, a diferencia del resto de flags. Un comando no puede estar a la vez en esta lista y en la de bloqueados (denied-commands), el mod lo rechaza al intentar añadirlo.", false, Category.COMMANDS));
        list.add(new FlagInfo(DENIED_COMMANDS, "Comandos bloqueados", "Lista negra: si esta activo, nadie (salvo OP) puede ejecutar los comandos añadidos con /sundprotect command <region> deny add <comando>; el resto de comandos se permite. Con la lista vacia no bloquea nada. Independiente de 'Solo comandos permitidos' -- si ambos flags estan activos a la vez se aplican los dos filtros. Un comando no puede estar a la vez en esta lista y en la de permitidos (allowed-commands), el mod lo rechaza al intentar añadirlo.", true, Category.COMMANDS));
        return List.copyOf(list);
    }

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
