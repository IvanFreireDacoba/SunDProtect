package es.sund.protect.gui;

import es.sund.protect.compat.CobblemonSupport;
import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.SpawnExemptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menu de seleccion de entidades para el flag CUSTOM_SPAWN (click derecho
 * sobre ese flag en SundProtectMenu). Cofre de 6 filas: 45 huecos de
 * contenido (filas 0-4) con un item por tipo de entidad registrado en el
 * servidor -- vanilla y de mods, incluye Cobblemon si esta cargado salvo
 * el propio tipo "cobblemon:pokemon" (ver CobblemonSupport.
 * isPokemonEntityType, ya tiene sus propios flags dedicados) -- y una
 * fila de navegacion (pagina anterior/siguiente + volver). Nunca ofrece
 * los tipos de SpawnExemptions (items, orbes de experiencia, NPCs de
 * CustomNPCs): bloquearlos no haria nada, ver esa clase.
 *
 * Click en un item alterna si esa entidad esta en la lista negra de la
 * region (region.customSpawnList) -- verde = permitido, rojo = bloqueado.
 * El icono es el huevo de spawn real si el tipo tiene uno registrado
 * (vanilla y la mayoria de mods de mobs lo traen), o un spawner generico
 * si no.
 */
public class EntitySpawnPickerMenu extends ChestMenu {

    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private static final int SIZE = ROWS * COLUMNS;
    private static final int CONTENT_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private static final List<EntityType<?>> ENTITY_TYPES = computeEntityTypes();

    private final ProtectRegion region;
    private final Container display;
    private int page = 0;

    public EntitySpawnPickerMenu(int syncId, Inventory playerInventory, ProtectRegion region) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, new SimpleContainer(SIZE), ROWS);
        this.region = region;
        this.display = this.getContainer();
        renderPage();
    }

    private static List<EntityType<?>> computeEntityTypes() {
        List<EntityType<?>> list = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (SpawnExemptions.isExempt(type) || CobblemonSupport.isPokemonEntityType(type)) {
                continue;
            }
            list.add(type);
        }
        list.sort(Comparator.comparing(t -> EntityType.getKey(t).toString()));
        return list;
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(ENTITY_TYPES.size() / (double) CONTENT_SLOTS));
    }

    private void renderPage() {
        int from = page * CONTENT_SLOTS;
        for (int slot = 0; slot < CONTENT_SLOTS; slot++) {
            int index = from + slot;
            display.setItem(slot, index < ENTITY_TYPES.size() ? itemFor(ENTITY_TYPES.get(index)) : filler());
        }
        for (int slot = CONTENT_SLOTS; slot < SIZE; slot++) {
            display.setItem(slot, filler());
        }
        display.setItem(PREV_SLOT, page > 0 ? navItem(Items.ARROW, "§e<< Pagina anterior") : filler());
        display.setItem(NEXT_SLOT, page < totalPages() - 1 ? navItem(Items.ARROW, "§eSiguiente pagina >>") : filler());
        display.setItem(PAGE_INFO_SLOT, navItem(Items.PAPER, "§fPagina " + (page + 1) + "/" + totalPages()));
        display.setItem(BACK_SLOT, navItem(Items.BARRIER, "§cVolver a los flags"));
        this.broadcastChanges();
    }

    private static ItemStack navItem(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static ItemStack filler() {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(" "));
        return filler;
    }

    private boolean isBlocked(EntityType<?> type) {
        String key = EntityType.getKey(type).toString();
        return region.customSpawnList.stream().anyMatch(id -> id.equalsIgnoreCase(key));
    }

    private ItemStack itemFor(EntityType<?> type) {
        boolean blocked = isBlocked(type);
        SpawnEggItem egg = SpawnEggItem.byId(type);
        Item base = egg != null ? egg : Items.SPAWNER;
        ItemStack stack = new ItemStack(base);
        stack.setHoverName(Component.literal(blocked ? "§c" : "§a")
                .append(type.getDescription())
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7ID: " + EntityType.getKey(type)));
        lore.add(Component.literal(""));
        lore.add(Component.literal(blocked ? "§cBloqueado en esta region" : "§aPermitido en esta region"));
        lore.add(Component.literal("§8Click para alternar"));

        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        CompoundTag displayTag = stack.getOrCreateTagElement("display");
        displayTag.put("Lore", loreTag);
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == PREV_SLOT) {
            if (page > 0) {
                page--;
                renderPage();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (page < totalPages() - 1) {
                page++;
                renderPage();
            }
            return;
        }
        if (slotId == BACK_SLOT) {
            if (player instanceof ServerPlayer serverPlayer) {
                openFlagsMenu(serverPlayer);
            }
            return;
        }
        if (slotId < 0 || slotId >= CONTENT_SLOTS) {
            return;
        }
        int index = page * CONTENT_SLOTS + slotId;
        if (index >= ENTITY_TYPES.size()) {
            return;
        }
        String key = EntityType.getKey(ENTITY_TYPES.get(index)).toString();
        if (!region.customSpawnList.removeIf(id -> id.equalsIgnoreCase(key))) {
            region.customSpawnList.add(key);
        }
        RegionManager.save();
        renderPage();
    }

    private void openFlagsMenu(ServerPlayer player) {
        ProtectRegion region = this.region;
        MenuProvider provider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Flags: " + region.name);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new SundProtectMenu(syncId, inv, region);
            }
        };
        player.openMenu(provider);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
