package me.marcronte.colisaocobblemon.client.gui;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.item.PokemonItem; //Novo
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import me.marcronte.colisaocobblemon.ColisaoCobblemon;
import me.marcronte.colisaocobblemon.network.BreedingNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag; //Novo
import net.minecraft.nbt.StringTag; //Novo
import net.minecraft.nbt.Tag; //Novo
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack; //Novo
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ajuda as telas de breeding (BreedingScreen e HabitatScreen) a desenhar os pais.
 *
 * Durante o breeding os dois Pokémon são removidos do party e ficam guardados só como NBT no
 * servidor, então o cliente não consegue mais achá-los no party. Aqui o Pokémon é reconstruído
 * a partir desse NBT (mantendo forma, shiny, gênero e aspectos).
 */
public final class BreedingDisplay {

    private static boolean renderFailureLogged = false;

    private BreedingDisplay() {}

    /**
     * Reconstrói o Pokémon em breeding a partir do NBT enviado pelo servidor.
     * Se não for possível, cai para um Pokémon simples criado só com a espécie.
     */
    @Nullable
    public static Pokemon fromData(@Nullable CompoundTag data, @Nullable String speciesFallback) {
        if (data != null && !data.isEmpty()) {
            try {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    Pokemon pokemon;
                    if (!data.contains("UUID") && data.contains("AspectsBackup")) {
                        pokemon = BreedingNetwork.reconstructPokemon(data, level.registryAccess());
                    } else {
                        pokemon = Pokemon.Companion.loadFromNBT(level.registryAccess(), data);
                    }
                    if (pokemon != null) return applyDisplayForm(pokemon, data); //Alteração
                }
            } catch (Exception e) {
                ColisaoCobblemon.LOGGER.warn("[Breeding] Não foi possível reconstruir o Pokémon no cliente, usando fallback pela espécie.", e);
            }
        }

