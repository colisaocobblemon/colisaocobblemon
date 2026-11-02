package me.marcronte.colisaocobblemon.features.blocks.machines.pokeminer;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.ModBlocks;
import me.marcronte.colisaocobblemon.ModItems;
import me.marcronte.colisaocobblemon.features.blocks.machines.PokemonWorkBlockEntity;
import me.marcronte.colisaocobblemon.network.BreedingNetwork;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class PokeMinerBlockEntity extends PokemonWorkBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<BlockPos> {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    private int productionCounter = 0;
    private static final Random RANDOM = new Random();

    public PokeMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POKE_MINER_BE, pos, state);
    }

    @Override
    public boolean isValidPokemon(Pokemon pokemon) {
        String primary = pokemon.getPrimaryType().getName().toLowerCase();
        String secondary = pokemon.getSecondaryType() != null ? pokemon.getSecondaryType().getName().toLowerCase() : "";
        return primary.equals("rock") || primary.equals("ground") ||
                secondary.equals("rock") || secondary.equals("ground");
    }

    @Override
    protected int calculateWorkTime(WorkPower power) {
        return 600; // 1 item / second
    }

    @Override
    protected int getFriendshipConsumption() {
        return 1;
    }

    @Override
    protected boolean canWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        return isValidPokemon(pokemon) && !isInventoryFull();
    }

    @Override
    protected void performWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        if (isInventoryFull()) return;

        ItemStack minedItem = generateItemFromPool(power);
        if (!minedItem.isEmpty()) {
            insertIntoInventory(minedItem);
        }

        productionCounter++;
        int requiredItemsToConsume = switch (power) {
            case STRONG -> 10; // 1 friendship / 10 item
            case MEDIUM -> 6; // 1 friendship / 6 item
            case WEAK -> 2;  // 1 friendship / 2 item
        };

        if (productionCounter >= requiredItemsToConsume) {
            productionCounter = 0;
            int newFriendship = Math.max(0, getMachineFriendship() - getFriendshipConsumption());
            setMachineFriendship(newFriendship);
        }
    }

    public record WeightedItem(ItemStack itemStack, int weight) {
    }

    private ItemStack generateItemFromPool(WorkPower power) {
        List<WeightedItem> pool = switch (power) {
            case STRONG -> List.of(
                    new WeightedItem(new ItemStack(Items.OBSIDIAN, 1), 35),
                    new WeightedItem(new ItemStack(Items.RAW_GOLD, 1), 25), // Buff: 20 -> 25
                    new WeightedItem(new ItemStack(Items.DIAMOND, 1), 7),   // Buff: 5 -> 7
                    new WeightedItem(new ItemStack(Items.EMERALD, 1), 6),   // Buff: 4 -> 6
                    new WeightedItem(new ItemStack(Items.AMETHYST_SHARD, 1), 45), // Buff: 40 -> 45
                    new WeightedItem(new ItemStack(Items.ECHO_SHARD, 1), 8),  // Buff: 7 -> 8
                    new WeightedItem(new ItemStack(CobblemonItems.COVER_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.CLAW_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.ARMOR_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.DOME_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.FOSSILIZED_BIRD, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.FOSSILIZED_DINO, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.FOSSILIZED_DRAKE, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.FOSSILIZED_FISH, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.HELIX_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.JAW_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.OLD_AMBER_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.PLUME_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.ROOT_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.SAIL_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(CobblemonItems.SKULL_FOSSIL, 1), 1),
                    new WeightedItem(new ItemStack(Items.COBBLESTONE, 1), 30), // Nerf: 35 -> 30
                    new WeightedItem(new ItemStack(Items.GRAVEL, 1), 20),      // Nerf: 25 -> 20
                    new WeightedItem(new ItemStack(Items.DIRT, 1), 20),        // Nerf: 25 -> 20
                    new WeightedItem(new ItemStack(Items.COAL, 1), 25),        // Buff: 23 -> 25
                    new WeightedItem(new ItemStack(Items.RAW_COPPER, 1), 18),  // Buff: 15 -> 18
                    new WeightedItem(new ItemStack(Items.RAW_IRON, 1), 28),    // Buff: 25 -> 28
                    new WeightedItem(new ItemStack(Items.FLINT, 1), 8),
                    new WeightedItem(new ItemStack(CobblemonItems.WATER_GEM, 1), 3), // Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.FIRE_GEM, 1), 3),  // Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.DARK_GEM, 1), 3),  // Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.GHOST_GEM, 1), 3), // Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.STEEL_GEM, 1), 3), // Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.GROUND_GEM, 1), 3),// Buff: 2 -> 3
                    new WeightedItem(new ItemStack(CobblemonItems.DRAGON_GEM, 1), 2),// Buff: 1 -> 2
                    new WeightedItem(new ItemStack(CobblemonItems.POISON_GEM, 1), 3),// Buff: 2 -> 3
                    new WeightedItem(new ItemStack(ModItems.ETERNATITE_ORE_ITEM, 1), 2) // Buff: 1 -> 2
            );
            case MEDIUM -> List.of(
                    new WeightedItem(new ItemStack(Items.DEEPSLATE, 1), 40),   // Nerf: 45 -> 40
                    new WeightedItem(new ItemStack(Items.RAW_IRON, 1), 25),    // Buff: 20 -> 25
                    new WeightedItem(new ItemStack(Items.COAL, 1), 16),        // Buff: 12 -> 16
                    new WeightedItem(new ItemStack(Items.REDSTONE, 1), 12),    // Buff: 9 -> 12
                    new WeightedItem(new ItemStack(Items.LAPIS_LAZULI, 1), 10),// Buff: 8 -> 10
                    new WeightedItem(new ItemStack(Items.RAW_GOLD, 1), 7),     // Buff: 5 -> 7
                    new WeightedItem(new ItemStack(Items.DIAMOND, 1), 2),      // Buff: 1 -> 2
                    new WeightedItem(new ItemStack(Items.COBBLESTONE, 1), 48), // Nerf: 55 -> 48
                    new WeightedItem(new ItemStack(Items.GRAVEL, 1), 8),
                    new WeightedItem(new ItemStack(Items.DIRT, 1), 7),
                    new WeightedItem(new ItemStack(Items.RAW_COPPER, 1), 12),  // Buff: 10 -> 12
                    new WeightedItem(new ItemStack(Items.FLINT, 1), 12)
            );

            case WEAK -> List.of(
                    new WeightedItem(new ItemStack(Items.COBBLESTONE, 1), 45), // Nerf: 55 -> 45
                    new WeightedItem(new ItemStack(Items.GRAVEL, 1), 8),
                    new WeightedItem(new ItemStack(Items.DIRT, 1), 7),
                    new WeightedItem(new ItemStack(Items.COAL, 1), 18),        // Buff: 15 -> 18
                    new WeightedItem(new ItemStack(Items.RAW_COPPER, 1), 13),  // Buff: 10 -> 13
                    new WeightedItem(new ItemStack(Items.RAW_IRON, 1), 7),     // Buff: 4 -> 7
                    new WeightedItem(new ItemStack(Items.FLINT, 1), 1)
            );
        };

        if (pool.isEmpty()) return ItemStack.EMPTY;

        int totalWeight = 0;
        for (WeightedItem entry : pool) {
            totalWeight += entry.weight();
        }

        int randomValue = RANDOM.nextInt(totalWeight);
        int currentWeightSum = 0;

        for (WeightedItem entry : pool) {
            currentWeightSum += entry.weight();
            if (randomValue < currentWeightSum) {
                return entry.itemStack().copy();
            }
        }

        return pool.get(0).itemStack().copy();
    }

    private boolean isInventoryFull() {
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) return false;
        }
        return true;
    }

    private void insertIntoInventory(ItemStack drop) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) {
                inventory.set(i, drop.copy());
                drop.setCount(0);
                return;
            } else if (ItemStack.isSameItemSameComponents(slot, drop) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int amountToAdd = Math.min(space, drop.getCount());
                slot.grow(amountToAdd);
                drop.shrink(amountToAdd);
                if (drop.isEmpty()) return;
            }
        }
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (pokemonData == null) return 0;

            return switch (index) {
                case 0 -> getMachineFriendship();
                case 1 -> 1; // 1 = Has Pokemon
                case 2 -> {
                    if (level == null) yield 1;
                    Pokemon p = BreedingNetwork.reconstructPokemon(pokemonData, level.registryAccess());
                    WorkPower pwr = calculatePower(p);
                    yield pwr == WorkPower.STRONG ? 3 : (pwr == WorkPower.MEDIUM ? 2 : 1);
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("ProductionCounter", this.productionCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.productionCounter = tag.getInt("ProductionCounter");
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventory, slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction side) {
        int[] slots = new int[27];
        for (int i = 0; i < 27; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.colisao-cobblemon.poke_miner_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new PokeMinerMenu(syncId, playerInventory, this, this.dataAccess, this.getBlockPos());
    }
}