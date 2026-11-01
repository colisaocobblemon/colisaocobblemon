package me.marcronte.colisaocobblemon.network;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.ModItems;
import me.marcronte.colisaocobblemon.features.breeding.BreedingCalculator;
import me.marcronte.colisaocobblemon.features.breeding.BreedingData;
import me.marcronte.colisaocobblemon.features.breeding.habitat.BreedingHabitatBlockEntity;
import me.marcronte.colisaocobblemon.network.payloads.BreedingButtonPayload;
import me.marcronte.colisaocobblemon.network.payloads.BreedingSelectPayload;
import me.marcronte.colisaocobblemon.network.payloads.BreedingSyncPayload;
import me.marcronte.colisaocobblemon.network.payloads.HabitatPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BreedingNetwork {
    public static final ResourceLocation SYNC_SCREEN = ResourceLocation.parse("colisao-cobblemon:breeding_sync");
    public static final ResourceLocation CLICK_BUTTON = ResourceLocation.parse("colisao-cobblemon:breeding_click");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BreedingButtonPayload.ID, (payload, context) ->
                context.server().execute(() -> handleButtonClick(context.player(), payload.buttonId())));

        ServerPlayNetworking.registerGlobalReceiver(BreedingSelectPayload.ID, (payload, context) ->
                context.server().execute(() -> handleSelection(context.player(), payload.slot(), payload.pokemonUuid())));

        ServerPlayNetworking.registerGlobalReceiver(HabitatPayloads.HabitatActionPayload.ID, (payload, context) ->
                context.server().execute(() -> handleHabitatAction(context.player(), payload.actionId(), payload.motherId(), payload.fatherId(), payload.berryId())));
    }

    public static void openBreedingScreen(ServerPlayer player) {
        BreedingData data = BreedingData.get(player.serverLevel());
        BreedingData.PlayerBreedingInfo info = data.getPlayerInfo(player.getUUID());

        String mSpecies = null;
        String fSpecies = null;

        if (info.isActive) {
            if (info.motherData != null) mSpecies = info.motherData.getString("UI_Species");
            if (info.fatherData != null) fSpecies = info.fatherData.getString("UI_Species");
        } else {
            Pokemon m = data.getPokemon(player, info.motherId);
            if (m != null) mSpecies = m.getSpecies().resourceIdentifier.toString();

            Pokemon f = data.getPokemon(player, info.fatherId);
            if (f != null) fSpecies = f.getSpecies().resourceIdentifier.toString();
        }

        ServerPlayNetworking.send(player, new BreedingSyncPayload(
                info.motherId,
                info.fatherId,
                mSpecies,
                fSpecies,
                info.startTime,
                BreedingData.PlayerBreedingInfo.BREEDING_DURATION,
                info.isActive,
                info.isReady()
        ));
    }


    private static void handleButtonClick(ServerPlayer player, int buttonId) {
        if (player == null) return;

        ServerLevel level = player.serverLevel();
        BreedingData data = BreedingData.get(level);
        BreedingData.PlayerBreedingInfo info = data.getPlayerInfo(player.getUUID());

        if (buttonId == 2) {
            if (info.isActive) return;

            Pokemon mother = data.getPokemon(player, info.motherId);
            Pokemon father = data.getPokemon(player, info.fatherId);

            if (mother == null || father == null) {
                player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_select_pokemon").withStyle(ChatFormatting.RED));
                return;
            }

            if (BreedingCalculator.createOffspring(mother, father) == null) {
                player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_incompatible").withStyle(ChatFormatting.RED));
                return;
            }

            info.motherData = savePokemonSecurely(mother, player);
            info.fatherData = savePokemonSecurely(father, player);

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            party.remove(mother);
            party.remove(father);

            info.isActive = true;
            info.startTime = System.currentTimeMillis();
            data.setDirty();

            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_started").withStyle(ChatFormatting.GREEN));
            openBreedingScreen(player);
        }
        else if (buttonId == 3) {
            if (!info.isReady()) return;

            Pokemon mother = reconstructPokemon(info.motherData, player.registryAccess());
            Pokemon father = reconstructPokemon(info.fatherData, player.registryAccess());

            if (mother != null && father != null) {
                Pokemon child = BreedingCalculator.createOffspring(mother, father);

                if (child != null) {
                    ItemStack eggStack = new ItemStack(ModItems.POKEMON_EGG);
                    CompoundTag tag = new CompoundTag();

                    CompoundTag pokemonData = child.saveToNBT(player.registryAccess(), new CompoundTag());

                    tag.put("PokemonData", pokemonData);
                    tag.putString("SpeciesIdentifier", child.getSpecies().resourceIdentifier.toString());
                    tag.putString("Form", child.getForm().getName());
                    tag.putString("NatureInternal", child.getNature().getName().getPath());
                    tag.putString("AbilityInternal", child.getAbility().getTemplate().getName());
                    tag.putBoolean("IsShiny", child.getShiny());
                    tag.putInt("IV_HP", child.getIvs().getOrDefault(Stats.HP));
                    tag.putInt("IV_ATK", child.getIvs().getOrDefault(Stats.ATTACK));
                    tag.putInt("IV_DEF", child.getIvs().getOrDefault(Stats.DEFENCE));
                    tag.putInt("IV_SPA", child.getIvs().getOrDefault(Stats.SPECIAL_ATTACK));
                    tag.putInt("IV_SPD", child.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE));
                    tag.putInt("IV_SPE", child.getIvs().getOrDefault(Stats.SPEED));

                    List<String> moveNames = new java.util.ArrayList<>();
                    for (Move move : child.getMoveSet().getMoves()) {
                        if (move != null) {
                            moveNames.add(move.getTemplate().getName().toLowerCase().replace(" ", "_"));
                        }
                    }
                    tag.putString("Moves", String.join(",", moveNames));

                    int cycles = child.getSpecies().getEggCycles();
                    if (cycles <= 0) cycles = 20;
                    int eggCycles = 100;
                    tag.putInt("RemainingSteps", cycles * eggCycles);
                    tag.putFloat("LastKnownWalkDist", player.walkDist);

                    CustomData.set(DataComponents.CUSTOM_DATA, eggStack, tag);

                    givePokemonOrToPC(player, mother);
                    givePokemonOrToPC(player, father);

                    if (!player.getInventory().add(eggStack)) {
                        player.drop(eggStack, false);
                    }

                    player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_concluded").withStyle(ChatFormatting.GOLD));

                    info.isActive = false;
                    info.startTime = 0;
                    info.motherId = null;
                    info.fatherId = null;
                    info.motherData = null;
                    info.fatherData = null;
                    data.setDirty();

                    ServerPlayNetworking.send(player, new BreedingSyncPayload(
                            null, null, null, null, 0, BreedingData.PlayerBreedingInfo.BREEDING_DURATION, false, false
                    ));
                }
            }
        }
    }

    public static void givePokemonOrToPC(ServerPlayer player, Pokemon pokemon) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (party.add(pokemon)) {
            player.sendSystemMessage(Component.translatable(pokemon.getSpecies().getName(), "message.colisao-cobblemon.pokemon_came_back").withStyle(ChatFormatting.GREEN));
        } else {
            Cobblemon.INSTANCE.getStorage().getPC(player).add(pokemon);
            player.sendSystemMessage(Component.translatable(pokemon.getSpecies().getName(), "message.colisao-cobblemon.pokemon_sent_pc").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void handleSelection(ServerPlayer player, int slot, UUID pokemonUuid) {
        if (player == null) return;
        BreedingData data = BreedingData.get(player.serverLevel());
        BreedingData.PlayerBreedingInfo info = data.getPlayerInfo(player.getUUID());
        if (info.isActive) return;
        if (slot == 0) info.motherId = pokemonUuid;
        else if (slot == 1) info.fatherId = pokemonUuid;
        data.setDirty();
        openBreedingScreen(player);
    }

    public static CompoundTag savePokemonSecurely(Pokemon pokemon, ServerPlayer player) {
        CompoundTag tag = pokemon.saveToNBT(player.registryAccess(), new CompoundTag());

        tag.putString("UI_Species", pokemon.getSpecies().resourceIdentifier.toString());
        tag.putString("Species", pokemon.getSpecies().resourceIdentifier.toString());
        return tag;
    }

    public static Pokemon reconstructPokemon(CompoundTag tag, net.minecraft.core.RegistryAccess registryAccess) {
        if (tag == null || tag.isEmpty()) return null;

        if (!tag.contains("UUID") && tag.contains("AspectsBackup")) {
            return reconstructLegacyPokemon(tag, registryAccess);
        }

        try {
            Pokemon p = com.cobblemon.mod.common.pokemon.Pokemon.Companion.loadFromNBT(registryAccess, tag);
            if (p != null) p.heal();
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Pokemon reconstructLegacyPokemon(CompoundTag tag, net.minecraft.core.RegistryAccess registryAccess) {
        String speciesStr = tag.getString("Species");
        if (speciesStr == null || speciesStr.isEmpty()) return null;
        if (speciesStr.startsWith("cobblemon:")) speciesStr = speciesStr.substring(10);

        ResourceLocation loc = ResourceLocation.parse(speciesStr.contains(":") ? speciesStr : "cobblemon:" + speciesStr);
        com.cobblemon.mod.common.pokemon.Species species = com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(loc);
        if (species == null) return null;

        StringBuilder dna = new StringBuilder(loc.getPath());

        String form = tag.getString("Form");
        if (form != null && !form.isEmpty() && !form.equalsIgnoreCase("normal")) dna.append(" form=").append(form.toLowerCase());

        String gender = tag.getString("Gender");
        if (gender != null && !gender.isEmpty()) dna.append(" gender=").append(gender.toLowerCase());

        if (tag.getBoolean("Shiny")) dna.append(" shiny=yes"); else dna.append(" shiny=no");

        String nature = tag.getString("Nature");
        if (nature != null && !nature.isEmpty()) dna.append(" nature=").append(nature.toLowerCase());

        String ability = tag.getString("Ability");
        if (ability != null && !ability.isEmpty()) dna.append(" ability=").append(ability.toLowerCase().replace(" ", "_"));

        if (tag.contains("AspectsBackup")) {
            ListTag aspects = (ListTag) tag.get("AspectsBackup");
            for (int i = 0; i < Objects.requireNonNull(aspects).size(); i++) {
                String aspect = aspects.getString(i).toLowerCase();
                if (!aspect.equalsIgnoreCase("normal") && (form == null || !aspect.equalsIgnoreCase(form)) && !aspect.equals("male") && !aspect.equals("female") && !aspect.equals("genderless") && !aspect.equals("shiny")) {
                    dna.append(" ").append(aspect);
                }
            }
        }

        if (tag.contains("MovesBackup")) {
            ListTag moves = (ListTag) tag.get("MovesBackup");
            List<String> moveNames = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                assert moves != null;
                if (i < moves.size()) {
                    String m = moves.getString(i);
                    if (!m.equals("EMPTY") && !m.isEmpty()) moveNames.add(m.toLowerCase().replace(" ", "_"));
                }
            }
            if (!moveNames.isEmpty()) dna.append(" moves=").append(String.join(",", moveNames));
        }

        Pokemon p = com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(dna.toString()).create();

        if (tag.contains("Level")) p.setLevel(tag.getInt("Level"));
        if (tag.contains("Experience")) {
            PokemonProperties.Companion.parse("xp=" + tag.getInt("Experience")).apply(p);
        }

        if (tag.contains("Friendship")) {
            PokemonProperties.Companion.parse("friendship=" + tag.getInt("Friendship")).apply(p);
        }

        if (tag.contains("IVsBackup")) {
            CompoundTag ivs = tag.getCompound("IVsBackup");
            p.getIvs().set(Stats.HP, ivs.getInt("HP"));
            p.getIvs().set(Stats.ATTACK, ivs.getInt("ATK"));
            p.getIvs().set(Stats.DEFENCE, ivs.getInt("DEF"));
            p.getIvs().set(Stats.SPECIAL_ATTACK, ivs.getInt("SPA"));
            p.getIvs().set(Stats.SPECIAL_DEFENCE, ivs.getInt("SPD"));
            p.getIvs().set(Stats.SPEED, ivs.getInt("SPE"));
        }

        if (tag.contains("EVsBackup")) {
            CompoundTag evs = tag.getCompound("EVsBackup");
            p.getEvs().set(Stats.HP, evs.getInt("HP"));
            p.getEvs().set(Stats.ATTACK, evs.getInt("ATK"));
            p.getEvs().set(Stats.DEFENCE, evs.getInt("DEF"));
            p.getEvs().set(Stats.SPECIAL_ATTACK, evs.getInt("SPA"));
            p.getEvs().set(Stats.SPECIAL_DEFENCE, evs.getInt("SPD"));
            p.getEvs().set(Stats.SPEED, evs.getInt("SPE"));
        }

        if (tag.contains("HeldItemBackup")) {
            net.minecraft.world.item.ItemStack heldItem = net.minecraft.world.item.ItemStack.parseOptional(registryAccess, tag.getCompound("HeldItemBackup"));
            if (heldItem != null && !heldItem.isEmpty()) {
                try { p.swapHeldItem(heldItem, true, false); } catch (Exception ignored) { }
            }
        }

        p.heal();
        return p;
    }

    private static void handleHabitatAction(ServerPlayer player, int actionId, UUID motherId, UUID fatherId, String berryId) {
        if (player == null) return;

        if (!(player.containerMenu instanceof me.marcronte.colisaocobblemon.features.breeding.habitat.HabitatMenu habitatMenu)) return;

        net.minecraft.world.level.block.entity.BlockEntity be = player.serverLevel().getBlockEntity(habitatMenu.blockPos);
        if (!(be instanceof me.marcronte.colisaocobblemon.features.breeding.habitat.BreedingHabitatBlockEntity habitat)) return;

        BreedingData data = BreedingData.get(player.serverLevel());

        if (actionId >= 10 && actionId <= 15) {
            handleBerryTransfer(player, habitat, actionId, berryId);
            return;
        }

        if (actionId == 1) {
            habitat.cancelBreedingAndReturnParents();
            player.closeContainer();
        }
        else if (actionId == 2) {
            Pokemon mother = data.getPokemon(player, motherId);
            Pokemon father = data.getPokemon(player, fatherId);

            if (mother == null || father == null) return;
            if (BreedingCalculator.createOffspring(mother, father) == null) {
                player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_incompatible").withStyle(ChatFormatting.RED));
                return;
            }

            CompoundTag mData = savePokemonSecurely(mother, player);
            CompoundTag fData = savePokemonSecurely(father, player);

            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            party.remove(mother);
            party.remove(father);

            habitat.startBreeding(player, mData, fData, mother.getPrimaryType().getName(), father.getPrimaryType().getName());

            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.breeding_started").withStyle(ChatFormatting.GREEN));
            player.closeContainer();
        }
        else if (actionId == 3) {
            habitat.collectEgg(player);
            player.closeContainer();
        }
    }

    private static void handleBerryTransfer(ServerPlayer player, BreedingHabitatBlockEntity habitat, int actionId, String berryId) {
        int slot = (actionId >= 10 && actionId <= 12) ? 0 : 1;

        boolean addOne = (actionId == 10 || actionId == 13 || actionId == 12 || actionId == 15);
        boolean removeOne = (actionId == 11 || actionId == 14);
        boolean isSelecting = (actionId == 12 || actionId == 15);

        ItemStack slotStack = habitat.getItem(slot);

        if (removeOne) {
            if (!slotStack.isEmpty()) {
                ItemStack returned = slotStack.split(1);
                if (!player.getInventory().add(returned)) player.drop(returned, false);
                habitat.setChanged();
            }
            return;
        }

        if (isSelecting && !slotStack.isEmpty() && !berryId.isEmpty()) {
            String currentSlotId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(slotStack.getItem()).toString();
            if (!currentSlotId.equals(berryId)) {
                if (!player.getInventory().add(slotStack.copy())) player.drop(slotStack.copy(), false);
                habitat.setItem(slot, ItemStack.EMPTY);
                slotStack = habitat.getItem(slot);
            }
        }

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack invStack = inv.getItem(i);

            if (!invStack.isEmpty() && invStack.getItem().toString().contains("berry")) {

                if (isSelecting && !berryId.isEmpty()) {
                    String currentItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(invStack.getItem()).toString();
                    if (!currentItemId.equals(berryId)) continue;
                }

                if (slotStack.isEmpty() || ItemStack.isSameItemSameComponents(slotStack, invStack)) {
                    if (addOne) {
                        if (slotStack.isEmpty()) {
                            habitat.setItem(slot, invStack.split(1));
                        } else if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                            slotStack.grow(1);
                            invStack.shrink(1);
                        }
                        habitat.setChanged();
                        return;
                    }
                }
            }
        }
    }
}