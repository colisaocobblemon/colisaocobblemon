package me.marcronte.colisaocobblemon.features.professions;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import me.marcronte.colisaocobblemon.config.ProfessionsPerksConfig;
import me.marcronte.colisaocobblemon.data.ProfessionPlayerData;
import me.marcronte.colisaocobblemon.network.payloads.StylistPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StylistManager {
    private static final List<String> RANK_ORDER = List.of("rank_e", "rank_d", "rank_c", "rank_b", "rank_a");

    public static void handleCategorySelect(ServerPlayer player, StylistPayloads.SelectCategoryPayload payload) {
        ProfessionPlayerData data = ProfessionPlayerData.get(player.serverLevel());
        ProfessionPlayerData.PlayerProf prof = data.getPlayer(player.getUUID());
        String currentRank = prof.rank.toLowerCase();

        Map<String, Object> stylistData = ProfessionsPerksConfig.INSTANCE.perks.get("estilista");
        if (stylistData == null) return;

        List<Map<String, Object>> validRecipes = new ArrayList<>();

        for (String rankLevel : RANK_ORDER) {
            Object rankObj = stylistData.get(rankLevel);
            if (rankObj instanceof Map<?, ?> rankMap) {
                Object categoriesObj = rankMap.get("categories");
                if (categoriesObj instanceof Map<?, ?> categoriesMap) {
                    Object categoryObj = categoriesMap.get(payload.categoryName());
                    if (categoryObj instanceof List<?> recipeList) {
                        for (Object r : recipeList) {
                            if (r instanceof Map<?, ?> tempMap) {
                                Map<String, Object> safeMap = new java.util.HashMap<>();
                                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                                    safeMap.put(String.valueOf(entry.getKey()), entry.getValue());
                                }
                                validRecipes.add(safeMap);
                            }
                        }
                    }
                }
            }
            if (rankLevel.equals(currentRank)) break;
        }

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<String> partySpecies = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Pokemon p = party.get(i);
            if (p != null) {
                partySpecies.add(p.getSpecies().getName().toLowerCase());
            } else {
                partySpecies.add("empty");
            }
        }

        Gson gson = new Gson();
        String recipesJson = gson.toJson(validRecipes);
        String partyJson = gson.toJson(partySpecies);

        ServerPlayNetworking.send(player, new StylistPayloads.OpenCraftPayload(payload.categoryName(), recipesJson, partyJson));
    }

    public static void handleApply(ServerPlayer player, StylistPayloads.PerformApplyPayload payload) {
        ProfessionPlayerData data = ProfessionPlayerData.get(player.serverLevel());
        ProfessionPlayerData.PlayerProf prof = data.getPlayer(player.getUUID());
        String currentRank = prof.rank.toLowerCase();

        Map<String, Object> stylistData = ProfessionsPerksConfig.INSTANCE.perks.get("estilista");
        if (stylistData == null) return;

        List<Map<String, Object>> validRecipes = new ArrayList<>();
        for (String rankLevel : RANK_ORDER) {
            Object rankObj = stylistData.get(rankLevel);
            if (rankObj instanceof Map<?, ?> rankMap) {
                Object categoriesObj = rankMap.get("categories");
                if (categoriesObj instanceof Map<?, ?> categoriesMap) {
                    Object categoryObj = categoriesMap.get(payload.category());
                    if (categoryObj instanceof List<?> recipeList) {
                        for (Object r : recipeList) {
                            if (r instanceof Map<?, ?> tempMap) {
                                Map<String, Object> safeMap = new java.util.HashMap<>();
                                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                                    safeMap.put(String.valueOf(entry.getKey()), entry.getValue());
                                }
                                validRecipes.add(safeMap);
                            }
                        }
                    }
                }
            }
            if (rankLevel.equals(currentRank)) break;
        }

        if (payload.recipeIndex() < 0 || payload.recipeIndex() >= validRecipes.size()) return;
        Map<String, Object> recipe = validRecipes.get(payload.recipeIndex());

        List<String> ingredientsList = new ArrayList<>();
        Object ingObj = recipe.get("ingredients");
        if (ingObj instanceof List<?> tempList) {
            for (Object item : tempList) {
                ingredientsList.add(String.valueOf(item));
            }
        }

        String targetSpecies = (String) recipe.get("species");
        String aspect = (String) recipe.get("aspect");

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon pokemon = party.get(payload.partySlot());

        if (pokemon == null || !pokemon.getSpecies().getName().equalsIgnoreCase(targetSpecies)) {
            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.pokemon_stylist_not_compatible"));
            return;
        }

        for (String ing : ingredientsList) {
            String[] parts = ing.split("_", 2);
            int amountNeeded = Integer.parseInt(parts[0].trim());
            Item itemNeeded = BuiltInRegistries.ITEM.get(ResourceLocation.parse(parts[1].trim()));

            if (countItems(player, itemNeeded) < amountNeeded) {
                player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.player_need_materials", amountNeeded, itemNeeded.getDescription().getString()));
                return;
            }
        }

        for (String ing : ingredientsList) {
            String[] parts = ing.split("_", 2);
            int amountToRemove = Integer.parseInt(parts[0].trim());
            Item itemToRemove = BuiltInRegistries.ITEM.get(ResourceLocation.parse(parts[1].trim()));
            removeItems(player, itemToRemove, amountToRemove);
        }

        pokemon.getPersistentData().putString("custom_stylist_skin", aspect);

        try {
            pokemon.getAspects().add(aspect);
        } catch (Exception e) {
            PokemonProperties.Companion.parse(aspect).apply(pokemon);
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);

        String formattedTarget = targetSpecies.substring(0, 1).toUpperCase() + targetSpecies.substring(1);
        player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.pokemon_applied_addon"));

        player.closeContainer();
    }

    private static int countItems(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, Item item, int amount) {
        int leftToRemove = amount;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(item)) {
                if (stack.getCount() >= leftToRemove) {
                    stack.shrink(leftToRemove);
                    return;
                } else {
                    leftToRemove -= stack.getCount();
                    stack.setCount(0);
                }
            }
        }
    }
}