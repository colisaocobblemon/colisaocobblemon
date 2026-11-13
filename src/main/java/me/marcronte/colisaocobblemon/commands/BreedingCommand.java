package me.marcronte.colisaocobblemon.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.marcronte.colisaocobblemon.config.GeneralConfig;
import me.marcronte.colisaocobblemon.features.breeding.BreedingData;
import me.marcronte.colisaocobblemon.network.BreedingNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BreedingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("breed")
                .executes(BreedingCommand::openGui)
                // /breed select <mother/father> <slot>
                .then(Commands.literal("select")
                        .then(Commands.argument("type", StringArgumentType.word()) // mother ou father
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                                        .executes(BreedingCommand::selectParent))))
                // /breed cancel
                .then(Commands.literal("cancel")
                        .executes(BreedingCommand::cancelBreeding))
        );
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        if (!GeneralConfig.get().breedingCommand) {
            ctx.getSource().sendFailure(Component.literal("§cO comando de breeding está desativado neste servidor."));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        BreedingNetwork.openBreedingScreen(player);
        return 1;
    }

    private static int cancelBreeding(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        boolean wasActive = BreedingNetwork.cancelBreeding(player);

        if (wasActive) {
            ctx.getSource().sendSuccess(() -> Component.translatable("message.colisao-cobblemon.breeding_canceled").withStyle(ChatFormatting.GREEN), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("message.colisao-cobblemon.breeding_canceled_error").withStyle(ChatFormatting.YELLOW), false);
        }

        return 1;
    }

    private static int selectParent(CommandContext<CommandSourceStack> ctx) {
        if (!GeneralConfig.get().breedingCommand) {
            ctx.getSource().sendFailure(Component.literal("§cO comando de breeding está desativado neste servidor."));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String type = StringArgumentType.getString(ctx, "type");
        int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;

        Pokemon pokemon;
        try {
            pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(slot);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.invalid_slot"));
            return 0;
        }

        if (pokemon == null) {
            ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.empty_slot"));
            return 0;
        }

        BreedingData data = BreedingData.get(player.serverLevel());
        BreedingData.PlayerBreedingInfo info = data.getPlayerInfo(player.getUUID());

        if (info.isActive) {
            ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.breeding_already").withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean isDitto = pokemon.getSpecies().getName().equalsIgnoreCase("ditto");
        boolean isUndiscovered = pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED);

        if (isUndiscovered && !isDitto) {
            ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.pokemon_cant_breed").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (type.equalsIgnoreCase("mother")) {
            if (pokemon.getGender() == Gender.MALE) {
                ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.female_slot"));
                return 0;
            }
            info.motherId = pokemon.getUuid();
            ctx.getSource().sendSuccess(() -> Component.translatable("message.colisao-cobblemon.female_defined", pokemon.getSpecies().getName()).withStyle(ChatFormatting.GREEN), false);
        }
        else if (type.equalsIgnoreCase("father")) {
            if (pokemon.getGender() == Gender.FEMALE) {
                ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.male_slot"));
                return 0;
            }
            info.fatherId = pokemon.getUuid();
            ctx.getSource().sendSuccess(() -> Component.translatable("message.colisao-cobblemon.male_defined", pokemon.getSpecies().getName()).withStyle(ChatFormatting.GREEN), false);
        } else {
            ctx.getSource().sendFailure(Component.translatable("message.colisao-cobblemon.breed_invalid_type"));
            return 0;
        }

        data.setDirty();
        BreedingNetwork.openBreedingScreen(player);

        return 1;
    }
}