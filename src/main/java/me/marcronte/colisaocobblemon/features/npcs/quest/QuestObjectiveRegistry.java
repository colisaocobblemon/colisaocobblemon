package me.marcronte.colisaocobblemon.features.npcs.quest;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestObjectiveRegistry {

    public interface QuestObjective {
        boolean canComplete(ServerPlayer player, String target, int amount, String questId);
        void onComplete(ServerPlayer player, String target, int amount, String questId);
        String getProgressText(ServerPlayer player, String target, int amount, String questId);
    }

    private static final Map<String, QuestObjective> OBJECTIVES = new HashMap<>();

    public static void register() {
        // 1. ITEMS
        OBJECTIVES.put("item", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                List<ItemReq> requirements = ItemReq.parse(target);

                for (ItemReq req : requirements) {
                    int count = 0;
                    for (ItemStack stack : player.getInventory().items) {
                        if (req.matches(stack)) count += stack.getCount();
                    }
                    if (count < amount) return false;
                }
                return true;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {
                List<ItemReq> requirements = ItemReq.parse(target);

                for (ItemReq req : requirements) {
                    player.getInventory().clearOrCountMatchingItems(
                            req::matches, amount, player.inventoryMenu.getCraftSlots()
                    );
                }
            }

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                List<ItemReq> requirements = ItemReq.parse(target);

                if (requirements.size() == 1) {
                    ItemReq req = requirements.get(0);
                    int count = 0;
                    for (ItemStack stack : player.getInventory().items) if (req.matches(stack)) count += stack.getCount();
                    return "\n\n(Progresso: " + count + " / " + amount + " " + req.getDisplayName() + ")";
                }

                StringBuilder sb = new StringBuilder("\n\n(Progresso:");
                for (ItemReq req : requirements) {
                    int count = 0;
                    for (ItemStack stack : player.getInventory().items) if (req.matches(stack)) count += stack.getCount();
                    sb.append("\n- ").append(count).append(" / ").append(amount).append(" ").append(req.getDisplayName());
                }
                sb.append(")");
                return sb.toString();
            }
        });

        // 2. POKEDEX SPECIFIC
        OBJECTIVES.put("dex_specific", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                Species species = PokemonSpecies.getByName(target.toLowerCase());
                if (species == null) return false;

                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
                SpeciesDexRecord record = dex.getSpeciesRecords().get(species.getResourceIdentifier());

                return record != null && record.getKnowledge().name().equalsIgnoreCase("OWNED");
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {}

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                return canComplete(player, target, amount, questId) ? "\n\n(Capturado!)" : "\n\n(Ainda não capturou um " + target + ")";
            }
        });

        // 3. POKEDEX QUANTITY
        OBJECTIVES.put("dex_count", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
                int count = 0;
                for (SpeciesDexRecord record : dex.getSpeciesRecords().values()) {
                    if (record.getKnowledge().name().equalsIgnoreCase("OWNED")) count++;
                }
                return count >= amount;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {}

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
                int count = 0;
                for (SpeciesDexRecord record : dex.getSpeciesRecords().values()) {
                    if (record.getKnowledge().name().equalsIgnoreCase("OWNED")) count++;
                }
                return "\n\n(Pokedex: " + count + " / " + amount + ")";
            }
        });

        // 4. POKEDEX GENERATION
        OBJECTIVES.put("dex_gen", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                String[] parts = target.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());

                for (Species s : PokemonSpecies.getImplemented()) {
                    if (s.getNationalPokedexNumber() >= start && s.getNationalPokedexNumber() <= end) {
                        SpeciesDexRecord record = dex.getSpeciesRecords().get(s.getResourceIdentifier());
                        if (record == null || !record.getKnowledge().name().equalsIgnoreCase("OWNED")) return false;
                    }
                }
                return true;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {}

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                String[] parts = target.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());

                int caught = 0;
                int total = 0;

                for (Species s : PokemonSpecies.getImplemented()) {
                    if (s.getNationalPokedexNumber() >= start && s.getNationalPokedexNumber() <= end) {
                        total++;
                        SpeciesDexRecord record = dex.getSpeciesRecords().get(s.getResourceIdentifier());
                        if (record != null && record.getKnowledge().name().equalsIgnoreCase("OWNED")) caught++;
                    }
                }
                return "\n\n(Gen Progresso: " + caught + " / " + total + ")";
            }
        });

        // 5. BEAT POKÉMON
        OBJECTIVES.put("defeat_pokemon", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                return QuestCounterData.get(player.serverLevel()).getCount(player.getUUID(), questId) >= amount;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {
                QuestCounterData.get(player.serverLevel()).clearCount(player.getUUID(), questId);
            }

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                int count = QuestCounterData.get(player.serverLevel()).getCount(player.getUUID(), questId);
                return "\n\n(Derrotados: " + count + " / " + amount + " " + target + ")";
            }
        });

        // 6. BEAT TYPE
        OBJECTIVES.put("defeat_type", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                return QuestCounterData.get(player.serverLevel()).getCount(player.getUUID(), questId) >= amount;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {
                QuestCounterData.get(player.serverLevel()).clearCount(player.getUUID(), questId);
            }

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                int count = QuestCounterData.get(player.serverLevel()).getCount(player.getUUID(), questId);
                return "\n\n(Derrotados: " + count + " / " + amount + " Pokémons do tipo " + target.substring(0, 1).toUpperCase() + target.substring(1) + ")";
            }
        });

        // 7. POKEDEX PER TYPE
        OBJECTIVES.put("dex_type", new QuestObjective() {
            @Override
            public boolean canComplete(ServerPlayer player, String target, int amount, String questId) {
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
                int count = 0;

                for (Species s : PokemonSpecies.getImplemented()) {
                    SpeciesDexRecord record = dex.getSpeciesRecords().get(s.getResourceIdentifier());

                    if (record != null && record.getKnowledge().name().equalsIgnoreCase("OWNED")) {
                        boolean hasType = false;
                        for (com.cobblemon.mod.common.api.types.ElementalType type : s.getTypes()) {
                            if (type.getName().equalsIgnoreCase(target)) {
                                hasType = true;
                                break;
                            }
                        }
                        if (hasType) count++;
                    }
                }
                return count >= amount;
            }

            @Override
            public void onComplete(ServerPlayer player, String target, int amount, String questId) {}

            @Override
            public String getProgressText(ServerPlayer player, String target, int amount, String questId) {
                PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
                int count = 0;

                for (Species s : PokemonSpecies.getImplemented()) {
                    SpeciesDexRecord record = dex.getSpeciesRecords().get(s.getResourceIdentifier());

                    if (record != null && record.getKnowledge().name().equalsIgnoreCase("OWNED")) {
                        boolean hasType = false;
                        for (com.cobblemon.mod.common.api.types.ElementalType type : s.getTypes()) {
                            if (type.getName().equalsIgnoreCase(target)) {
                                hasType = true;
                                break;
                            }
                        }
                        if (hasType) count++;
                    }
                }

                String formatedType = target.substring(0, 1).toUpperCase() + target.substring(1);
                return "\n\n(Pokédex " + formatedType + ": " + count + " / " + amount + ")";
            }
        });
    }

    public static QuestObjective getObjective(String type) {
        if (type == null || type.isEmpty()) return OBJECTIVES.get("item");
        return OBJECTIVES.getOrDefault(type.toLowerCase(), OBJECTIVES.get("item"));
    }

    private record ItemReq(String id, String customName, Item item) {

        public static List<ItemReq> parse(String target) {
            List<ItemReq> list = new ArrayList<>();
            for (String raw : target.split(",")) {
                raw = raw.trim();

                if (raw.contains("=")) {
                    String[] parts = raw.split("=", 2);
                    String id = parts[0].trim();
                    String name = parts[1].trim();

                    if (name.startsWith("'") && name.endsWith("'")) {
                        name = name.substring(1, name.length() - 1);
                    }

                    list.add(new ItemReq(id, name, BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))));
                } else {
                    list.add(new ItemReq(raw, null, BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw))));
                }
            }
            return list;
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !stack.is(item)) return false;

            if (customName != null) {
                return stack.getHoverName().getString().equals(customName);
            }
            return true;
        }

        public String getDisplayName() {
            return customName != null ? customName : item.getDescription().getString();
        }
    }
}