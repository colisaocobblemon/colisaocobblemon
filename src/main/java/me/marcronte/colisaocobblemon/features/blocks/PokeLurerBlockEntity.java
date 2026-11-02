package me.marcronte.colisaocobblemon.features.blocks;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.marcronte.colisaocobblemon.ModBlocks;
import me.marcronte.colisaocobblemon.client.gui.PokeLurerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PokeLurerBlockEntity extends BlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<BlockPos> {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    private int fuelTicks = 0;
    private String currentLureType = "";

    public PokeLurerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POKE_LURER_BE, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (fuelTicks <= 0) {
            ItemStack slotItem = inventory.get(0);
            if (!slotItem.isEmpty()) {
                String lureType = getLureTypeFromItem(slotItem);

                if (lureType != null) {
                    this.currentLureType = lureType;
                    this.fuelTicks = 3000; // 2.5 Minutes
                    slotItem.shrink(1);
                    this.setChanged();
                }
            }
        }
        else {
            fuelTicks--;

            if (fuelTicks % 400 == 0) { // 1 Pokemon / 20 seconds
                spawnPokemonInArea(level, pos, currentLureType);
            }

            if (fuelTicks == 0) {
                currentLureType = "";
                this.setChanged();
            }
        }
    }

    private void spawnPokemonInArea(Level level, BlockPos center, String targetType) {
        AABB area = new AABB(center).inflate(8);

        long nearbyCount = level.getEntitiesOfClass(PokemonEntity.class, area).stream()
                .filter(p -> p.getPokemon().getOwnerUUID() == null)
                .count();

        if (nearbyCount >= 6) return;

        java.util.List<com.cobblemon.mod.common.pokemon.Species> validSpecies = new java.util.ArrayList<>();
        java.util.List<Integer> weights = new java.util.ArrayList<>();
        int totalWeight = 0;

        for (com.cobblemon.mod.common.pokemon.Species species : PokemonSpecies.getImplemented()) {

            boolean hasType = false;
            for (com.cobblemon.mod.common.api.types.ElementalType type : species.getTypes()) {
                if (type.getName().equalsIgnoreCase(targetType)) {
                    hasType = true;
                    break;
                }
            }
            if (!hasType) continue;

            java.util.Set<String> labels = species.getLabels();
            String rawPokemonName = species.getName().toLowerCase();

            String normalizedName = rawPokemonName.replace("-", "").replace("_", "").replace(" ", "");

            java.util.List<String> blockedPseudoLines = java.util.Arrays.asList(
                    "dratini", "dragonair", "dragonite",
                    "larvitar", "pupitar", "tyranitar",
                    "bagon", "shelgon", "salamence",
                    "beldum", "metang", "metagross",
                    "gible", "gabite", "garchomp",
                    "deino", "zweilous", "hydreigon",
                    "goomy", "sliggoo", "goodra",
                    "jangmoo", "hakamoo", "kommoo",
                    "dreepy", "drakloak", "dragapult",
                    "frigibax", "arctibax", "baxcalibur",
                    "dracovish", "arctozolt", "arctovish", "dracozolt"
            );

            if (labels.contains("legendary") || labels.contains("mythical") ||
                    labels.contains("powerhouse") || labels.contains("ultra_beast") ||
                    labels.contains("fossil") || labels.contains("paradox") || labels.contains("baby") || blockedPseudoLines.contains(normalizedName)) {
                continue;
            }

            int weight = LureRarityManager.getSpeciesWeight(rawPokemonName);

            validSpecies.add(species);
            weights.add(weight);
            totalWeight += weight;
        }

        if (validSpecies.isEmpty() || totalWeight <= 0) return;

        int randomValue = level.random.nextInt(totalWeight);
        com.cobblemon.mod.common.pokemon.Species chosen = validSpecies.getFirst();

        for (int i = 0; i < validSpecies.size(); i++) {
            randomValue -= weights.get(i);
            if (randomValue < 0) {
                chosen = validSpecies.get(i);
                break;
            }
        }

        int randomLevel = level.random.nextInt(21) + 10;
        com.cobblemon.mod.common.pokemon.Pokemon pokemon = chosen.create(randomLevel);

        net.minecraft.world.entity.EntityType<?> entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("cobblemon:pokemon"));

        if (entityType != null) {
            net.minecraft.world.entity.Entity entity = entityType.create(level);

            if (entity instanceof PokemonEntity pokeEntity) {
                pokeEntity.setPokemon(pokemon);

                for (int i = 0; i < 10; i++) {
                    double spawnX = center.getX() + 0.5 + (level.random.nextDouble() * 12 - 6);
                    double spawnZ = center.getZ() + 0.5 + (level.random.nextDouble() * 12 - 6);
                    double spawnY = center.getY();

                    BlockPos testPos = new BlockPos((int) spawnX, (int) spawnY, (int) spawnZ);

                    if (level.getBlockState(testPos).isAir() && level.getBlockState(testPos.below()).isFaceSturdy(level, testPos.below(), Direction.UP)) {

                        pokeEntity.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360F, 0);
                        pokeEntity.addTag("lure_spawned");

                        level.addFreshEntity(pokeEntity);
                        break;
                    }
                }
            }
        }
    }

    @Nullable
    private String getLureTypeFromItem(ItemStack stack) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        return switch (itemId) {
            case "colisao-cobblemon:occa_leaves" -> "fire";
            case "colisao-cobblemon:passho_leaves" -> "water";
            case "colisao-cobblemon:wacan_leaves" -> "electric";
            case "colisao-cobblemon:rindo_leaves" -> "grass";
            case "colisao-cobblemon:yache_leaves" -> "ice";
            case "colisao-cobblemon:kebia_leaves" -> "poison";
            case "colisao-cobblemon:shuca_leaves" -> "ground";
            case "colisao-cobblemon:chople_leaves" -> "fighting";
            case "colisao-cobblemon:coba_leaves" -> "flying";
            case "colisao-cobblemon:payapa_leaves" -> "psychic";
            case "colisao-cobblemon:tanga_leaves" -> "bug";
            case "colisao-cobblemon:charti_leaves" -> "rock";
            case "colisao-cobblemon:kasib_leaves" -> "ghost";
            case "colisao-cobblemon:haban_leaves" -> "dragon";
            case "colisao-cobblemon:colbur_leaves" -> "dark";
            case "colisao-cobblemon:babiri_leaves" -> "steel";
            case "colisao-cobblemon:chilan_leaves" -> "normal";
            case "colisao-cobblemon:roseli_leaves" -> "fairy";
            default -> null;
        };
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return inventory.get(0).isEmpty(); }
    @Override public @NotNull ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack s = ContainerHelper.removeItem(inventory, slot, amount);
        this.setChanged();
        return s;
    }
    @Override public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = ContainerHelper.takeItem(inventory, slot);
        this.setChanged();
        return s;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        this.setChanged();
    }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { inventory.clear(); this.setChanged(); }

    @Override public int @NotNull [] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return getLureTypeFromItem(stack) != null;
    }
    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return true; }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) return fuelTicks;
            if (index == 1) {
                return switch (currentLureType) {
                    case "fire" -> 1;
                    case "water" -> 2;
                    case "grass" -> 3;
                    case "electric" -> 4;
                    case "ice" -> 5;
                    case "fighting" -> 6;
                    case "poison" -> 7;
                    case "ground" -> 8;
                    case "flying" -> 9;
                    case "psychic" -> 10;
                    case "bug" -> 11;
                    case "rock" -> 12;
                    case "ghost" -> 13;
                    case "dragon" -> 14;
                    case "dark" -> 15;
                    case "steel" -> 16;
                    case "fairy" -> 17;
                    case "normal" -> 18;
                    default -> 0;
                };
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) fuelTicks = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) { return this.getBlockPos(); }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.colisao-cobblemon.poke_lurer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PokeLurerMenu(syncId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("FuelTicks", this.fuelTicks);
        tag.putString("LureType", this.currentLureType);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.fuelTicks = tag.getInt("FuelTicks");
        this.currentLureType = tag.getString("LureType");
    }
}