        String species = speciesFallback;
        if ((species == null || species.isBlank()) && data != null) {
            species = data.getString("UI_Species");
        }
        return applyDisplayForm(dummyFromSpecies(species), data); //Alteração
    }

    //Novo
    /**
     * Cria o ItemStack do sprite de um Pokémon já garantindo o visual da forma regional
     * (Alola, Galar, Hisui, Paldea). Use no lugar de PokemonItem.from(pokemon, 1) nas telas.
     */
    public static ItemStack spriteStack(@Nullable Pokemon pokemon) {
        if (pokemon == null) return ItemStack.EMPTY;
        return PokemonItem.from(applyDisplayForm(pokemon, null), 1);
    }

    //Novo
    /** Aspectos que identificam as formas regionais. */
    private static final String[] REGIONAL_ASPECTS = {"alolan", "galarian", "hisuian", "paldean"};

    //Alteração
    /**
     * Garante que formas regionais (Alola, Galar, Hisui, Paldea) apareçam no sprite.
     * O sprite é desenhado a partir da espécie + aspectos do Pokémon; ao reconstruir pelo NBT (ou pelo
     * fallback só com a espécie) o aspecto regional pode ficar de fora e o Pokémon aparece na forma base.
     * Por isso procura no NBT (e no nome da forma) um dos aspectos regionais e, se achar, recria o
     * Pokémon de exibição com esse aspecto.
     */
    @Nullable
    private static Pokemon applyDisplayForm(@Nullable Pokemon pokemon, @Nullable CompoundTag data) {
        if (pokemon == null) return null;

        try {
            String aspect = matchRegionalAspect(pokemon.getForm().getName());
            if (aspect == null && data != null) aspect = findRegionalAspect(data);
            if (aspect == null) return pokemon;

            //Alteração
            // O sprite é desenhado por PokemonItem.from(pokemon) a partir de species + pokemon.getAspects().
            // Recriar via PokemonProperties.apply() não atualizava os aspectos exibidos; então usa o mesmo
            // padrão já usado no resto do mod (BreedingCalculator, CobblemonEggItem): adicionar o aspecto
            // diretamente ao conjunto de aspectos do Pokémon.
            if (!pokemon.getAspects().contains(aspect)) {
                pokemon.getAspects().add(aspect);
            }
            return pokemon;
        } catch (Exception e) {
            ColisaoCobblemon.LOGGER.warn("[Breeding] Não foi possível aplicar a forma ao Pokémon exibido, mantendo a forma atual.", e);
            return pokemon;
        }
    }

    //Novo
    /** Devolve o aspecto regional se o texto for exatamente um deles (sem diferenciar maiúsculas). */
    @Nullable
    private static String matchRegionalAspect(@Nullable String value) {
        if (value == null) return null;
        for (String aspect : REGIONAL_ASPECTS) {
            if (aspect.equalsIgnoreCase(value.trim())) return aspect;
        }
        return null;
    }

    //Novo
    /** Percorre o NBT inteiro procurando um valor de texto que seja um aspecto regional. */
    @Nullable
    private static String findRegionalAspect(@Nullable Tag tag) {
        if (tag == null) return null;

        if (tag instanceof StringTag stringTag) {
            return matchRegionalAspect(stringTag.getAsString());
        }
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                String found = findRegionalAspect(compound.get(key));
                if (found != null) return found;
            }
        } else if (tag instanceof ListTag list) {
            for (Tag element : list) {
                String found = findRegionalAspect(element);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Nullable
    public static Pokemon dummyFromSpecies(@Nullable String speciesStr) {
        if (speciesStr == null || speciesStr.isBlank()) return null;

        try {
            String cleanName = speciesStr.toLowerCase().replace("cobblemon:", "").trim();
            Species species = PokemonSpecies.getByName(cleanName);
            if (species == null) return null;

            Pokemon dummy = new Pokemon();
            dummy.setSpecies(species);
            return dummy;
        } catch (Exception e) {
            ColisaoCobblemon.LOGGER.warn("[Breeding] Falha ao criar Pokémon de exibição para '{}'.", speciesStr, e);
            return null;
        }
    }

    // ---- Pokémon castrados -------------------------------------------------------------------

    private static final int CASTRATED_COLOR = 0xFF5555;

    /** UUIDs dos Pokémon do party castrados, enviados pelo servidor ao abrir uma tela de breeding. */
    private static Set<UUID> castratedParty = new HashSet<>();

    public static void setCastratedParty(Collection<UUID> uuids) {
        castratedParty = new HashSet<>(uuids);
    }

    public static boolean isCastrated(@Nullable Pokemon pokemon) {
        return pokemon != null && castratedParty.contains(pokemon.getUuid());
    }

    /** Selo "Castrado" centralizado, usado abaixo do nome do Pokémon nos slots de pai/mãe. */
    public static void drawCastratedTag(GuiGraphics graphics, Font font, int centerX, int y) {
        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.castrated"), centerX, y, CASTRATED_COLOR);
    }

    /** Versão pequena, alinhada à esquerda, para as entradas da lista de party. */
    public static void drawCastratedTagSmall(GuiGraphics graphics, Font font, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.7f, 0.7f, 1f);
        graphics.drawString(font, Component.translatable("gui.colisao-cobblemon.castrated"), 0, 0, CASTRATED_COLOR);
        graphics.pose().popPose();
    }

    /** Cor de fundo de uma entrada da lista de party (avermelhada se o Pokémon for castrado). */
    public static int partyEntryColor(boolean castrated, boolean hovered) {
        if (castrated) return hovered ? 0xC0CC4444 : 0xC0661111;
        return hovered ? 0xC0FFFFFF : 0xC0000000;
    }

    /** Registra (uma única vez) falhas ao desenhar o sprite, em vez de engolir o erro em silêncio. */
    public static void logRenderFailure(Exception e) {
        if (renderFailureLogged) return;
        renderFailureLogged = true;
        ColisaoCobblemon.LOGGER.warn("[Breeding] Falha ao desenhar o sprite do Pokémon na tela de breeding.", e);
    }
}
