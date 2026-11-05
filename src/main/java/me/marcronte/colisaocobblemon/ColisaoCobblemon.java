package me.marcronte.colisaocobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.commands.*;
import me.marcronte.colisaocobblemon.features.RideRequirement;
import me.marcronte.colisaocobblemon.features.blocks.LureRarityManager;
import me.marcronte.colisaocobblemon.features.breeding.PendingPokemonReturns;
import me.marcronte.colisaocobblemon.features.breeding.habitat.BreedingEntityCleaner;
import me.marcronte.colisaocobblemon.placeholders.ModPlaceholders;
import me.marcronte.colisaocobblemon.config.ColisaoSettingsManager;
import me.marcronte.colisaocobblemon.features.CaptureRestrictionHandler;
import me.marcronte.colisaocobblemon.features.badges.*;
import me.marcronte.colisaocobblemon.features.boostpad.BoostPadBlock;
import me.marcronte.colisaocobblemon.features.boostpad.BoostPadHandler;
import me.marcronte.colisaocobblemon.features.clans.ClanMissionHandler;
import me.marcronte.colisaocobblemon.features.clans.ClanPerkHandler;
import me.marcronte.colisaocobblemon.features.clans.ClanScheduler;
import me.marcronte.colisaocobblemon.features.drops.PokemonDropModifier;
import me.marcronte.colisaocobblemon.features.elitefour.EliteFourHandler;
import me.marcronte.colisaocobblemon.features.eventblock.EventBlockRegistry;
import me.marcronte.colisaocobblemon.features.fadeblock.*;
import me.marcronte.colisaocobblemon.features.eventblock.EventBattleHandler;
import me.marcronte.colisaocobblemon.features.genlimit.GenerationCommand;
import me.marcronte.colisaocobblemon.features.genlimit.GenerationLimiter;
import me.marcronte.colisaocobblemon.features.npcs.NpcInteractionHandler;
import me.marcronte.colisaocobblemon.features.npcs.quest.QuestObjectiveRegistry;
import me.marcronte.colisaocobblemon.features.npcs.quest.QuestTrackerEvents;
import me.marcronte.colisaocobblemon.features.pokeloot.PokeLootRegistry;
import me.marcronte.colisaocobblemon.features.pokemondrop.PokemonCustomDropEvents;
import me.marcronte.colisaocobblemon.features.routes.RouteSpawner;
import me.marcronte.colisaocobblemon.features.routes.RouteTracker;
import me.marcronte.colisaocobblemon.features.switchstate.*;
import me.marcronte.colisaocobblemon.features.teleportblock.TeleportRegistry;
import net.fabricmc.api.ModInitializer;
import me.marcronte.colisaocobblemon.features.hms.HmManager;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColisaoCobblemon implements ModInitializer {
    public static final String MOD_ID = "colisao-cobblemon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static MinecraftServer serverInstance;
    public static final ResourceLocation GEN_LIMIT_PACKET_ID = ResourceLocation.fromNamespaceAndPath("colisao_cobblemon", "gen_limit");

    @Override
    public void onInitialize() {
        LOGGER.info("Inicializando Colisao Cobblemon...");

        // INITIALIZE MODULES
        ModItemGroup.register();

        // Badges & Level Cap
        BadgePickupEvents.register();
        BadgeInventoryCheck.register();

        LevelCapEvents.register();
        TrainerBattleEvents.register();

        // Features
        HmManager.register();
        ModScreenHandlers.register();
        BreedingEntityCleaner.register();
        PendingPokemonReturns.register();
        RideRequirement.register();

        // PokeLoot
        PokeLootRegistry.register();

        // Boost Pad
        BoostPadBlock.register();
        BoostPadHandler.register();

        // Fade Block
        FadeBlockRegistry.register();

        // Event Block
        EventBlockRegistry.register();
        EventBattleHandler.register();

        // Items
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();

        // State Block Mechanic
        SwitchStateRegistry.register();

        // Teleport Block
        TeleportRegistry.register();

        // Elite Four
        EliteFourHandler.register();

        // Gen Limit
        GenerationLimiter.register();

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GenerationCommand.register(dispatcher);
            ColisaoCommand.register(dispatcher);
            SpawnNpcCommand.register(dispatcher);
            BreedingCommand.register(dispatcher);
            LevelCapCommand.register(dispatcher);
            ProfessionCommand.register(dispatcher);
            ProfessorCommand.register(dispatcher);
            StylistCommand.register(dispatcher);
            BadgeCommand.register(dispatcher);
            EndBattleCommand.register(dispatcher);
            CastrateCommand.register(dispatcher);
        });

        ClanCommands.register();

        PokemonDropModifier.register();

        // NPCs
        NpcInteractionHandler.register();
        QuestObjectiveRegistry.register();
        QuestTrackerEvents.register();

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof NPCEntity && entity.isInvulnerable()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Route Mechanic

        RouteSpawner.register();
        RouteTracker.register();
        CaptureRestrictionHandler.register();

        // Clan
        ClanCommands.register();
        ClanPerkHandler.register();
        ClanMissionHandler.register();
        ClanScheduler.register();

        PokemonCustomDropEvents.register();

        ServerLifecycleEvents.SERVER_STARTING.register(ColisaoSettingsManager::init);

        // Network Payloads
        ModNetwork.register();

        ServerLifecycleEvents.SERVER_STARTED.register(LureRarityManager::initializeRarities);

        ServerLifecycleEvents.SERVER_STARTED.register(LureRarityManager::initializeRarities);

        String[] ores = {
                "eternatite_ore",
                "terastalite_ore"
        };

        for (String ore : ores) {
            BiomeModifications.addFeature(
                    BiomeSelectors.foundInOverworld(),
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(
                            Registries.PLACED_FEATURE,
                            ResourceLocation.fromNamespaceAndPath("colisao-cobblemon", ore)
                    )
            );
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var party = Cobblemon.INSTANCE.getStorage().getParty(handler.player);
            if (party != null) {
                for (int i = 0; i < party.size(); i++) {
                    Pokemon pokemon = party.get(i);
                    if (pokemon != null && pokemon.getPersistentData().contains("custom_stylist_skin")) {
                        String skinAspect = pokemon.getPersistentData().getString("custom_stylist_skin");
                        if (!pokemon.getAspects().contains(skinAspect)) {
                            pokemon.getAspects().add(skinAspect);
                        }
                    }
                }
            }
        });

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            Pokemon pokemon = event.getEntity().getPokemon();
            if (pokemon.getPersistentData().contains("custom_stylist_skin")) {
                String skinAspect = pokemon.getPersistentData().getString("custom_stylist_skin");
                if (!pokemon.getAspects().contains(skinAspect)) {
                    pokemon.getAspects().add(skinAspect);
                }
            }
            return null;
        });

        ModPlaceholders.register();

        // SERVER START CAPTURE
        ServerLifecycleEvents.SERVER_STARTED.register(server -> serverInstance = server);

        // CLEARS THE REFERENCE WHEN STOPPED (memory leak protection)
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> serverInstance = null);
    }

    public static MinecraftServer getServer() {
        return serverInstance;
    }
}