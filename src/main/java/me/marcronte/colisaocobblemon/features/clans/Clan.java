package me.marcronte.colisaocobblemon.features.clans;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Clan {
    private final String name;
    private String tag;
    private String tagColor;
    private String primaryType;
    private String secondaryType;

    private int level;
    private int xp;

    private BlockPos homePos;
    private String homeDimension;

    private final Map<UUID, ClanMember> members = new HashMap<>();

    private final SimpleContainer chest = new SimpleContainer(54);

    // --- WEEKLYS ---
    private int defeatedCount = 0;
    private int defeatedTypeCount = 0;
    private int caughtCount = 0;
    private int caughtTypeCount = 0;
    private int hatchedCount = 0;

    private boolean defeatedDone = false;
    private boolean defeatedTypeDone = false;
    private boolean caughtDone = false;
    private boolean caughtTypeDone = false;
    private boolean hatchedDone = false;

    private long bonusEndTime = 0;

    public Clan(String name, String tag, String tagColor, String primaryType, String secondaryType, UUID ownerUuid, String ownerName) {
        this.name = name;
        this.tag = tag;
        this.tagColor = tagColor;
        this.primaryType = primaryType;
        this.secondaryType = secondaryType;
        this.level = 1;
        this.xp = 0;

        this.members.put(ownerUuid, new ClanMember(ownerUuid, ownerName, ClanRank.OWNER));
    }

    private Clan(String name) {
        this.name = name;
    }

    public void addXp(int amount) {
        this.xp += amount;

        while (this.level < 5 && this.xp >= getXpNeededForNextLevel()) {
            this.xp -= getXpNeededForNextLevel();
            this.level++;
        }
    }


    // --- GETTERS E SETTERS ---
    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String getTagColor() {
        return tagColor;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public String getSecondaryType() {
        return secondaryType;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public String getHomeDimension() {
        return homeDimension;
    }

    public Map<UUID, ClanMember> getMembers() {
        return members;
    }

    public SimpleContainer getChest() {
        return chest;
    }

    public long getBonusEndTime() {
        return bonusEndTime;
    }

    public void setBonusEndTime(long bonusEndTime) {
        this.bonusEndTime = bonusEndTime;
    }

    public int getXpNeededForNextLevel() {
        return switch (this.level) {
            case 1 -> 500;
            case 2 -> 1000;
            case 3 -> 1500;
            case 4 -> 2000;
            default -> 0;
        };
    }

    public int getTargetDefeat() {
        int x = Math.max(1, members.size());
        return 4 * (x - 1) * (x - 1) + 500;
    }

    public int getTargetDefeatType() {
        return getTargetDefeat() / 2;
    }

    public int getTargetCatch() {
        int x = Math.max(1, members.size());
        return 2 * (x - 1) * (x - 1) + 100;
    }

    public int getTargetCatchType() {
        return getTargetCatch() / 2;
    }

    public int getTargetHatch() {
        int x = Math.max(1, members.size());
        return 4 * (x - 1) + 20;
    }

    public int getDefeatedCount() {
        return defeatedCount;
    }

    public int getDefeatedTypeCount() {
        return defeatedTypeCount;
    }

    public int getCaughtCount() {
        return caughtCount;
    }

    public int getCaughtTypeCount() {
        return caughtTypeCount;
    }

    public int getHatchedCount() {
        return hatchedCount;
    }

    public void setHome(BlockPos pos, String dimension) {
        this.homePos = pos;
        this.homeDimension = dimension;
    }

    // --- MISSIONS PROGRESS ---
    public void progressDefeat(net.minecraft.server.level.ServerLevel level, boolean isClanType) {
        if (!defeatedDone) {
            defeatedCount++;
            if (defeatedCount >= getTargetDefeat())
                completeMission(level, "Derrotar " + getTargetDefeat() + " Pokémon", 1);
        }
        if (isClanType && !defeatedTypeDone) {
            defeatedTypeCount++;
            if (defeatedTypeCount >= getTargetDefeatType())
                completeMission(level, "Derrotar " + getTargetDefeatType() + " Pokémon dos Tipos do Clã", 2);
        }
    }

    public void progressCatch(net.minecraft.server.level.ServerLevel level, boolean isClanType) {
        if (!caughtDone) {
            caughtCount++;
            if (caughtCount >= getTargetCatch()) completeMission(level, "Capturar " + getTargetCatch() + " Pokémon", 3);
        }
        if (isClanType && !caughtTypeDone) {
            caughtTypeCount++;
            if (caughtTypeCount >= getTargetCatchType())
                completeMission(level, "Capturar " + getTargetCatchType() + " Pokémon dos Tipos do Clã", 4);
        }
    }

    public void progressHatch(net.minecraft.server.level.ServerLevel level) {
        if (!hatchedDone) {
            hatchedCount++;
            if (hatchedCount >= getTargetHatch()) completeMission(level, "Chocar " + getTargetHatch() + " Ovos", 5);
        }
    }

    private void completeMission(net.minecraft.server.level.ServerLevel level, String name, int missionId) {
        switch (missionId) {
            case 1 -> defeatedDone = true;
            case 2 -> defeatedTypeDone = true;
            case 3 -> caughtDone = true;
            case 4 -> caughtTypeDone = true;
            case 5 -> hatchedDone = true;
        }

        addXp(143);
        ClanSavedData.get(level).setDirty();

        for (ClanMember m : members.values()) {
            net.minecraft.server.level.ServerPlayer p = level.getServer().getPlayerList().getPlayer(m.getUuid());
            if (p != null) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l[CLAN] §aMissão Concluída: §f" + name + " §b(+143 XP)"));
            }
        }
    }

    public void applyDecay() {
        this.xp = Math.max(0, (this.xp - 200));
        this.resetWeeklyMissions();
    }

    public void resetWeeklyMissions() {
        defeatedCount = 0;
        defeatedTypeCount = 0;
        caughtCount = 0;
        caughtTypeCount = 0;
        hatchedCount = 0;
        defeatedDone = false;
        defeatedTypeDone = false;
        caughtDone = false;
        caughtTypeDone = false;
        hatchedDone = false;
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        root.putString("Name", name);
        root.putString("Tag", tag);
        root.putString("TagColor", tagColor);
        root.putString("PrimaryType", primaryType);
        root.putString("SecondaryType", secondaryType);
        root.putInt("Level", level);
        root.putInt("Xp", xp);

        if (homePos != null) {
            root.put("HomePos", NbtUtils.writeBlockPos(homePos));
            root.putString("HomeDim", homeDimension);
        }

        ListTag memberList = new ListTag();
        for (ClanMember m : members.values()) {
            memberList.add(m.toNbt());
        }
        root.put("Members", memberList);

        ListTag chestItems = new ListTag();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                chestItems.add(stack.save(provider, itemTag));
            }
        }
        root.put("ChestItems", chestItems);

        root.putInt("M_DefeatC", defeatedCount);
        root.putInt("M_DefeatTC", defeatedTypeCount);
        root.putInt("M_CatchC", caughtCount);
        root.putInt("M_CatchTC", caughtTypeCount);
        root.putInt("M_HatchC", hatchedCount);
        root.putBoolean("M_DefeatD", defeatedDone);
        root.putBoolean("M_DefeatTD", defeatedTypeDone);
        root.putBoolean("M_CatchD", caughtDone);
        root.putBoolean("M_CatchTD", caughtTypeDone);
        root.putBoolean("M_HatchD", hatchedDone);
        root.putLong("XpBonusEndTime", bonusEndTime);

        return root;
    }

    public static Clan load(CompoundTag root, HolderLookup.Provider provider) {
        Clan clan = new Clan(root.getString("Name"));
        clan.tag = root.getString("Tag");
        clan.tagColor = root.getString("TagColor");
        clan.primaryType = root.getString("PrimaryType");
        clan.secondaryType = root.getString("SecondaryType");
        clan.level = root.getInt("Level");
        clan.xp = root.getInt("Xp");

        if (root.contains("HomePos")) {
            clan.homePos = NbtUtils.readBlockPos(root, "HomePos").orElse(null);
            clan.homeDimension = root.getString("HomeDim");
        }

        ListTag memberList = root.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            ClanMember m = ClanMember.fromNbt(memberList.getCompound(i));
            clan.members.put(m.getUuid(), m);
        }

        ListTag chestItems = root.getList("ChestItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < chestItems.size(); i++) {
            CompoundTag itemTag = chestItems.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < clan.chest.getContainerSize()) {
                clan.chest.setItem(slot, ItemStack.parse(provider, itemTag).orElse(ItemStack.EMPTY));
            }
        }

        clan.defeatedCount = root.getInt("M_DefeatC");
        clan.defeatedTypeCount = root.getInt("M_DefeatTC");
        clan.caughtCount = root.getInt("M_CatchC");
        clan.caughtTypeCount = root.getInt("M_CatchTC");
        clan.hatchedCount = root.getInt("M_HatchC");
        clan.defeatedDone = root.getBoolean("M_DefeatD");
        clan.defeatedTypeDone = root.getBoolean("M_DefeatTD");
        clan.caughtDone = root.getBoolean("M_CatchD");
        clan.caughtTypeDone = root.getBoolean("M_CatchTD");
        clan.hatchedDone = root.getBoolean("M_HatchD");
        clan.bonusEndTime = root.getLong("XpBonusEndTime");

        return clan;
    }
}