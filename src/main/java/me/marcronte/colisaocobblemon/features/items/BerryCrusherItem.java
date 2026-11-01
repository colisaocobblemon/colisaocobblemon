package me.marcronte.colisaocobblemon.features.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

public class BerryCrusherItem extends Item {
    private static final int MAX_DURABILITY = 100;

    public BerryCrusherItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDamage(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int damage = getDamage(stack);
        return Math.round(13.0f - (float) damage * 13.0f / (float) MAX_DURABILITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0f, ((float) MAX_DURABILITY - (float) getDamage(stack)) / (float) MAX_DURABILITY);
        return java.awt.Color.HSBtoRGB(f / 3.0f, 1.0f, 1.0f);
    }

    private static int getDamage(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getInt("CrusherDamage");
    }

    private static void setDamage(ItemStack stack, int damage) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("CrusherDamage", Math.min(damage, MAX_DURABILITY));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        if (stack.getCount() > 1) {
            return ItemStack.EMPTY;
        }

        int currentDamage = getDamage(stack) + 1;

        if (currentDamage >= MAX_DURABILITY) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        setDamage(remainder, currentDamage);
        return remainder;
    }

    @Override
    public @NotNull InteractionResultHolder use(Level level, Player player, InteractionHand hand) {
        ItemStack crusher = player.getItemInHand(hand);
        int currentDamage = getDamage(crusher);

        if (currentDamage <= 0) {
            return InteractionResultHolder.pass(crusher);
        }

        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack repairMaterial = player.getItemInHand(otherHand);

        if (repairMaterial.is(Items.STICK)) {

            if (!player.isCreative()) {
                repairMaterial.shrink(1);
            }

            int newDamage = Math.max(0, currentDamage - 25);
            setDamage(crusher, newDamage);

            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.0f);

            return InteractionResultHolder.success(crusher);
        }

        return InteractionResultHolder.pass(crusher);
    }
}