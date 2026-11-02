package me.marcronte.colisaocobblemon.features.blocks.machines.pokefurnace;

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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PokeFurnaceBlockEntity extends PokemonWorkBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<BlockPos> {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(28, ItemStack.EMPTY);

    private int smeltProgress = 0;
    private int smeltTotalTime = 200;
    private int productionCounter = 0;

    public PokeFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POKE_FURNACE_BE, pos, state);
    }

    @Override
    public boolean isValidPokemon(Pokemon pokemon) {
        String primary = pokemon.getPrimaryType().getName().toLowerCase();
        String secondary = pokemon.getSecondaryType() != null ? pokemon.getSecondaryType().getName().toLowerCase() : "";
        return primary.equals("fire") ||
                secondary.equals("fire");
    }

    @Override
    protected int calculateWorkTime(WorkPower power) {
        return switch (power) {
            case STRONG -> 200 / 6;
            case MEDIUM -> 200 / 3;
            case WEAK -> 200;
        };
    }

    @Override
    protected int getFriendshipConsumption() {
        return 1;
    }

    @Override
    protected boolean canWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        if (!isValidPokemon(pokemon)) return false;

        ItemStack inputStack = inventory.get(0);
        if (inputStack.isEmpty()) {
            if (smeltProgress > 0) smeltProgress = 0;
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) return false;

        var recipeHolder = serverLevel.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(inputStack), serverLevel)
                .orElse(null);

        if (recipeHolder == null) {
            if (smeltProgress > 0) smeltProgress = 0;
            return false;
        }

        ItemStack resultStack = recipeHolder.value().getResultItem(level.registryAccess());
        if (resultStack.isEmpty()) return false;

        return canInsertIntoOutput(resultStack);
    }

    @Override
    protected void performWork(Level level, BlockPos pos, Pokemon pokemon, WorkPower power) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ItemStack inputStack = inventory.get(0);
        var recipeHolder = serverLevel.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(inputStack), serverLevel)
                .orElse(null);

        if (recipeHolder == null) {
            smeltProgress = 0;
            return;
        }

        smeltTotalTime = calculateWorkTime(power);
        smeltProgress++;

        if (smeltProgress >= smeltTotalTime) {
            ItemStack baseResult = recipeHolder.value().getResultItem(level.registryAccess());

            int multiplier = getOreDuplicationMultiplier(serverLevel, power, baseResult);

            ItemStack finalResult = baseResult.copy();
            finalResult.setCount(baseResult.getCount() * multiplier);

            insertIntoOutput(finalResult);

            inputStack.shrink(1);
            if (inputStack.isEmpty()) {
                inventory.set(0, ItemStack.EMPTY);
            }

            smeltProgress = 0;

            productionCounter++;
            int requiredItemsToConsume = switch (power) {
                case STRONG -> 5;
                case MEDIUM -> 3;
                case WEAK -> 1;
            };

            if (productionCounter >= requiredItemsToConsume) {
                productionCounter = 0;
                int newFriendship = Math.max(0, getMachineFriendship() - getFriendshipConsumption());
                setMachineFriendship(newFriendship);
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos) {
        if (level.isClientSide() || pokemonData == null) return;

        Pokemon pokemon = BreedingNetwork.reconstructPokemon(pokemonData, level.registryAccess());
        if (pokemon == null) {
            updateLitState(level, pos, false);
            return;
        }

        if (getMachineFriendship() < getFriendshipConsumption()) {
            this.progressTicks = 0;
            updateLitState(level, pos, false);
            return;
        }

        WorkPower power = calculatePower(pokemon);

        if (canWork(level, pos, pokemon, power)) {
            updateLitState(level, pos, true);
            spawnActiveParticles(level, pos);

            performWork(level, pos, pokemon, power);
        } else {
            updateLitState(level, pos, false);
            if (this.smeltProgress > 0) {
                this.smeltProgress = 0;
                this.setChanged();
            }
        }
    }

    private int getOreDuplicationMultiplier(ServerLevel level, WorkPower power, ItemStack resultStack) {
        boolean isOreProduct = resultStack.is(net.minecraft.world.item.Items.IRON_INGOT) ||
                resultStack.is(net.minecraft.world.item.Items.GOLD_INGOT) ||
                resultStack.is(net.minecraft.world.item.Items.COPPER_INGOT) ||
                resultStack.is(net.minecraft.world.item.Items.DIAMOND) ||
                resultStack.is(net.minecraft.world.item.Items.EMERALD) ||
                resultStack.is(net.minecraft.world.item.Items.LAPIS_LAZULI) ||
                resultStack.is(net.minecraft.world.item.Items.REDSTONE) ||
                resultStack.is(net.minecraft.world.item.Items.COAL) ||
                resultStack.is(net.minecraft.world.item.Items.NETHERITE_SCRAP) ||
                resultStack.is(net.minecraft.world.item.Items.QUARTZ) ||
                resultStack.is(me.marcronte.colisaocobblemon.ModItems.ETERNATITE_SCRAP);

        if (!isOreProduct) return 1;

        return switch (power) {
            case STRONG -> 2; // 100% chance
            case MEDIUM -> level.random.nextFloat() < 0.40f ? 2 : 1; // 40% chance
            case WEAK -> level.random.nextFloat() < 0.10f ? 2 : 1; // 10% chance
        };
    }

    private void updateLitState(Level level, BlockPos pos, boolean lit) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.hasProperty(PokeFurnaceBlock.LIT) && currentState.getValue(PokeFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, currentState.setValue(PokeFurnaceBlock.LIT, lit), 3);
        }
    }

    private void spawnActiveParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (serverLevel.random.nextFloat() < 0.3F) {
            double x = (double) pos.getX() + 0.5D;
            double y = (double) pos.getY() + 0.5D;
            double z = (double) pos.getZ() + 0.5D;

            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y + 0.5D, z, 1,
                    0.1D, 0.1D, 0.1D, 0.02D
            );
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    x, y - 0.2D, z, 1,
                    0.1D, 0.1D, 0.1D, 0.02D
            );
        }
    }

    private boolean canInsertIntoOutput(ItemStack result) {
        for (int i = 1; i < inventory.size(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, result) && slot.getCount() + result.getCount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void insertIntoOutput(ItemStack result) {
        for (int i = 1; i < inventory.size(); i++) {
            ItemStack slot = inventory.get(i);
            if (slot.isEmpty()) {
                inventory.set(i, result.copy());
                return;
            } else if (ItemStack.isSameItemSameComponents(slot, result) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int amountToAdd = Math.min(space, result.getCount());
                slot.grow(amountToAdd);
                result.shrink(amountToAdd);
                if (result.isEmpty()) return;
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
                case 3 -> smeltProgress;
                case 4 -> smeltTotalTime;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 5; }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("SmeltProgress", this.smeltProgress);
        tag.putInt("ProductionCounter", this.productionCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.smeltProgress = tag.getInt("SmeltProgress");
        this.productionCounter = tag.getInt("ProductionCounter");
    }

    @Override
    public int getContainerSize() {
        return 28;
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
        if (side == Direction.DOWN) {
            int[] slots = new int[27];
            for (int i = 0; i < 27; i++) slots[i] = i + 1;
            return slots;
        } else if (side == Direction.UP || side.getAxis().isHorizontal()) {
            return new int[]{0};
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        if (index != 0) {
            return false;
        }
        if (direction == null || direction.getAxis().isHorizontal() || direction == Direction.UP) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index > 0;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.colisao-cobblemon.poke_furnace_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new PokeFurnaceMenu(syncId, playerInventory, this, this.dataAccess, this.getBlockPos());
    }
}