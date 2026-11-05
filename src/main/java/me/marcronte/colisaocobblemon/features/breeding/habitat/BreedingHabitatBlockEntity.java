package me.marcronte.colisaocobblemon.features.breeding.habitat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.ModBlocks;
import me.marcronte.colisaocobblemon.features.breeding.BreedingCalculator;
import me.marcronte.colisaocobblemon.features.breeding.PendingPokemonReturns;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BreedingHabitatBlockEntity extends BlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<BlockPos> {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    private UUID owner;

    private CompoundTag motherData;
    private CompoundTag fatherData;
    private String motherPrimaryType;
    private String fatherPrimaryType;

    private int habitatPoints = 0;
    private int requiredTicks = -1;
    private int progressTicks = 0;

    private int currentFuelTicks = 0;

    private UUID spawnedMotherId;
    private UUID spawnedFatherId;

    public BreedingHabitatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BREEDING_HABITAT_BE, pos, state);
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide) {
            despawnEntities((ServerLevel) this.level);
        }
        super.setRemoved();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || motherData == null || fatherData == null || state.getValue(BreedingHabitatBlock.READY)) return;

        if (level.getGameTime() % 20 == 0) {
            enforceLocks((ServerLevel) level);
        }

        if (level.getGameTime() % 100 == 0) {
            scanArea(level, pos);
            maintainEntities((ServerLevel) level, pos);
        }

        if (habitatPoints < 34) return;

        if (currentFuelTicks <= 0) {
            if (tryConsumeBerries()) {
                currentFuelTicks = 18000;
                this.setChanged();
            } else {
                return;
            }
        }

        currentFuelTicks--;
        progressTicks++;

        if (progressTicks % 100 == 0) {
            this.setChanged();
        }

        if (progressTicks >= requiredTicks) {
            completeBreeding(level, pos, state);
        }
    }

    private void enforceLocks(ServerLevel level) {
        if (spawnedMotherId != null) {
            Entity e = level.getEntity(spawnedMotherId);
            if (e instanceof PokemonEntity poke && !poke.getBusyLocks().contains("habitat_display")) {
                poke.getBusyLocks().add("habitat_display");
            }
        }
        if (spawnedFatherId != null) {
            Entity e = level.getEntity(spawnedFatherId);
            if (e instanceof PokemonEntity poke && !poke.getBusyLocks().contains("habitat_display")) {
                poke.getBusyLocks().add("habitat_display");
            }
        }
    }

    private void scanArea(Level level, BlockPos center) {
        int points = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int y = center.getY() - 1;
        for (int x = center.getX() - 3; x <= center.getX() + 3; x++) {
            for (int z = center.getZ() - 3; z <= center.getZ() + 3; z++) {
                mutable.set(x, y, z);
                Block block = level.getBlockState(mutable).getBlock();

                int valMother = HabitatDictionary.getBlockValueForTypes(block, motherPrimaryType, getSecondaryType(motherData));
                int valFather = HabitatDictionary.getBlockValueForTypes(block, fatherPrimaryType, getSecondaryType(fatherData));

                points += Math.max(valMother, valFather);
            }
        }

        this.habitatPoints = points;

        if (points < 34) this.requiredTicks = -1;
        else if (points <= 68) this.requiredTicks = 108000;
        else if (points <= 102) this.requiredTicks = 90000;
        else if (points <= 134) this.requiredTicks = 72000;
        else this.requiredTicks = 54000;
    }

    private boolean tryConsumeBerries() {
        String reqMother = HabitatDictionary.getRequiredBerryId(motherPrimaryType);
        String reqFather = HabitatDictionary.getRequiredBerryId(fatherPrimaryType);

        if (reqMother == null || reqFather == null) return false;

        int mSlot = -1, fSlot = -1;

        for (int i = 0; i < 2; i++) {
            if (HabitatDictionary.isBerryMatch(inventory.get(i), reqMother)) { mSlot = i; break; }
        }

        for (int i = 0; i < 2; i++) {
            if (HabitatDictionary.isBerryMatch(inventory.get(i), reqFather)) {
                if (i == mSlot && inventory.get(i).getCount() < 2) continue;
                fSlot = i; break;
            }
        }

        if (mSlot != -1 && fSlot != -1) {
            inventory.get(mSlot).shrink(1);
            inventory.get(fSlot).shrink(1);
            this.setChanged();
            return true;
        }
        return false;
    }

    private void completeBreeding(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BreedingHabitatBlock.READY, true), 3);
        despawnEntities((ServerLevel) level);
        this.setChanged();
    }

    public void collectEgg(ServerPlayer player) {
        if (!player.getUUID().equals(owner)) {
            player.sendSystemMessage(Component.literal("§cApenas o dono pode coletar este ovo."));
            return;
        }

        Pokemon mother = BreedingNetwork.reconstructPokemon(motherData, player.registryAccess());
        Pokemon father = BreedingNetwork.reconstructPokemon(fatherData, player.registryAccess());

        if (mother != null && father != null) {
            Pokemon child = BreedingCalculator.createOffspring(mother, father);
            if (child != null) {

                ItemStack eggStack = new ItemStack(me.marcronte.colisaocobblemon.ModItems.POKEMON_EGG);
                CompoundTag tag = new CompoundTag();
                CompoundTag pokemonData = new CompoundTag();
                child.saveToNBT(player.registryAccess(), pokemonData);
                tag.put("PokemonData", pokemonData);
                tag.putString("SpeciesIdentifier", child.getSpecies().resourceIdentifier.toString());

                String formName = child.getForm().getName();
                tag.putString("Form", formName != null ? formName : "normal");

                tag.putString("NatureInternal", child.getNature().getName().getPath());
                tag.putString("AbilityInternal", child.getAbility().getTemplate().getName());
                tag.putBoolean("IsShiny", child.getShiny());

                tag.putInt("IV_HP", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP));
                tag.putInt("IV_ATK", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK));
                tag.putInt("IV_DEF", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE));
                tag.putInt("IV_SPA", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK));
                tag.putInt("IV_SPD", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE));
                tag.putInt("IV_SPE", child.getIvs().getOrDefault(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED));

                java.util.List<String> moveNames = new java.util.ArrayList<>();
                for (com.cobblemon.mod.common.api.moves.Move move : child.getMoveSet().getMoves()) {
                    if (move != null) {
                        moveNames.add(move.getTemplate().getName().toLowerCase().replace(" ", "_"));
                    }
                }
                tag.putString("Moves", String.join(",", moveNames));

                int cycles = child.getSpecies().getEggCycles();
                if (cycles <= 0) cycles = 20;
                tag.putInt("RemainingSteps", cycles * 100);
                tag.putFloat("LastKnownWalkDist", player.walkDist);

                net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, eggStack, tag);

                if (!player.getInventory().add(eggStack)) {
                    player.drop(eggStack, false);
                }

                player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_concluded").withStyle(net.minecraft.ChatFormatting.GOLD));

                BreedingNetwork.givePokemonOrToPC(player, mother);
                BreedingNetwork.givePokemonOrToPC(player, father);

                this.motherData = null;
                this.fatherData = null;
                this.progressTicks = 0;
                this.currentFuelTicks = 0;
                this.spawnedMotherId = null;
                this.spawnedFatherId = null;
                this.setChanged();

                level.setBlock(getBlockPos(), getBlockState().setValue(BreedingHabitatBlock.READY, false), 3);
            }
        }
    }

    public void cancelBreedingAndReturnParents() {
        if (level == null || level.isClientSide || owner == null) return;

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);

        returnParentToOwner(player, motherData);
        returnParentToOwner(player, fatherData);

        despawnEntities((ServerLevel) level);

        this.motherData = null;
        this.fatherData = null;
        this.progressTicks = 0;
        this.currentFuelTicks = 0;
        this.setChanged();
    }

    /**
     * Dono online: o Pokemon vai direto para a party (ou PC se estiver cheia).
     * Dono offline (ou se algo falhar ao reconstruir): os dados completos ficam guardados e
     * o Pokemon e entregue quando o dono entrar no servidor. Nunca dropa item no chao.
     */
    private void returnParentToOwner(ServerPlayer player, CompoundTag data) {
        if (data == null) return;

        if (player != null) {
            Pokemon pokemon = BreedingNetwork.reconstructPokemon(data, level.registryAccess());
            if (pokemon != null) {
                BreedingNetwork.givePokemonOrToPC(player, pokemon);
                return;
            }
        }

        PendingPokemonReturns.get(level.getServer()).add(owner, data);
    }

    private void maintainEntities(ServerLevel level, BlockPos pos) {
        if (this.motherData == null || this.fatherData == null || this.getBlockState().getValue(BreedingHabitatBlock.READY)) {
            despawnEntities(level);
            return;
        }

        String ownerTag = "habitat_owner_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();

        maintainSingleEntity(level, pos, this.spawnedMotherId, this.motherData, ownerTag, true);
        maintainSingleEntity(level, pos, this.spawnedFatherId, this.fatherData, ownerTag, false);
    }

    private void maintainSingleEntity(ServerLevel level, BlockPos pos, UUID currentEntityId, CompoundTag data, String ownerTag, boolean isMother) {
        Pokemon pokemon = BreedingNetwork.reconstructPokemon(data, level.registryAccess());
        if (pokemon == null) return;

        if (currentEntityId != null) {
            Entity e = level.getEntity(currentEntityId);
            if (e instanceof PokemonEntity poke && poke.isAlive()) {
                tetherEntity(poke, pos);
                return;
            }
        }

        net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("cobblemon:pokemon"));
        if (type != null) {
            Entity entity = type.create(level);
            if (entity instanceof PokemonEntity pokeEntity) {
                com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("uncatchable=true").apply(pokemon);
                pokeEntity.setPokemon(pokemon);

                double spawnX = pos.getX() + 0.5 + (level.random.nextDouble() * 4 - 2);
                double spawnY = pos.getY() + 1.0;
                double spawnZ = pos.getZ() + 0.5 + (level.random.nextDouble() * 4 - 2);

                pokeEntity.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360F, 0);

                pokeEntity.setPersistenceRequired();
                pokeEntity.getBusyLocks().add("habitat_display");

                pokeEntity.addTag("habitat_display_entity");
                pokeEntity.addTag(ownerTag);

                if (isMother) {
                    this.spawnedMotherId = pokeEntity.getUUID();
                } else {
                    this.spawnedFatherId = pokeEntity.getUUID();
                }

                level.addFreshEntity(pokeEntity);
                this.setChanged();
            }
        }
    }

    private void tetherEntity(PokemonEntity poke, BlockPos pos) {
        if (poke.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 36) {
            poke.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        }
    }

    private void despawnEntities(ServerLevel level) {
        if (spawnedMotherId != null) {
            Entity em = level.getEntity(spawnedMotherId);
            if (em instanceof PokemonEntity) em.discard();
            spawnedMotherId = null;
            this.setChanged();
        }

        if (spawnedFatherId != null) {
            Entity ef = level.getEntity(spawnedFatherId);
            if (ef instanceof PokemonEntity) ef.discard();
            spawnedFatherId = null;
            this.setChanged();
        }

    }

    public void startBreeding(ServerPlayer player, CompoundTag mData, CompoundTag fData, String mType, String fType) {
        this.owner = player.getUUID();
        this.motherData = mData;
        this.fatherData = fData;
        this.motherPrimaryType = mType;
        this.fatherPrimaryType = fType;
        this.progressTicks = 0;
        this.currentFuelTicks = 0;
        this.setChanged();
    }

    private String getSecondaryType(CompoundTag data) {
        if (this.level == null || data == null) return null;

        Pokemon pokemon = BreedingNetwork.reconstructPokemon(data, this.level.registryAccess());

        if (pokemon != null && pokemon.getSecondaryType() != null) {
            return pokemon.getSecondaryType().getName();
        }

        return null;
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progressTicks / 20;
                case 1 -> requiredTicks == -1 ? -1 : requiredTicks / 20;
                case 2 -> currentFuelTicks / 20;
                case 3 -> habitatPoints;
                case 4 -> (motherData != null) ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 5; }
    };

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.colisao-cobblemon.breeding_habitat");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HabitatMenu(syncId, playerInventory, this, this.dataAccess, this.getBlockPos());
    }

    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public @NotNull ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(inventory, slot, amount);
        this.setChanged();
        return stack;
    }
    @Override public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(inventory, slot);
        this.setChanged();
        return stack;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        this.setChanged();
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    @Override public void clearContent() {
        inventory.clear();
        this.setChanged();
    }
    @Override
    public int @NotNull [] getSlotsForFace(Direction side) {
        if (side == Direction.WEST) return new int[]{0};
        if (side == Direction.EAST) return new int[]{1};
        return new int[]{0, 1};
    }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @org.jetbrains.annotations.Nullable Direction direction) {
        return itemStack.getItem().toString().contains("berry");
    }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        if (this.owner != null) tag.putUUID("Owner", this.owner);
        if (this.motherData != null) tag.put("MotherData", this.motherData);
        if (this.fatherData != null) tag.put("FatherData", this.fatherData);
        if (this.motherPrimaryType != null) tag.putString("MotherType", this.motherPrimaryType);
        if (this.fatherPrimaryType != null) tag.putString("FatherType", this.fatherPrimaryType);

        if (this.spawnedMotherId != null) tag.putUUID("SpawnedMother", this.spawnedMotherId);
        if (this.spawnedFatherId != null) tag.putUUID("SpawnedFather", this.spawnedFatherId);

        tag.putInt("ProgressTicks", this.progressTicks);
        tag.putInt("FuelTicks", this.currentFuelTicks);
        tag.putInt("HabitatPoints", this.habitatPoints);
        tag.putInt("RequiredTicks", this.requiredTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        if (tag.hasUUID("Owner")) this.owner = tag.getUUID("Owner");
        if (tag.contains("MotherData")) this.motherData = tag.getCompound("MotherData");
        if (tag.contains("FatherData")) this.fatherData = tag.getCompound("FatherData");
        if (tag.contains("MotherType")) this.motherPrimaryType = tag.getString("MotherType");
        if (tag.contains("FatherType")) this.fatherPrimaryType = tag.getString("FatherType");

        if (tag.hasUUID("SpawnedMother")) this.spawnedMotherId = tag.getUUID("SpawnedMother");
        if (tag.hasUUID("SpawnedFather")) this.spawnedFatherId = tag.getUUID("SpawnedFather");

        this.progressTicks = tag.getInt("ProgressTicks");
        this.currentFuelTicks = tag.getInt("FuelTicks");
        if (tag.contains("HabitatPoints")) this.habitatPoints = tag.getInt("HabitatPoints");
        if (tag.contains("RequiredTicks")) this.requiredTicks = tag.getInt("RequiredTicks");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public UUID getSpawnedMotherId() {
        return spawnedMotherId;
    }

    public UUID getSpawnedFatherId() {
        return spawnedFatherId;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public boolean hasActiveBreeding() {
        return this.motherData != null || this.fatherData != null;
    }
}