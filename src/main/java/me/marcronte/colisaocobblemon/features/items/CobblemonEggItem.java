package me.marcronte.colisaocobblemon.features.items;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.marcronte.colisaocobblemon.features.clans.ClanMissionHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class CobblemonEggItem extends Item {

    public CobblemonEggItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("PokemonData")) return;

        float currentDist = player.walkDist;
        float lastDist = tag.getFloat("LastKnownWalkDist");

        if (lastDist == 0) {
            tag.putFloat("LastKnownWalkDist", currentDist);
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            return;
        }

        float delta = currentDist - lastDist;

        if (delta < 0 || delta > 100) {
            tag.putFloat("LastKnownWalkDist", currentDist);
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            return;
        }

        float blocksPerStep = 1.0f;

        if (delta >= blocksPerStep) {
            int stepsToTake = (int) (delta / blocksPerStep);
            float consumedDist = stepsToTake * blocksPerStep;

            int remaining = tag.getInt("RemainingSteps");

            if (remaining > 0) {
                int stepMultiplier = 1;
                var party = Cobblemon.INSTANCE.getStorage().getParty(player);

                for (int i = 0; i < party.size(); i++) {
                    Pokemon p = party.get(i);
                    if (p != null && p.getAbility() != null) {
                        String abilityName = p.getAbility().getName().toLowerCase().replace("_", "").replace(" ", "");

                        if (abilityName.equals("flamebody") ||
                                abilityName.equals("magmaarmor") ||
                                abilityName.equals("steamengine")) {
                            stepMultiplier = 2;
                            break;
                        }
                    }
                }

                int effectiveSteps = stepsToTake * stepMultiplier;

                tag.putInt("RemainingSteps", Math.max(0, remaining - effectiveSteps));
                tag.putFloat("LastKnownWalkDist", lastDist + consumedDist);

                CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            } else {
                hatchEgg(player, stack, tag);
            }
        }
    }

    private void hatchEgg(ServerPlayer player, ItemStack stack, CompoundTag tag) {
        String speciesStr = tag.getString("SpeciesIdentifier");
        String formStr = tag.getString("Form");
        Pokemon pokemon;

        if (!speciesStr.isEmpty()) {
            StringBuilder dna = new StringBuilder("species=").append(speciesStr);

            if (!formStr.isEmpty() && !formStr.equalsIgnoreCase("normal")) {
                String cleanForm = formStr.toLowerCase();
                dna.append(" form=").append(cleanForm);

                if (cleanForm.contains("hisui")) {
                    dna.append(" hisuian");
                } else if (cleanForm.contains("alola")) {
                    dna.append(" alolan");
                } else if (cleanForm.contains("galar")) {
                    dna.append(" galarian");
                } else if (cleanForm.contains("paldea")) {
                    dna.append(" paldean");
                }
            }

            pokemon = PokemonProperties.Companion.parse(dna.toString()).create();

            if (!formStr.isEmpty() && !formStr.equalsIgnoreCase("normal")) {
                String cleanForm = formStr.toLowerCase();
                if (cleanForm.contains("hisui") && !pokemon.getAspects().contains("hisuian")) {
                    pokemon.getAspects().add("hisuian");
                } else if (cleanForm.contains("alola") && !pokemon.getAspects().contains("alolan")) {
                    pokemon.getAspects().add("alolan");
                } else if (cleanForm.contains("galar") && !pokemon.getAspects().contains("galarian")) {
                    pokemon.getAspects().add("galarian");
                } else if (cleanForm.contains("paldea") && !pokemon.getAspects().contains("paldean")) {
                    pokemon.getAspects().add("paldean");
                }
            }
        } else {
            pokemon = new Pokemon();
        }

        String natureName = tag.getString("NatureInternal");
        if (!natureName.isEmpty()) {
            PokemonProperties.Companion.parse("nature=" + natureName).apply(pokemon);
        }

        String abilityName = tag.getString("AbilityInternal");
        if (!abilityName.isEmpty()) {
            PokemonProperties.Companion.parse("ability=" + abilityName).apply(pokemon);
        }

        String movesString = tag.getString("Moves");
        if (!movesString.isEmpty()) {
            PokemonProperties.Companion.parse("moves=" + movesString).apply(pokemon);
        }

        boolean isShiny = tag.getBoolean("IsShiny");
        pokemon.setShiny(isShiny);

        pokemon.getIvs().set(Stats.HP, tag.getInt("IV_HP"));
        pokemon.getIvs().set(Stats.ATTACK, tag.getInt("IV_ATK"));
        pokemon.getIvs().set(Stats.DEFENCE, tag.getInt("IV_DEF"));
        pokemon.getIvs().set(Stats.SPECIAL_ATTACK, tag.getInt("IV_SPA"));
        pokemon.getIvs().set(Stats.SPECIAL_DEFENCE, tag.getInt("IV_SPD"));
        pokemon.getIvs().set(Stats.SPEED, tag.getInt("IV_SPE"));

        pokemon.setLevel(1);
        pokemon.heal();

        Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
        player.getInventory().removeItem(stack);

        ClanMissionHandler.onEggHatched(player);

        if (isShiny) {
            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.egg_hatched_shiny", pokemon.getSpecies().getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            player.sendSystemMessage(Component.translatable("message.colisao-cobblemon.egg_hatched_normal", pokemon.getSpecies().getName()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains("SpeciesIdentifier")) {
            tooltipComponents.add(Component.literal("???").withStyle(ChatFormatting.RED));
            return;
        }

        CompoundTag tag = customData.copyTag();

        String speciesId = tag.getString("SpeciesIdentifier");
        String displayName = "???";

        try {
            String cleanName = speciesId.toLowerCase().replace("cobblemon:", "").trim();
            com.cobblemon.mod.common.pokemon.Species species = PokemonSpecies.getByName(cleanName);
            if (species != null) {
                displayName = species.getName();
            } else {
                displayName = formatName(cleanName);
            }
        } catch (Exception e) {
            displayName = formatName(speciesId);
        }

        tooltipComponents.add(Component.literal("Pokémon: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(displayName).withStyle(ChatFormatting.AQUA)));

        String rawNature = tag.getString("NatureInternal");
        tooltipComponents.add(Component.literal("Nature: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formatName(rawNature)).withStyle(ChatFormatting.YELLOW)));

        String rawAbility = tag.getString("AbilityInternal");
        tooltipComponents.add(Component.literal("Habilidade: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formatName(rawAbility)).withStyle(ChatFormatting.LIGHT_PURPLE)));

        int steps = tag.getInt("RemainingSteps");
        tooltipComponents.add(Component.literal("Passos restantes: " + steps).withStyle(ChatFormatting.DARK_GREEN));

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("IVs:").withStyle(ChatFormatting.GOLD));

        appendIvLine(tooltipComponents, "HP", tag.getInt("IV_HP"));
        appendIvLine(tooltipComponents, "Atk", tag.getInt("IV_ATK"));
        appendIvLine(tooltipComponents, "Def", tag.getInt("IV_DEF"));
        appendIvLine(tooltipComponents, "SpA", tag.getInt("IV_SPA"));
        appendIvLine(tooltipComponents, "SpD", tag.getInt("IV_SPD"));
        appendIvLine(tooltipComponents, "Spe", tag.getInt("IV_SPE"));
    }

    private void appendIvLine(List<Component> tooltip, String label, int value) {
        ChatFormatting color = ChatFormatting.WHITE;
        if (value == 31) color = ChatFormatting.GOLD;
        else if (value >= 25) color = ChatFormatting.GREEN;
        else if (value <= 5) color = ChatFormatting.RED;
        tooltip.add(Component.literal(" " + label + ": " + value).withStyle(color));
    }

    private String formatName(String input) {
        if (input == null || input.isEmpty()) return "???";

        if (input.contains(".")) {
            input = input.substring(input.lastIndexOf(".") + 1);
        }

        input = input.replace("_", " ");

        StringBuilder result = new StringBuilder();
        String[] words = input.split(" ");
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1).toLowerCase());
        }

        return result.toString();
    }
}