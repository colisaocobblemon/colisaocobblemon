package me.marcronte.colisaocobblemon.client.gui;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.systems.RenderSystem;
import me.marcronte.colisaocobblemon.network.payloads.BreedingButtonPayload;
import me.marcronte.colisaocobblemon.network.payloads.BreedingSelectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class BreedingScreen extends Screen {

    private static final ResourceLocation BG_NOT_READY = ResourceLocation.parse("colisao-cobblemon:textures/gui/breeding_not_ready.png");
    private static final ResourceLocation BG_READY = ResourceLocation.parse("colisao-cobblemon:textures/gui/breeding_ready.png");

    private UUID motherId;
    private UUID fatherId;

    private String motherSpeciesStr;
    private String fatherSpeciesStr;

    private final CompoundTag motherData;
    private final CompoundTag fatherData;

    private long startTime;
    private long totalDuration;
    private boolean isActive;
    private boolean isReadyServer;

    private Pokemon cachedMotherDummy;
    private Pokemon cachedFatherDummy;

    private int selectingSlot = -1;

    public BreedingScreen(UUID mother, UUID father, String mSpecies, String fSpecies, CompoundTag mData, CompoundTag fData, long start, long duration, boolean active, boolean ready) {
        super(Component.literal("Breeding"));
        this.motherId = mother;
        this.fatherId = father;
        this.motherSpeciesStr = mSpecies;
        this.fatherSpeciesStr = fSpecies;
        this.motherData = mData;
        this.fatherData = fData;
        this.startTime = start;
        this.totalDuration = duration;
        this.isActive = active;
        this.isReadyServer = ready;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        boolean isReadyTime = isActive && (System.currentTimeMillis() - startTime >= totalDuration);
        ResourceLocation bg = (isReadyServer || isReadyTime) ? BG_READY : BG_NOT_READY;

        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;

        if (isActive) {
            renderTimer(graphics, x, y);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, bg);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        graphics.blit(bg, x, y, 0, 0, 256, 256, 256, 256);

        if (motherId != null) {
            Pokemon mother = resolvePokemonToRender(motherId, motherSpeciesStr, true);
            if (mother != null) {
                renderPokemonSprite(graphics, mother, x + 30, y + 70, 3.0f);
                String name = (mother.getSpecies() != null) ? mother.getSpecies().getName() : "???";
                graphics.drawCenteredString(font, name, x + 51, y + 115, 0xFFFFFF);
                if (BreedingDisplay.isCastrated(mother)) BreedingDisplay.drawCastratedTag(graphics, font, x + 51, y + 126);
            }
        }

        if (fatherId != null) {
            Pokemon father = resolvePokemonToRender(fatherId, fatherSpeciesStr, false);
            if (father != null) {
                renderPokemonSprite(graphics, father, x + 182, y + 70, 3.0f);
                String name = (father.getSpecies() != null) ? father.getSpecies().getName() : "???";
                graphics.drawCenteredString(font, name, x + 203, y + 115, 0xFFFFFF);
                if (BreedingDisplay.isCastrated(father)) BreedingDisplay.drawCastratedTag(graphics, font, x + 203, y + 126);
            }
        }
        float scale = 1.5f;

        graphics.pose().pushPose();
        graphics.pose().translate(x + 204, y + 55, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.breeding_father"), 0, 0, 0xFFFFFF);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(x + 52, y + 55, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.breeding_mother"), 0, 0, 0xFFFFFF);
        graphics.pose().popPose();

        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.choose"), x + 52, y + 153, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.choose"), x + 204, y + 153, 0xFFFFFF);
        if (!isActive) {
            graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.breeding_breed"), x + 128, y + 193, 0xFFFFFF);
        } else if (!(isReadyServer || isReadyTime)) {
            // Breed em andamento: o botão vira "Cancelar". Com o ovo pronto o botão some (coleta-se o ovo).
            graphics.drawCenteredString(font, Component.translatable("message.colisao-cobblemon.cancel"), x + 128, y + 193, 0xFF6666);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.claim"), x + 127, y + 153, 0xFFFFFF);

        if (selectingSlot == 0) graphics.fill(x + 25, y + 145, x + 79, y + 169, 0x80009BAD);
        if (selectingSlot == 1) graphics.fill(x + 177, y + 145, x + 230, y + 168, 0x80009BAD);

        if (selectingSlot != -1) {
            renderPartyList(graphics, mouseX, mouseY, x - 70, y + 20);
        }
    }

    private Pokemon resolvePokemonToRender(UUID uuid, String speciesStr, boolean isMother) {
        // Com o breeding ativo os pais já saíram do party, então são reconstruídos
        // a partir dos dados enviados pelo servidor (e não buscados no party).
        if (isActive) {
            if (isMother) {
                if (cachedMotherDummy == null) {
                    cachedMotherDummy = BreedingDisplay.fromData(motherData, speciesStr);
                }
                return cachedMotherDummy;
            } else {
                if (cachedFatherDummy == null) {
                    cachedFatherDummy = BreedingDisplay.fromData(fatherData, speciesStr);
                }
                return cachedFatherDummy;
            }
        }

        return getClientSidePokemon(uuid);
    }

    private void renderPartyList(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY) {
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
        int i = 0;

        graphics.drawString(font, "Party:", startX, startY - 15, 0xFFFFFF);

        for (Pokemon p : party) {
            if (p == null) { i++; continue; }

            int slotY = startY + (i * 35);
            boolean isHovered = mouseX >= startX && mouseX <= startX + 60 && mouseY >= slotY && mouseY <= slotY + 32;
            boolean castrated = BreedingDisplay.isCastrated(p);
            int color = BreedingDisplay.partyEntryColor(castrated, isHovered);

            graphics.fill(startX, slotY, startX + 60, slotY + 32, color);
            renderPokemonSprite(graphics, p, startX + 2, slotY + 2, 1.5f);
            if (castrated) BreedingDisplay.drawCastratedTagSmall(graphics, font, startX + 22, slotY + 21);

            graphics.pose().pushPose();
            graphics.pose().translate(startX + 22, slotY + 12, 0);
            graphics.pose().scale(0.7f, 0.7f, 1f);
            String name = (p.getSpecies() != null) ? p.getSpecies().getName() : "???";
            graphics.drawString(font, name, 0, 0, 0xFFFFFF);
            graphics.pose().popPose();

            i++;
        }
    }

    private Pokemon getClientSidePokemon(UUID uuid) {
        if (uuid == null) return null;
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
        for (Pokemon p : party) {
            if (p != null && p.getUuid().equals(uuid)) return p;
        }
        return null;
    }

    private void renderPokemonSprite(GuiGraphics graphics, Pokemon pokemon, int x, int y, float scale) {
        if (pokemon == null) return;
        try {
            ItemStack stack = PokemonItem.from(pokemon, 1);
            if (stack.isEmpty()) return;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        } catch (Exception e) {
            BreedingDisplay.logRenderFailure(e);
        }
    }

    private void renderTimer(GuiGraphics graphics, int x, int y) {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = totalDuration - elapsed;
        if (remaining < 0) remaining = 0;

        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / (1000 * 60)) % 60;
        long hours = (remaining / (1000 * 60 * 60));

        String timeText = (hours > 0) ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);

        int color = (remaining <= 0) ? 0x00FF00 : 0xFFFFFF;
        graphics.drawCenteredString(font, timeText, x + 128, y + 45, color);

        if (remaining <= 0 && !isReadyServer) {
            graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.waiting"), x + 128, y + 55, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;

        if (selectingSlot != -1) {
            int startX = x - 70;
            int startY = y + 20;
            ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
            int i = 0;
            for (Pokemon p : party) {
                if (p == null) { i++; continue; }
                int slotY = startY + (i * 35);
                if (mouseX >= startX && mouseX <= startX + 60 && mouseY >= slotY && mouseY <= slotY + 32) {
                    Gender gender = p.getGender();
                    if (selectingSlot == 0 && gender == Gender.MALE) {
                        Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.colisao-cobblemon.female_slot").withStyle(ChatFormatting.RED), true);
                        return true;
                    } else if (selectingSlot == 1 && gender == Gender.FEMALE) {
                        Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.colisao-cobblemon.male_slot").withStyle(ChatFormatting.RED), true);
                        return true;
                    }
                    ClientPlayNetworking.send(new BreedingSelectPayload(selectingSlot, p.getUuid()));
                    selectingSlot = -1;
                    return true;
                }
                i++;
            }
        }

        double relX = mouseX - x;
        double relY = mouseY - y;
        if (relX >= 25 && relX <= 78 && relY >= 145 && relY <= 168 && !isActive) { selectingSlot = (selectingSlot == 0) ? -1 : 0; return true; }
        if (relX >= 177 && relX <= 230 && relY >= 145 && relY <= 168 && !isActive) { selectingSlot = (selectingSlot == 1) ? -1 : 1; return true; }
        if (relX >= 101 && relX <= 154 && relY >= 145 && relY <= 168) {
            boolean isReadyTime = isActive && (System.currentTimeMillis() - startTime >= totalDuration);
            if (isReadyServer || isReadyTime) { sendClick(3); return true; }
        }
        if (relX >= 101 && relX <= 154 && relY >= 185 && relY <= 208) {
            if (!isActive) { sendClick(2); return true; }   // Breed
            boolean readyNow = isReadyServer || (System.currentTimeMillis() - startTime >= totalDuration);
            if (!readyNow) { sendClick(4); return true; }   // Cancelar
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendClick(int buttonId) {
        ClientPlayNetworking.send(new BreedingButtonPayload(buttonId));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}