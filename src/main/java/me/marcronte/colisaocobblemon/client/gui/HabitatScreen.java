package me.marcronte.colisaocobblemon.client.gui;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.blaze3d.systems.RenderSystem;
import me.marcronte.colisaocobblemon.features.breeding.habitat.BreedingHabitatBlockEntity;
import me.marcronte.colisaocobblemon.features.breeding.habitat.HabitatMenu;
import me.marcronte.colisaocobblemon.network.payloads.HabitatPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HabitatScreen extends AbstractContainerScreen<HabitatMenu> {

    private static final ResourceLocation BG_NOT_READY = ResourceLocation.parse("colisao-cobblemon:textures/gui/breeding_habitat.png");
    private static final ResourceLocation BG_READY = ResourceLocation.parse("colisao-cobblemon:textures/gui/breeding_habitat_ready.png");

    private UUID motherId;
    private UUID fatherId;
    private int selectingSlot = -1;

    // Pais exibidos enquanto o breeding está ativo (já não estão mais no party do jogador).
    private Pokemon activeMother;
    private Pokemon activeFather;

    private int selectingBerryForSlot = -1;
    private int berryPage = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private final List<ItemStack> availableBerries = new ArrayList<>();

    public HabitatScreen(HabitatMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;

        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> sendAction(11, null, ""))
                .bounds(x + 15, y + 210, 15, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Berry"), btn -> openBerrySelector(0))
                .bounds(x + 32, y + 210, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> sendAction(10, null, ""))
                .bounds(x + 74, y + 210, 15, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> sendAction(14, null, ""))
                .bounds(x + 167, y + 210, 15, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Berry"), btn -> openBerrySelector(1))
                .bounds(x + 184, y + 210, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> sendAction(13, null, ""))
                .bounds(x + 226, y + 210, 15, 20).build());
    }

    private void sendAction(int actionId, UUID pokemonId, String berryId) {
        ClientPlayNetworking.send(new HabitatPayloads.HabitatActionPayload(actionId, pokemonId != null ? pokemonId : new UUID(0,0), new UUID(0,0), berryId));
    }

    private void openBerrySelector(int slot) {
        this.selectingBerryForSlot = slot;
        this.berryPage = 0;
        this.availableBerries.clear();

        assert Minecraft.getInstance().player != null;
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem().toString().contains("berry")) {
                boolean alreadyHas = false;
                for (ItemStack b : availableBerries) {
                    if (ItemStack.isSameItem(b, stack)) { alreadyHas = true; break; }
                }
                if (!alreadyHas) availableBerries.add(stack.copy());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.isActive()) {
            renderTimer(graphics, x, y);
        } else {
            int points = previewHabitatPoints();
            graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.habitat_points", points), x + 128, y + 20, 0x44FF44);

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
            graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.breeding_breed"), x + 128, y + 193, 0xFFFFFF);
        }

        graphics.drawCenteredString(font, Component.translatable("gui.colisao-cobblemon.claim"), x + 127, y + 153, 0xFFFFFF);

        int fuel = this.menu.getFuel();
        if (fuel > 0) {
            String fuelText = formatTicksToTime(fuel);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 0);
            graphics.pose().scale(0.85f, 0.85f, 1.0f);
            graphics.drawCenteredString(font, fuelText, (int)((x + 52) * 1.176f), (int)((y + 177) * 1.176f), 0xFFAAAA);
            graphics.drawCenteredString(font, fuelText, (int)((x + 205) * 1.176f), (int)((y + 177) * 1.176f), 0xFFAAAA);
            graphics.pose().popPose();
        } else if (this.menu.isActive() && this.menu.getRequired() != -1 && this.menu.getProgress() < this.menu.getRequired()) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.85f, 0.85f, 1.0f);
            graphics.drawCenteredString(font, "Sem Berry!", (int)((x + 52) * 1.176f), (int)((y + 177) * 1.176f), 0xFF4444);
            graphics.drawCenteredString(font, "Sem Berry!", (int)((x + 205) * 1.176f), (int)((y + 177) * 1.176f), 0xFF4444);
            graphics.pose().popPose();
        }

        if (selectingSlot == 0) graphics.fill(x + 25, y + 145, x + 79, y + 169, 0x80009BAD);
        if (selectingSlot == 1) graphics.fill(x + 177, y + 145, x + 230, y + 168, 0x80009BAD);

        if (selectingSlot != -1) {
            renderPartyList(graphics, mouseX, mouseY, x - 70, y + 20);
        }

        if (selectingBerryForSlot != -1) {
            renderBerrySelector(graphics, mouseX, mouseY);
        }
    }

    private void renderBerrySelector(GuiGraphics graphics, int mouseX, int mouseY) {
        int maxPages = (int) Math.ceil(availableBerries.size() / (double) ITEMS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;

        int boxWidth = 240;
        int boxHeight = 40;
        int boxX = this.leftPos + (256 - boxWidth) / 2;
        int boxY = this.topPos + 100;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xEE000000);
        graphics.drawCenteredString(this.font, "§fEscolha a Berry (" + (berryPage + 1) + "/" + maxPages + "):", boxX + boxWidth / 2, boxY + 5, 0xFFFFFF);

        if (availableBerries.isEmpty()) {
            graphics.drawCenteredString(this.font, "§cNenhuma berry no inventário!", boxX + boxWidth / 2, boxY + 22, 0xFFFFFF);
            return;
        }

        if (berryPage > 0) {
            int arrowLX = boxX + 5;
            boolean hoverL = mouseX >= arrowLX && mouseX <= arrowLX + 15 && mouseY >= boxY + 18 && mouseY <= boxY + 34;
            graphics.drawString(this.font, "<", arrowLX + 4, boxY + 22, hoverL ? 0x00FF00 : 0xFFFFFF, false);
        }

        if (berryPage < maxPages - 1) {
            int arrowRX = boxX + boxWidth - 15;
            boolean hoverR = mouseX >= arrowRX && mouseX <= arrowRX + 15 && mouseY >= boxY + 18 && mouseY <= boxY + 34;
            graphics.drawString(this.font, ">", arrowRX + 4, boxY + 22, hoverR ? 0x00FF00 : 0xFFFFFF, false);
        }

        int startIndex = berryPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableBerries.size());

        for (int i = startIndex; i < endIndex; i++) {
            int bx = boxX + 22 + ((i - startIndex) * 20);
            int by = boxY + 18;

            ItemStack berryItem = availableBerries.get(i);
            graphics.renderFakeItem(berryItem, bx, by);

            if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                graphics.fill(bx, by, bx + 16, by + 16, 0x55FFFFFF);
                graphics.renderTooltip(this.font, berryItem.getHoverName(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        boolean isReady = this.menu.isActive() && this.menu.getProgress() >= this.menu.getRequired() && this.menu.getRequired() > 0;
        ResourceLocation bg = isReady ? BG_READY : BG_NOT_READY;
        graphics.blit(bg, this.leftPos, this.topPos, 0, 0, 256, 256, 256, 256);

        Pokemon mother = resolveDisplayedPokemon(true);
        if (mother != null) {
            renderPokemonSprite(graphics, mother, this.leftPos + 30, this.topPos + 70, 3.0f);
            graphics.drawCenteredString(font, mother.getSpecies().getName(), this.leftPos + 51, this.topPos + 115, 0xFFFFFF);
        }

        Pokemon father = resolveDisplayedPokemon(false);
        if (father != null) {
            renderPokemonSprite(graphics, father, this.leftPos + 182, this.topPos + 70, 3.0f);
            graphics.drawCenteredString(font, father.getSpecies().getName(), this.leftPos + 203, this.topPos + 115, 0xFFFFFF);
        }
    }

    /**
     * Com o breeding ativo, os pais já saíram do party: eles são lidos do block entity do Habitat
     * (que o servidor sincroniza com o cliente). Antes de iniciar, usa os Pokémon escolhidos no party.
     */
    private Pokemon resolveDisplayedPokemon(boolean isMother) {
        if (!this.menu.isActive()) {
            return getClientSidePokemon(isMother ? motherId : fatherId);
        }

        Pokemon cached = isMother ? activeMother : activeFather;
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || this.menu.blockPos == null) return null;
        if (!(mc.level.getBlockEntity(this.menu.blockPos) instanceof BreedingHabitatBlockEntity habitat)) return null;

        CompoundTag data = isMother ? habitat.getMotherData() : habitat.getFatherData();
        if (data == null || data.isEmpty()) return null;

        Pokemon built = BreedingDisplay.fromData(data, data.getString("UI_Species"));
        if (isMother) activeMother = built; else activeFather = built;
        return built;
    }

    private void renderTimer(GuiGraphics graphics, int x, int y) {
        int progressSeconds = this.menu.getProgress();
        int requiredSeconds = this.menu.getRequired();

        if (requiredSeconds == -1) {
            graphics.drawCenteredString(font, "Faltam Blocos de Habitat!", x + 128, y + 45, 0xFF4444);
            return;
        }

        int remainingSeconds = requiredSeconds - progressSeconds;
        if (remainingSeconds < 0) remainingSeconds = 0;

        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        String timeText = (hours > 0) ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);

        int color = (remainingSeconds <= 0) ? 0x00FF00 : 0xFFFFFF;
        graphics.drawCenteredString(font, timeText, x + 128, y + 45, color);
    }

    private String formatTicksToTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0m 0s";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) return String.format("%dh %dm", hours, minutes);
        return String.format("%dm %ds", minutes, seconds);
    }

    private void renderPartyList(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY) {
        ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
        int i = 0;
        graphics.drawString(font, "Party:", startX, startY - 15, 0xFFFFFF);
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
            graphics.drawString(font, p.getSpecies().getName(), 0, 0, 0xFFFFFF);
            graphics.pose().popPose();
            i++;
        }
    }

    private Pokemon getClientSidePokemon(UUID uuid) {
        if (uuid == null) return null;
        for (Pokemon p : CobblemonClient.INSTANCE.getStorage().getParty()) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = this.leftPos;
        int y = this.topPos;

        if (selectingBerryForSlot != -1) {
            int maxPages = (int) Math.ceil(availableBerries.size() / (double) ITEMS_PER_PAGE);
            if (maxPages == 0) maxPages = 1;
            int boxWidth = 240;
            int boxX = x + (256 - boxWidth) / 2;
            int boxY = y + 100;

            if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + 40) {
                selectingBerryForSlot = -1;
                return true;
            }

            if (berryPage > 0 && mouseX >= boxX + 5 && mouseX <= boxX + 20 && mouseY >= boxY + 18 && mouseY <= boxY + 34) {
                berryPage--; return true;
            }
            if (berryPage < maxPages - 1 && mouseX >= boxX + boxWidth - 15 && mouseX <= boxX + boxWidth && mouseY >= boxY + 18 && mouseY <= boxY + 34) {
                berryPage++; return true;
            }

            int startIndex = berryPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableBerries.size());

            for (int i = startIndex; i < endIndex; i++) {
                int bx = boxX + 22 + ((i - startIndex) * 20);
                int by = boxY + 18;

                if (mouseX >= bx && mouseX <= bx + 16 && mouseY >= by && mouseY <= by + 16) {
                    String itemId = BuiltInRegistries.ITEM.getKey(availableBerries.get(i).getItem()).toString();
                    int actionId = (selectingBerryForSlot == 0) ? 12 : 15;
                    sendAction(actionId, null, itemId);
                    selectingBerryForSlot = -1;
                    return true;
                }
            }
            return true;
        }

        if (selectingSlot != -1) {
            int startX = x - 70;
            int startY = y + 20;
            int i = 0;
            for (Pokemon p : CobblemonClient.INSTANCE.getStorage().getParty()) {
                if (p == null) { i++; continue; }
                int slotY = startY + (i * 35);
                if (mouseX >= startX && mouseX <= startX + 60 && mouseY >= slotY && mouseY <= slotY + 32) {
                    Gender gender = p.getGender();
                    if (selectingSlot == 0 && gender == Gender.MALE) {
                        assert Minecraft.getInstance().player != null;
                        Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.colisao-cobblemon.female_slot").withStyle(ChatFormatting.RED), true);
                        return true;
                    } else if (selectingSlot == 1 && gender == Gender.FEMALE) {
                        assert Minecraft.getInstance().player != null;
                        Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.colisao-cobblemon.male_slot").withStyle(ChatFormatting.RED), true);
                        return true;
                    }
                    if (selectingSlot == 0) motherId = p.getUuid();
                    else fatherId = p.getUuid();
                    selectingSlot = -1;
                    return true;
                }
                i++;
            }
        }

        double relX = mouseX - x;
        double relY = mouseY - y;

        if (relX >= 25 && relX <= 78 && relY >= 145 && relY <= 168 && !this.menu.isActive()) { selectingSlot = (selectingSlot == 0) ? -1 : 0; return true; }
        if (relX >= 177 && relX <= 230 && relY >= 145 && relY <= 168 && !this.menu.isActive()) { selectingSlot = (selectingSlot == 1) ? -1 : 1; return true; }

        if (relX >= 101 && relX <= 154 && relY >= 145 && relY <= 168) {
            boolean isReady = this.menu.isActive() && this.menu.getProgress() >= this.menu.getRequired() && this.menu.getRequired() > 0;
            if (isReady) {
                ClientPlayNetworking.send(new HabitatPayloads.HabitatActionPayload(3, new UUID(0,0), new UUID(0,0), ""));
                return true;
            }
        }

        if (relX >= 101 && relX <= 154 && relY >= 185 && relY <= 208 && !this.menu.isActive()) {
            if (motherId != null && fatherId != null) {
                ClientPlayNetworking.send(new HabitatPayloads.HabitatActionPayload(2, motherId, fatherId, ""));
            } else {
                assert Minecraft.getInstance().player != null;
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§cSelecione os dois Pokémon primeiro!"), true);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int previewHabitatPoints() {
        if (motherId == null && fatherId == null) return 0;
        String mType = "normal";
        String fType = "normal";

        if (motherId != null) {
            Pokemon m = getClientSidePokemon(motherId);
            if (m != null) mType = m.getSpecies().getPrimaryType().getName();
        }
        if (fatherId != null) {
            Pokemon f = getClientSidePokemon(fatherId);
            if (f != null) fType = f.getSpecies().getPrimaryType().getName();
        }

        int points = 0;
        net.minecraft.world.level.Level level = Minecraft.getInstance().level;
        BlockPos center = this.menu.blockPos;

        if (level != null && center != null) {
            int y = center.getY() - 1;
            for (int x = center.getX() - 3; x <= center.getX() + 3; x++) {
                for (int z = center.getZ() - 3; z <= center.getZ() + 3; z++) {
                    net.minecraft.world.level.block.Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    int valM = me.marcronte.colisaocobblemon.features.breeding.habitat.HabitatDictionary.getBlockValueForTypes(block, mType, "normal");
                    int valF = me.marcronte.colisaocobblemon.features.breeding.habitat.HabitatDictionary.getBlockValueForTypes(block, fType, "normal");
                    points += Math.max(valM, valF);
                }
            }
        }
        return points;
    }
}