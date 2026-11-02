package me.marcronte.colisaocobblemon.features.clans;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class ClanPerkHandler {

    public static void register() {

        // 1. XP Bonus
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_POST.subscribe(Priority.NORMAL, (ExperienceGainedEvent.Post event) -> {
            Pokemon pokemon = event.getPokemon();

            if (event.getSource() instanceof com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource sidemodSource) {
                if (sidemodSource.getSidemodId().equals("colisao-cobblemon")) {
                    return Unit.INSTANCE;
                }
            }

            if (pokemon.getOwnerPlayer() instanceof ServerPlayer player) {
                ClanSavedData data = ClanSavedData.get(player.serverLevel());
                Clan clan = data.getClanByPlayer(player.getUUID());

                if (clan != null) {
                    long currentXp = event.getExperience();
                    double multiplier = 0.0;

                    // PERK 1: AFINITY I, II
                    if (hasTypePerk(pokemon, clan, 1) || hasTypePerk(pokemon, clan, 2)) {
                        multiplier += 0.30; // +30%
                    }

                    // PERK 2: '/clan bonus'
                    if (System.currentTimeMillis() < clan.getBonusEndTime()) {
                        multiplier += 0.20; // +20%
                    }

                    int extraXp = (int) (currentXp * multiplier);

                    if (extraXp > 0) {
                        com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource source =
                                new com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource("colisao-cobblemon");

                        pokemon.addExperience(source, extraXp);
                    }
                }
            }
            return Unit.INSTANCE;
        });

        // 2. Loot Bonus (+10%) & Type Gems (1%)
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (BattleVictoryEvent event) -> {

            event.getWinners().forEach((BattleActor winnerActor) -> {
                if (winnerActor instanceof PlayerBattleActor playerActor) {

                    ServerPlayer player = playerActor.getEntity();

                    if (player != null) {
                        ClanSavedData data = ClanSavedData.get(player.serverLevel());
                        Clan clan = data.getClanByPlayer(player.getUUID());

                        if (clan != null) {

                            event.getLosers().forEach((BattleActor loserActor) -> {
                                if (!(loserActor instanceof PlayerBattleActor)) {

                                    loserActor.getPokemonList().forEach((BattlePokemon loserBattlePkmn) -> {
                                        Pokemon loserPkmn = loserBattlePkmn.getEffectedPokemon();

                                        if (hasTypePerk(loserPkmn, clan, 1) || hasTypePerk(loserPkmn, clan, 2)) {

                                            if (Math.random() <= 0.10) {
                                                spawnExtraLoot(player.serverLevel(), loserPkmn, player);
                                            }

                                            if (Math.random() <= 0.01) {
                                                spawnTypeGem(player.serverLevel(), loserPkmn, clan, player);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                }
            });
            return Unit.INSTANCE;
        });
    }

    private static boolean hasTypePerk(Pokemon pokemon, Clan clan, int typeIndex) {
        if (typeIndex == 1 && clan.getLevel() < 3) return false;
        if (typeIndex == 2 && clan.getLevel() < 5) return false;

        String ptType = (typeIndex == 1) ? clan.getPrimaryType() : clan.getSecondaryType();
        if (ptType == null || ptType.equalsIgnoreCase("Nenhum")) return false;

        String englishType = getEnglishType(ptType);

        String primary = pokemon.getPrimaryType().getName().toLowerCase();
        String secondary = pokemon.getSecondaryType() != null ? pokemon.getSecondaryType().getName().toLowerCase() : "";

        return primary.equals(englishType) || secondary.equals(englishType);
    }

    private static void spawnExtraLoot(ServerLevel level, Pokemon pokemon, ServerPlayer player) {
        String pokemonName = pokemon.getSpecies().getName().toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("'", "");

        ResourceLocation lootLocation = ResourceLocation.parse("cobblemon:entities/pokemon/" + pokemonName);
        ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, lootLocation);

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootKey);

        if (lootTable != null && lootTable != LootTable.EMPTY) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .create(LootContextParamSets.ENTITY);

            List<ItemStack> drops = lootTable.getRandomItems(params);
            for (ItemStack stack : drops) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
                level.addFreshEntity(itemEntity);
            }
        }
    }

    private static void spawnTypeGem(ServerLevel level, Pokemon pokemon, Clan clan, ServerPlayer player) {
        String englishType = null;
        if (hasTypePerk(pokemon, clan, 1)) englishType = getEnglishType(clan.getPrimaryType());
        else if (hasTypePerk(pokemon, clan, 2)) englishType = getEnglishType(clan.getSecondaryType());

        if (englishType != null) {
            ResourceLocation gemId = ResourceLocation.parse("cobblemon:" + englishType + "_gem");
            net.minecraft.world.item.Item gemItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(gemId);

            if (gemItem != net.minecraft.world.item.Items.AIR) {
                ItemStack gemStack = new ItemStack(gemItem, 1);
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), gemStack);
                level.addFreshEntity(itemEntity);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d✨ Seu Clã ressoou com a batalha e gerou uma Joia do Tipo " + englishType.substring(0, 1).toUpperCase() + englishType.substring(1) + "!"));
            }
        }
    }

    private static String getEnglishType(String ptType) {
        return switch (ptType) {
            case "Fogo" -> "fire"; case "Água" -> "water"; case "Grama" -> "grass";
            case "Elétrico" -> "electric"; case "Gelo" -> "ice"; case "Lutador" -> "fighting";
            case "Venenoso" -> "poison"; case "Terra" -> "ground"; case "Voador" -> "flying";
            case "Psíquico" -> "psychic"; case "Inseto" -> "bug"; case "Pedra" -> "rock";
            case "Fantasma" -> "ghost"; case "Dragão" -> "dragon"; case "Sombrio" -> "dark";
            case "Aço" -> "steel"; case "Fada" -> "fairy"; default -> "normal";
        };
    }
}