package es.sund.protect.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regiones en memoria + persistencia en un JSON aparte dentro de la
 * carpeta del mundo (region/sundprotect_regions.json). No usa SavedData
 * de Minecraft a proposito -- SavedData esta atada al ciclo de carga del
 * mundo/dimension y puede complicarse con regiones que abarcan chunks
 * sin cargar; un JSON plano propio es mas simple y siempre disponible
 * independientemente de que chunk este cargado.
 */
public class RegionManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ProtectRegion> REGIONS = new ArrayList<>();
    private static Path savePath;

    public static void load(MinecraftServer server) {
        savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("sundprotect_regions.json");
        REGIONS.clear();
        if (Files.exists(savePath)) {
            try (Reader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<ProtectRegion>>() {
                }.getType();
                List<ProtectRegion> loaded = GSON.fromJson(reader, listType);
                if (loaded != null) {
                    REGIONS.addAll(loaded);
                }
            } catch (IOException e) {
                throw new RuntimeException("No se pudo leer sundprotect_regions.json", e);
            }
        }
    }

    public static void save() {
        if (savePath == null) {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
            GSON.toJson(REGIONS, writer);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar sundprotect_regions.json", e);
        }
    }

    public static List<ProtectRegion> all() {
        return REGIONS;
    }

    public static Optional<ProtectRegion> get(String name) {
        return REGIONS.stream().filter(r -> r.name.equalsIgnoreCase(name)).findFirst();
    }

    public static boolean exists(String name) {
        return get(name).isPresent();
    }

    public static void add(ProtectRegion region) {
        REGIONS.add(region);
        save();
    }

    public static boolean remove(String name) {
        boolean removed = REGIONS.removeIf(r -> r.name.equalsIgnoreCase(name));
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Region con mayor volumen entre las que contienen la posicion (si hay
     * varias solapadas, la mas pequena "gana" -- igual de intuitivo que
     * prioridad explicita para el alcance actual, sin necesidad de un
     * campo de prioridad manual).
     */
    public static Optional<ProtectRegion> findResponsibleRegion(ResourceKey<Level> dim, BlockPos pos) {
        ProtectRegion best = null;
        long bestVolume = Long.MAX_VALUE;
        for (ProtectRegion r : REGIONS) {
            if (r.contains(dim, pos)) {
                long volume = (long) (r.maxX - r.minX + 1) * (r.maxY - r.minY + 1) * (r.maxZ - r.minZ + 1);
                if (volume < bestVolume) {
                    bestVolume = volume;
                    best = r;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean isDenied(ResourceKey<Level> dim, BlockPos pos, String flag) {
        return findResponsibleRegion(dim, pos).map(r -> r.isFlagDenied(flag)).orElse(false);
    }
}
