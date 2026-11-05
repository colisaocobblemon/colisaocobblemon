package me.marcronte.colisaocobblemon.features.breeding;

import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.ColisaoCobblemon;
import me.marcronte.colisaocobblemon.network.BreedingNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda, de forma persistente, os Pokemon que precisam voltar para um jogador que estava
 * offline quando o Breeding Habitat dele foi quebrado. Quando o jogador entra no servidor,
 * os Pokemon sao entregues na party (ou no PC se a party estiver cheia).
 *
 * Os dados ficam sempre no Overworld, independente da dimensao do bloco quebrado.
 */
public class PendingPokemonReturns extends SavedData {

    private static final String STORAGE_KEY = "ColisaoPendingPokemonReturns";

    private final Map<UUID, List<CompoundTag>> pending = new HashMap<>();

    public static PendingPokemonReturns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PendingPokemonReturns::new, PendingPokemonReturns::load, null),
                STORAGE_KEY
        );
    }

    public void add(UUID owner, CompoundTag pokemonData) {
        if (owner == null || pokemonData == null) return;
        pending.computeIfAbsent(owner, k -> new ArrayList<>()).add(pokemonData.copy());
        setDirty();
    }

    public boolean has(UUID owner) {
        List<CompoundTag> list = pending.get(owner);
        return list != null && !list.isEmpty();
    }

    /** Remove e devolve tudo que estava pendente para o jogador. */
    public List<CompoundTag> take(UUID owner) {
        List<CompoundTag> list = pending.remove(owner);
        if (list != null) setDirty();
        return list == null ? new ArrayList<>() : list;
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> deliver(handler.player, server)));
    }

    public static void deliver(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;

        PendingPokemonReturns store = get(server);
        UUID uuid = player.getUUID();
        if (!store.has(uuid)) return;

        int delivered = 0;
        for (CompoundTag data : store.take(uuid)) {
            Pokemon pokemon = BreedingNetwork.reconstructPokemon(data, player.registryAccess());
            if (pokemon == null) {
                // Nao perde o Pokemon: mantem guardado para tentar de novo na proxima entrada.
                store.add(uuid, data);
                ColisaoCobblemon.LOGGER.warn("Nao foi possivel devolver um Pokemon pendente de {}. Mantido guardado.", uuid);
                continue;
            }
            BreedingNetwork.givePokemonOrToPC(player, pokemon);
            delivered++;
        }

        if (delivered > 0) {
            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.habitat_pokemon_returned")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    public static PendingPokemonReturns load(CompoundTag tag, HolderLookup.Provider provider) {
        PendingPokemonReturns data = new PendingPokemonReturns();
        CompoundTag root = tag.getCompound("Pending");

        for (String key : root.getAllKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ListTag list = root.getList(key, Tag.TAG_COMPOUND);
            List<CompoundTag> entries = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                entries.add(list.getCompound(i));
            }
            if (!entries.isEmpty()) data.pending.put(uuid, entries);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();

        pending.forEach((uuid, entries) -> {
            if (entries.isEmpty()) return;
            ListTag list = new ListTag();
            for (CompoundTag entry : entries) list.add(entry.copy());
            root.put(uuid.toString(), list);
        });

        tag.put("Pending", root);
        return tag;
    }
}
