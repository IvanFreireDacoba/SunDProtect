package es.sund.protect.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Method;

/**
 * Soporte opcional para Cobblemon (mod id "cobblemon", confirmado leyendo
 * el fabric.mod.json real del jar 1.7.3+1.21.1) via reflexion pura, SIN
 * dependencia de compilacion -- SunDProtect compila y funciona igual de
 * bien en un servidor sin Cobblemon instalado (SunD Origins). Reflexion en
 * vez de una dependencia Gradle porque los metodos que se llaman aqui son
 * de Cobblemon (nunca obfuscados, un mod no remapea su propio codigo), asi
 * que son estables independientemente del esquema de mappings de este
 * proyecto -- solo hace falta el nombre exacto, verificado con javap
 * contra el jar real, no adivinado:
 * - PokemonEntity#getPokemon(): Pokemon
 * - Pokemon#getSpecies(): Species
 * - Species#getResourceIdentifier(): ResourceLocation (namespace
 *   "cobblemon" = especie oficial, cualquier otro namespace = especie
 *   custom añadida por un datapack, p.ej. el de CobbleSpain)
 *
 * Importante para la coherencia entre flags: PokemonEntity hereda de
 * ShoulderRidingEntity -> TamableAnimal -> Animal (verificado con javap),
 * asi que ES una instancia de Animal. Sin este carve-out, el flag
 * generico "Spawn de animales" (deny-animal-spawn) bloquearia tambien
 * todos los Pokemon por un detalle de implementacion de Cobblemon ajeno
 * a la intencion del admin -- NaturalSpawnMixin excluye explicitamente
 * los Pokemon del check de Animal para que solo los controlen
 * spawn-pokemons/spawn-custom-pokemons, nunca deny-animal-spawn.
 */
public final class CobblemonSupport {

    public static final String MOD_ID = "cobblemon";
    // Confirmado en assets/cobblemon/lang/en_us.json del jar real
    // ("entity.cobblemon.pokemon") y en CobblemonEntities (campo POKEMON_KEY).
    public static final String POKEMON_ENTITY_KEY = "cobblemon:pokemon";

    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

    private static final Class<?> POKEMON_ENTITY_CLASS =
            loadClass("com.cobblemon.mod.common.entity.pokemon.PokemonEntity");
    private static final Class<?> POKEMON_CLASS =
            loadClass("com.cobblemon.mod.common.pokemon.Pokemon");
    private static final Class<?> SPECIES_CLASS =
            loadClass("com.cobblemon.mod.common.pokemon.Species");

    private static final Method GET_POKEMON = loadMethod(POKEMON_ENTITY_CLASS, "getPokemon");
    private static final Method GET_SPECIES = loadMethod(POKEMON_CLASS, "getSpecies");
    private static final Method GET_RESOURCE_IDENTIFIER = loadMethod(SPECIES_CLASS, "getResourceIdentifier");

    public static boolean isLoaded() {
        return LOADED && POKEMON_ENTITY_CLASS != null;
    }

    public static boolean isPokemonEntity(Entity entity) {
        return isLoaded() && POKEMON_ENTITY_CLASS.isInstance(entity);
    }

    /**
     * true si type es cobblemon:pokemon -- se usa para EXCLUIR ese tipo del
     * menu de seleccion de entidades de CUSTOM_SPAWN cuando Cobblemon esta
     * cargado. Ya lo controlan spawn-pokemons/spawn-custom-pokemons con mas
     * detalle (oficial vs custom); dejarlo tambien seleccionable en la lista
     * generica solo crearia dos mecanismos distintos apuntando al mismo
     * tipo de entidad sin ninguna ganancia real.
     */
    public static boolean isPokemonEntityType(EntityType<?> type) {
        return isLoaded() && POKEMON_ENTITY_KEY.equals(EntityType.getKey(type).toString());
    }

    /**
     * true si es un Pokemon cuya especie NO es oficial de Cobblemon (namespace
     * del resourceIdentifier distinto de "cobblemon") -- un fakemon/especie
     * custom añadida por datapack. false tambien si no se pudo determinar
     * (mas conservador: no se trata como custom por defecto).
     */
    public static boolean isCustomSpecies(Entity entity) {
        if (!isPokemonEntity(entity) || GET_POKEMON == null || GET_SPECIES == null || GET_RESOURCE_IDENTIFIER == null) {
            return false;
        }
        try {
            Object pokemon = GET_POKEMON.invoke(entity);
            Object species = GET_SPECIES.invoke(pokemon);
            ResourceLocation id = (ResourceLocation) GET_RESOURCE_IDENTIFIER.invoke(species);
            return !MOD_ID.equals(id.getNamespace());
        } catch (ReflectiveOperationException | ClassCastException e) {
            return false;
        }
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method loadMethod(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private CobblemonSupport() {
    }
}
