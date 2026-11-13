package me.marcronte.colisaocobblemon.client.gui;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.systems.RenderSystem;
import me.marcronte.colisaocobblemon.ColisaoCobblemon;
import me.marcronte.colisaocobblemon.features.blocks.machines.pokefurnace.PokeFurnaceMenu;
import me.marcronte.colisaocobblemon.network.payloads.MachineActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PokeFurnaceScreen extends AbstractContainerScreen<PokeFurnaceMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ColisaoCobblemon.MOD_ID, "textures/gui/poke_furnace.png");

    private boolean isSelecting = false;
    private Button actionButton;

    public PokeFurnaceScreen(PokeFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 235;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.actionButton = this.addRenderableWidget(Button.builder(Component.literal("..."), button -> {
            if (this.menu.hasPokemon()) {
                ClientPlayNetworking.send(new MachineActionPayload(0, this.menu.getBlockPos(), null));
                this.isSelecting = false;
            } else {
                this.isSelecting = !this.isSelecting;
            }
        }).bounds(this.leftPos + 82, this.topPos + 54, 47, 16).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.actionButton.setMessage(Component.literal(this.menu.hasPokemon() ? "Remover" : "Adicionar"));

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.menu.hasPokemon()) {
            int barX = this.leftPos + 143;
            int barYBottom = this.topPos + 61;
            int barHeight = 40;
            int barWidth = 15;

            int barYTop = barYBottom - barHeight;

            if (mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barYTop && mouseY <= barYBottom) {
                int friendship = Math.min(255, Math.max(0, this.menu.getFriendship()));
                Component tooltipText = Component.literal(friendship + "/255 de Amizade");
                guiGraphics.renderTooltip(this.font, tooltipText, mouseX, mouseY);
            }
        }

        if (this.isSelecting && !this.menu.hasPokemon()) {
            renderPartyList(guiGraphics, mouseX, mouseY, this.leftPos - 70, this.topPos + 20);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        if (this.menu.hasPokemon()) {
            int power = this.menu.getPowerLevel();
            String label = switch (power) {
                case 3 -> "§cForte";
                case 2 -> "§eMédia";
                default -> "§aFraca";
            };
            guiGraphics.drawString(this.font, "Potência:", 80, 15, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, label, 80, 25, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, "§cSem Pokémon", 73, 22, 0xFFFFFF, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                176,
                235
        );

        if (this.menu.hasPokemon()) {
            Pokemon pokemon = this.menu.getPokemon();
            if (pokemon != null) {
                renderPokemonSprite(guiGraphics, pokemon, this.leftPos + 16, this.topPos + 16, 3.2f);
            }
        }

        if (this.menu.hasPokemon()) {
            int friendship = Math.min(255, Math.max(0, this.menu.getFriendship()));
            int barHeight = (int) ((friendship / 255.0f) * 40.0f);

            int barX = this.leftPos + 143;
            int barYBottom = this.topPos + 61;

            guiGraphics.fill(barX, barYBottom - barHeight, barX + 15, barYBottom, 0xFFFF69B4);
        }
    }

    private void renderPartyList(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY) {
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
        int i = 0;

        graphics.drawString(this.font, "Sua Party:", startX, startY - 15, 0xFFFFFF);

        for (Pokemon p : party) {
            if (p == null) { i++; continue; }

            int slotY = startY + (i * 35);
            boolean isHovered = mouseX >= startX && mouseX <= startX + 60 && mouseY >= slotY && mouseY <= slotY + 32;
            int color = isHovered ? 0xC0FFFFFF : 0xC0000000;

            graphics.fill(startX, slotY, startX + 60, slotY + 32, color);
            renderPokemonSprite(graphics, p, startX + 2, slotY + 2, 1.5f);

            graphics.pose().pushPose();
            graphics.pose().translate(startX + 22, slotY + 12, 0);
            graphics.pose().scale(0.7f, 0.7f, 1f);
            String name = (p.getSpecies() != null) ? p.getSpecies().getName() : "???";
            graphics.drawString(this.font, name, 0, 0, 0xFFFFFF);
            graphics.pose().popPose();

            i++;
        }
    }

    private void renderPokemonSprite(GuiGraphics graphics, Pokemon pokemon, int x, int y, float scale) {
        if (pokemon == null) return;

        try {
            ItemStack stack = BreedingDisplay.spriteStack(pokemon); //Alteração
            if (stack.isEmpty()) return;

            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        } catch (Exception ignored) { }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isSelecting && !this.menu.hasPokemon()) {
            int startX = this.leftPos - 70;
            int startY = this.topPos + 20;

            ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
            int i = 0;

            for (Pokemon p : party) {
                if (p == null) { i++; continue; }
                int slotY = startY + (i * 35);

                if (mouseX >= startX && mouseX <= startX + 60 && mouseY >= slotY && mouseY <= slotY + 32) {

                    ClientPlayNetworking.send(new MachineActionPayload(1, this.menu.getBlockPos(), p.getUuid()));

                    this.isSelecting = false;
                    return true;
                }
                i++;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}