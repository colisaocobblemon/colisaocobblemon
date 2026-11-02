package me.marcronte.colisaocobblemon.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CastrateCommand {
    private static final Map<UUID, ConfirmationData> pendingConfirmations = new HashMap<>();

    private record ConfirmationData(int slot, long timestamp) {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("castrar")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int slot = IntegerArgumentType.getInteger(context, "slot");
                            return execute(player, slot);
                        })));
    }

    private static int execute(ServerPlayer player, int slot) {
        int zeroIndexedSlot = slot - 1;
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon pokemon = party.get(zeroIndexedSlot);

        if (pokemon == null) {
            player.sendSystemMessage(Component.literal("Não há nenhum Pokémon no slot " + slot + "!").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (pokemon.getPersistentData().getBoolean("castrado")) {
            player.sendSystemMessage(Component.literal("O Pokémon " + pokemon.getSpecies().getName() + " já está castrado!").withStyle(ChatFormatting.RED));
            return 0;
        }

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (pendingConfirmations.containsKey(playerId)) {
            ConfirmationData data = pendingConfirmations.get(playerId);

            if (data.slot() == slot && (currentTime - data.timestamp() <= 30000)) {

                pokemon.getPersistentData().putBoolean("castrado", true);

                player.sendSystemMessage(Component.literal("O Pokémon " + pokemon.getSpecies().getName() + " foi castrado e não poderá mais reproduzir!").withStyle(ChatFormatting.GREEN));
                pendingConfirmations.remove(playerId);
                return 1;
            }
        }

        pendingConfirmations.put(playerId, new ConfirmationData(slot, currentTime));
        player.sendSystemMessage(Component.literal("Você tem certeza que deseja castrar o Pokémon " + pokemon.getSpecies().getName() + "?").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Essa ação é IRREVERSÍVEL. O Pokémon nunca mais poderá breedar.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("Para confirmar, digite /castrar " + slot + " novamente em até 30 segundos.").withStyle(ChatFormatting.GOLD));

        return 1;
    }
}
