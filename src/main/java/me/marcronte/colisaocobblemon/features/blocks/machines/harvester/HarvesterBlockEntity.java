package me.marcronte.colisaocobblemon.features.blocks.machines.harvester;

import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.ModBlocks;
import me.marcronte.colisaocobblemon.features.blocks.machines.PokemonWorkBlockEntity;
import me.marcronte.colisaocobblemon.network.BreedingNetwork;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class HarvesterBlockEntity extends PokemonWorkBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<BlockPos> {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    private int harvestCounter = 0;
    private BlockPos targetPos = null;

    public HarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.HARVESTER_BE, pos, state);
    }

    @Override
    public boolean isValidPokemon(Pokemon pokemon) {
        String primary = pokemon.getPrimaryType().getName().toLowerCase();
        String secondary = pokemon.getSecondaryType() != null ? pokemon.getSecondaryType().getName().toLowerCase() : "";
        return primary.equals("grass") || primary.equals("bug") || secondary.equals("grass") || secondary.equals("bug");
    }

    @Override
    protected int calculateWorkTime(WorkPower power) {
        return switch (power) {
            case STRONG -> 30; // 1.5 seconds
            case MEDIUM -> 60; // 3 seconds
            case WEAK -> 100;  // 5 seconds
        };
    }

    @Override
    protected int getFriendshipConsumption() {
        return 1;
    }

    @Override
    protected boolean canWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        if (!isValidPokemon(pokemon) || isInventoryFull()) return false;

        int radius = switch (power) {
            case STRONG -> 3; // 7x7x7
            case MEDIUM -> 2; // 5x5x5
            case WEAK -> 1;   // 3x3x3
        };

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isReadyToHarvest(state)) {
                        this.targetPos = checkPos;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void performWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        if (targetPos == null || !(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(targetPos);
        Block block = state.getBlock();

        if (!isReadyToHarvest(state)) return;

        net.minecraft.resources.ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        String path = blockId.getPath();
        boolean isCobblemonPlant = blockId.getNamespace().equals("cobblemon") && (path.contains("berry") || path.contains("apricorn"));

        if (isCobblemonPlant) {
            String itemName = path.replace("_bush", "").replace("_tree", "").replace("_crop", "");
            net.minecraft.resources.ResourceLocation itemId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), itemName);
            net.minecraft.world.item.Item harvestItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);

            if (harvestItem != net.minecraft.world.item.Items.AIR) {
                int dropCount = path.contains("apricorn") ? 1 : 2;
                insertIntoInventory(new ItemStack(harvestItem, dropCount));
            }

            BlockState newState = state;
            for (Property<?> prop : state.getProperties()) {
                if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
                    int currentAge = state.getValue(intProp);
                    int minAge = Collections.min(intProp.getPossibleValues());
                    int resetAge = Math.max(minAge, currentAge - 2);
                    newState = newState.setValue(intProp, resetAge);
                }

                if (prop.getName().equals("berries") && prop instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp) {
                    newState = newState.setValue(boolProp, false);
                }
            }
            level.setBlock(targetPos, newState, 3);

        } else {
            List<ItemStack> drops = Block.getDrops(state, serverLevel, targetPos, level.getBlockEntity(targetPos));

            if (drops.isEmpty()) {
                net.minecraft.world.level.storage.loot.LootParams.Builder builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(serverLevel)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(targetPos))
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE));
                drops = state.getDrops(builder);
            }

            for (ItemStack drop : drops) {
                insertIntoInventory(drop);
            }

            for (Property<?> prop : state.getProperties()) {
                if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
                    int minAge = Collections.min(intProp.getPossibleValues());
                    level.setBlock(targetPos, state.setValue(intProp, minAge), 3);
                    break;
                }
            }
        }

        harvestCounter++;

        int requiredHarvestsToConsume = switch (power) {
            case STRONG -> 5;
            case MEDIUM -> 3;
            case WEAK -> 1;
        };

        if (harvestCounter >= requiredHarvestsToConsume) {
            harvestCounter = 0;
            int newFriendship = Math.max(0, getMachineFriendship() - getFriendshipConsumption());
            setMachineFriendship(newFriendship);
        }
    }


    private boolean isReadyToHarvest(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }

        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("berries") && prop instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProp) {
                if (state.getValue(boolProp)) return true;
            }

            if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
                int maxAge = Collections.max(intProp.getPossibleValues());
                int minAge = Collections.min(intProp.getPossibleValues());
                return state.getValue(intProp) == maxAge && maxAge > minAge;
            }
        }
        return false;
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

        if (!drop.isEmpty() && level != null) {
            Block.popResource(level, targetPos, drop);
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

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 3; }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("HarvestCounter", this.harvestCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.harvestCounter = tag.getInt("HarvestCounter");
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
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @org.jetbrains.annotations.Nullable Direction direction) {
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
        return Component.translatable("block.colisao-cobblemon.harvester_block");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new HarvesterMenu(syncId, playerInventory, this, this.dataAccess, this.getBlockPos());
    }
}