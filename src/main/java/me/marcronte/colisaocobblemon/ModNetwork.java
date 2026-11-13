package me.marcronte.colisaocobblemon;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.config.GeneralConfig;
import me.marcronte.colisaocobblemon.features.items.backpack.BackpackMenu;
import me.marcronte.colisaocobblemon.features.pokeloot.PokeLootNetwork;
import me.marcronte.colisaocobblemon.features.professions.CraftingManager;
import me.marcronte.colisaocobblemon.features.professions.PlantationManager;
import me.marcronte.colisaocobblemon.features.professions.StylistManager;
import me.marcronte.colisaocobblemon.features.routes.RouteNetwork;
import me.marcronte.colisaocobblemon.features.switchstate.SwitchNetwork;
import me.marcronte.colisaocobblemon.network.*;
import me.marcronte.colisaocobblemon.network.payloads.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;

public class ModNetwork {

    public static void register() {

        ClanPayloads.register();
        HabitatPayloads.registerC2S();

        // Quests
        PayloadTypeRegistry.playS2C().register(QuestBookPayload.ID, QuestBookPayload.CODEC);

        // Breeding
        PayloadTypeRegistry.playS2C().register(BreedingSyncPayload.ID, BreedingSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CastratedPartyPayload.ID, CastratedPartyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BreedingButtonPayload.ID, BreedingButtonPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BreedingSelectPayload.ID, BreedingSelectPayload.CODEC);

        // Professions (Plantation)
        PayloadTypeRegistry.playS2C().register(PlantationPayloads.SyncPayload.ID, PlantationPayloads.SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlantationPayloads.ActionPayload.ID, PlantationPayloads.ActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PlantationPayloads.ActionPayload.ID, (payload, context) ->
                context.server().execute(() -> PlantationManager.handleAction(context.player(), payload)));

        // Professions (Crafting)
        PayloadTypeRegistry.playS2C().register(ProfessionCraftPayloads.OpenMenuPayload.ID, ProfessionCraftPayloads.OpenMenuPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfessionCraftPayloads.PerformCraftPayload.ID, ProfessionCraftPayloads.PerformCraftPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProfessionCraftPayloads.SyncExpPayload.ID, ProfessionCraftPayloads.SyncExpPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ProfessionCraftPayloads.PerformCraftPayload.ID, (payload, context) ->
                context.server().execute(() -> CraftingManager.handleCraft(context.player(), payload)));

        // Professions (Stylist)
        PayloadTypeRegistry.playS2C().register(StylistPayloads.OpenMenuPayload.ID, StylistPayloads.OpenMenuPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StylistPayloads.SelectCategoryPayload.ID, StylistPayloads.SelectCategoryPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StylistPayloads.SelectCategoryPayload.ID, (payload, context) ->
                context.server().execute(() -> StylistManager.handleCategorySelect(context.player(), payload)));

        PayloadTypeRegistry.playS2C().register(StylistPayloads.OpenCraftPayload.ID, StylistPayloads.OpenCraftPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StylistPayloads.PerformApplyPayload.ID, StylistPayloads.PerformApplyPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StylistPayloads.PerformApplyPayload.ID, (payload, context) ->
                context.server().execute(() -> StylistManager.handleApply(context.player(), payload)));

        // Gen Limit
        PayloadTypeRegistry.playS2C().register(GenLimitPayload.ID, GenLimitPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            int limit = GeneralConfig.get().max_generation;
            ServerPlayNetworking.send(handler.getPlayer(), new GenLimitPayload(limit));
        });

        // Backpack
        PayloadTypeRegistry.playS2C().register(BackpackMenu.Payload.TYPE, BackpackMenu.Payload.CODEC);

        // Poke Lens
        PayloadTypeRegistry.playC2S().register(LensRequestPayload.ID, LensRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LensResponsePayload.ID, LensResponsePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(LensRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                Entity entity = context.player().serverLevel().getEntity(payload.entityId());

                if (entity instanceof PokemonEntity pokeEntity) {
                    Pokemon pokemon = pokeEntity.getPokemon();

                    String ability = pokemon.getAbility().getTemplate().getName();
                    String nature = pokemon.getNature().getName().getPath();

                    int hp = pokemon.getIvs().getOrDefault(Stats.HP);
                    int atk = pokemon.getIvs().getOrDefault(Stats.ATTACK);
                    int def = pokemon.getIvs().getOrDefault(Stats.DEFENCE);
                    int spa = pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK);
                    int spd = pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE);
                    int spe = pokemon.getIvs().getOrDefault(Stats.SPEED);

                    ServerPlayNetworking.send(context.player(), new LensResponsePayload(
                            payload.entityId(), ability, nature, hp, atk, def, spa, spd, spe
                    ));
                }
            });
        });

        PayloadTypeRegistry.playC2S().register(MachineActionPayload.ID, MachineActionPayload.CODEC);

        ClanNetwork.register();
        BadgeNetwork.register();
        PokeLootNetwork.register();
        BoostNetwork.registerCommon();
        RouteNetwork.register();
        BreedingNetwork.register();
        TeleportNetwork.registerCommon();
        TeleportNetwork.registerServerReceiver();
        GenLimitNetwork.registerCommon();
        SwitchNetwork.registerCommon();
        MachineNetwork.register();
    }
}