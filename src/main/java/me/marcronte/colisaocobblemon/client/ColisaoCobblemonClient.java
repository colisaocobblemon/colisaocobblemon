package me.marcronte.colisaocobblemon.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import me.marcronte.colisaocobblemon.ModBlocks;
import me.marcronte.colisaocobblemon.network.payloads.*;
import me.marcronte.colisaocobblemon.ModItems;
import me.marcronte.colisaocobblemon.ModScreenHandlers;
import me.marcronte.colisaocobblemon.client.gui.*;
import me.marcronte.colisaocobblemon.features.eventblock.EventBlockRegistry;
import me.marcronte.colisaocobblemon.features.eventblock.PokemonBlockade;
import me.marcronte.colisaocobblemon.features.fadeblock.FadeBlock;
import me.marcronte.colisaocobblemon.features.fadeblock.FadeNetwork;
import me.marcronte.colisaocobblemon.features.hms.FlashItem;
import me.marcronte.colisaocobblemon.features.hms.HmManager;
import me.marcronte.colisaocobblemon.features.pokeloot.PokeLootRegistry;
import me.marcronte.colisaocobblemon.client.renderer.*;
import me.marcronte.colisaocobblemon.client.model.*;
import me.marcronte.colisaocobblemon.features.switchstate.SwitchNetwork;
import me.marcronte.colisaocobblemon.features.switchstate.SwitchStateRegistry;
import me.marcronte.colisaocobblemon.network.BoostNetwork;
import me.marcronte.colisaocobblemon.network.GenLimitNetwork;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColisaoCobblemonClient implements ClientModInitializer {

    public static boolean isPlayerBoosting = false;

    private static final Set<BlockPos> clientUnlockedBlocks = new HashSet<>();

    public static void updateUnlockedBlocks(List<BlockPos> positions) {
        clientUnlockedBlocks.addAll(positions);
    }

    private static RunningShoesModel<LivingEntity> runningShoesModel;

    @Override
    public void onInitializeClient() {
        // --- 1. Rendering ---

        // Cut
        BlockRenderLayerMap.INSTANCE.putBlock(HmManager.CUT_OBSTACLE, RenderType.cutoutMipped());

        // Rock Smash
        BlockRenderLayerMap.INSTANCE.putBlock(HmManager.ROCK_SMASH, RenderType.cutout());

        // --- 2. Colors (Cut only) ---
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            if (tintIndex == 1) {
                if (view == null || pos == null) return -1;
                return BiomeColors.getAverageFoliageColor(view, pos);
            }
            return -1;
        }, HmManager.CUT_OBSTACLE);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> -1, HmManager.CUT_OBSTACLE);

        // --- 3. GUI SCREENS ---

        MenuScreens.register(ModScreenHandlers.KANTO_BADGE_CASE_MENU, BadgeCaseScreen::new);
        MenuScreens.register(ModScreenHandlers.POKE_LOOT_MENU, PokeLootScreen::new);
        BlockRenderLayerMap.INSTANCE.putBlock(PokeLootRegistry.POKE_LOOT_BLOCK, RenderType.cutout());
        MenuScreens.register(ModScreenHandlers.FADE_BLOCK_MENU, FadeBlockScreen::new);
        MenuScreens.register(ModScreenHandlers.POKEMON_BLOCKADE_MENU, PokemonBlockadeScreen::new);
        MenuScreens.register(ModScreenHandlers.HABITAT_MENU, HabitatScreen::new);
        MenuScreens.register(ModScreenHandlers.BACKPACK_MENU, BackpackScreen::new);
        MenuScreens.register(ModScreenHandlers.POKE_LURER_MENU, PokeLurerScreen::new);
        MenuScreens.register(ModScreenHandlers.HARVESTER_MENU, HarvesterScreen::new);
        MenuScreens.register(ModScreenHandlers.POKE_MINER_MENU, PokeMinerScreen::new);
        MenuScreens.register(ModScreenHandlers.POKE_FURNACE_MENU, PokeFurnaceScreen::new);
        // --- 4. CLIENT REGISTERS ---

        BoostNetwork.registerClient();
        FadeNetwork.registerClient();


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) return;

            for (BlockPos pos : clientUnlockedBlocks) {
                if (client.level.isLoaded(pos)) {
                    BlockState currentState = client.level.getBlockState(pos);

                    if (currentState.getBlock() instanceof FadeBlock || currentState.getBlock() instanceof PokemonBlockade) {
                        client.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
                    }
                }
            }
        });

        BlockEntityRenderers.register(EventBlockRegistry.POKEMON_BLOCKADE_ENTITY, PokemonBlockadeRenderer::new);



        EntityModelLayerRegistry.registerModelLayer(RunningShoesModel.LAYER_LOCATION, RunningShoesModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteBootsModel.LAYER_LOCATION, EternatiteBootsModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteLeggingsModel.LAYER_LOCATION, EternatiteLeggingsModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteChestplateModel.LAYER_LOCATION, EternatiteChestplateModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteHelmetModel.LAYER_LOCATION, EternatiteHelmetModel::createBodyLayer);

        EntityModelLayerRegistry.registerModelLayer(EternatiteSwordModel.LAYER_LOCATION, EternatiteSwordModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteAxeModel.LAYER_LOCATION, EternatiteAxeModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatitePickaxeModel.LAYER_LOCATION, EternatitePickaxeModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteShovelModel.LAYER_LOCATION, EternatiteShovelModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(EternatiteHoeModel.LAYER_LOCATION, EternatiteHoeModel::createBodyLayer);

        ModArmorRenderers.registerAll();
        ModItemRenderers.registerAll();

        ItemProperties.register(ModItems.FLASH_HM, ResourceLocation.parse("colisao-cobblemon:active"),
                (stack, level, entity, seed) -> FlashItem.isActive(stack) ? 1.0F : 0.0F);


        ModelLoadingPlugin.register(context -> context.addModels(
                ResourceLocation.fromNamespaceAndPath("colisao-cobblemon", "block/switch_statue"),
                ResourceLocation.fromNamespaceAndPath("colisao-cobblemon", "block/switch_statue_active")
        ));

        BlockEntityRendererRegistry.register(
                SwitchStateRegistry.STATE_BLOCK_BE,
                StateBlockRenderer::new
        );

        BlockEntityRendererRegistry.register(
                SwitchStateRegistry.SWITCH_STATUE_BE,
                SwitchStatueRenderer::new
        );

        SwitchNetwork.registerClient();
        GenLimitNetwork.registerClient();

        ClientPlayNetworking.registerGlobalReceiver(GenLimitPayload.ID, (payload, context) -> context.client().execute(() -> ClientGenLimit.setMaxGeneration(payload.limit())));

        ClientPlayNetworking.registerGlobalReceiver(OpenRouteScreenPayload.ID, (payload, context) -> context.client().execute(() -> context.client().setScreen(new RouteConfigScreen())));

        ClientPlayNetworking.registerGlobalReceiver(BreedingSyncPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new BreedingScreen(
                payload.mother(),
                payload.father(),
                payload.motherSpecies(),
                payload.fatherSpecies(),
                payload.motherData(),
                payload.fatherData(),
                payload.startTime(),
                payload.totalDuration(),
                payload.active(),
                payload.ready()
        ))));


        ClientPlayNetworking.registerGlobalReceiver(ClanPayloads.OpenClanCreationPayload.ID, (payload, context) -> context.client().execute(() -> {
            Minecraft.getInstance().setScreen(new ClanCreationScreen());
        }));


        ClientPlayNetworking.registerGlobalReceiver(ClanPayloads.OpenClanMenuPayload.ID, (payload, context) -> context.client().execute(() -> {
            Minecraft.getInstance().setScreen(new ClanMenuScreen(
                    payload.clanName(),
                    payload.clanLevel(),
                    payload.clanXp(),
                    payload.clanXpNeeded(),
                    payload.members(),
                    payload.perks(),
                    payload.missions(),
                    payload.timeRemaining(),
                    payload.nextResetTimestamp(),
                    payload.isManager()
            ));
        }));

        ClientPlayNetworking.registerGlobalReceiver(QuestBookPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new QuestBookScreen(payload.quests()))));

        ClientPlayNetworking.registerGlobalReceiver(ProfessionCraftPayloads.OpenMenuPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new ProfessionCraftingUI(payload))));

        ClientPlayNetworking.registerGlobalReceiver(ProfessionCraftPayloads.SyncExpPayload.ID, (payload, context) -> context.client().execute(() -> {
            if (Minecraft.getInstance().screen instanceof ProfessionTradeScreen screen) {
                screen.updateExp(payload.newExp());
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(PlantationPayloads.SyncPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new PlantationScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(StylistPayloads.OpenMenuPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new StylistMenuScreen(payload))));

        ClientPlayNetworking.registerGlobalReceiver(StylistPayloads.OpenCraftPayload.ID, (payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new StylistCraftScreen(payload))));


        PokeLensOverlay.register();
        ItemProperties.register(ModItems.POKE_LENS, new ResourceLocation("colisao-cobblemon", "using"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POKE_LURER, RenderType.translucent());


    }

    public static void openTeleportScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new TeleportConfigScreen(pos));
    }

    public static void openTeleportIdScreen(net.minecraft.core.BlockPos pos, me.marcronte.colisaocobblemon.features.teleportblock.TeleportType type) {
        Minecraft.getInstance().setScreen(new me.marcronte.colisaocobblemon.client.gui.TeleportIdConfigScreen(pos, type));
    }
}