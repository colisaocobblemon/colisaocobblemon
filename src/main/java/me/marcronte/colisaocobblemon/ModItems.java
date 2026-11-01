package me.marcronte.colisaocobblemon;

import me.marcronte.colisaocobblemon.features.badgecase.BadgeCaseItem;
import me.marcronte.colisaocobblemon.features.hms.FlashItem;
import me.marcronte.colisaocobblemon.features.items.*;
import me.marcronte.colisaocobblemon.features.items.backpack.BackpackItem;
import me.marcronte.colisaocobblemon.features.routes.RouteToolItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;


import static me.marcronte.colisaocobblemon.ModBlocks.POKE_LURER;

public class ModItems {

    public static float normalChance = 30.0f;
    public static float rareChance = 3.0f;


    // --- ADMIN ITEMS ---
    public static final Item ROUTE_TOOL = registerItem("route_tool", new RouteToolItem(new Item.Properties().stacksTo(1)));

    // --- BACKPACKs ---
    public static final Item PIKACHU_BACKPACK = registerItem("pikachu_backpack", new BackpackItem(18, new Item.Properties()));
    public static final Item GRENINJA_BACKPACK = registerItem("greninja_backpack", new BackpackItem(27, new Item.Properties()));
    public static final Item GARCHOMP_BACKPACK = registerItem("garchomp_backpack", new BackpackItem(36, new Item.Properties()));
    public static final Item DARKRAI_BACKPACK = registerItem("darkrai_backpack", new BackpackItem(45, new Item.Properties()));
    public static final Item RAYQUAZA_BACKPACK = registerItem("rayquaza_backpack", new BackpackItem(54, new Item.Properties()));

    // --- KEY ITEMS ---
    public static final Item KANTO_BADGE_CASE = registerItem("kanto_badge_case", new BadgeCaseItem(new Item.Properties().stacksTo(1)));
    public static final Item RUNNING_SHOES = registerItem("running_shoes", new RunningShoesItem(new Item.Properties().stacksTo(1)));
    public static final Item CARD_KEY = registerItem("card_key", new Item(new Item.Properties().stacksTo(1)));
    public static final Item COIN_CASE = registerItem("coin_case", new Item(new Item.Properties().stacksTo(1)));
    public static final Item GOLDEN_TEETH = registerItem("golden_teeth", new Item(new Item.Properties().stacksTo(1)));
    public static final Item LIFT_KEY = registerItem("lift_key", new Item(new Item.Properties().stacksTo(1)));
    public static final Item OAK_PARCEL = registerItem("oak_parcel", new Item(new Item.Properties().stacksTo(1)));
    public static final Item POKE_FLUTE = registerItem("poke_flute", new Item(new Item.Properties().stacksTo(1)));
    public static final Item SECRET_KEY = registerItem("secret_key", new Item(new Item.Properties().stacksTo(1)));
    public static final Item SILPH_SCOPE = registerItem("silph_scope", new Item(new Item.Properties().stacksTo(1)));
    public static final Item SS_TICKET = registerItem("ss_ticket", new Item(new Item.Properties().stacksTo(1)));
    public static final Item TEA = registerItem("tea", new Item(new Item.Properties().stacksTo(1)));

    // --- ITEMS ---
    public static final Item QUEST_BOOK = registerItem("quest_book", new QuestBookItem(new Item.Properties().stacksTo(1)));
    public static final Item POKEMON_PICKAXE = registerItem("pokemon_pickaxe", new PokemonPickaxeItem(Tiers.IRON, new Item.Properties().stacksTo(1)));
    public static final Item ETERNATITE_SCRAP = registerItem("eternatite_scrap", new Item(new Item.Properties().fireResistant()));
    public static final Item ETERNATITE_INGOT = registerItem("eternatite_ingot", new Item(new Item.Properties().fireResistant()));
    public static final Item TERASTALITE_CRYSTAL = registerItem("terastalite_crystal", new Item(new Item.Properties()));
    public static final Item AWAKENED_TERASTALITE = registerItem("awakened_terastalite", new Item(new Item.Properties().rarity(Rarity.RARE)));

    // --- BLOCKS ---
    public static final Item MINING_BLOCK_ITEM = registerItem("mining_block", new BlockItem(ModBlocks.MINING_BLOCK, new Item.Properties()));
    public static final Item ETERNATITE_ORE_ITEM = registerItem("eternatite_ore", new BlockItem(ModBlocks.ETERNATITE_ORE, new Item.Properties()));
    public static final Item TERASTALITE_ORE_ITEM = registerItem("terastalite_ore", new BlockItem(ModBlocks.TERASTALITE_ORE, new Item.Properties()));

    // ARMORs
    public static final Item ETERNATITE_HELMET = registerItem("eternatite_helmet",
            new EternatiteArmorItem(
                    ArmorMaterials.NETHERITE,
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .durability(700)
            )
    );
    public static final Item ETERNATITE_CHESTPLATE = registerItem("eternatite_chestplate",
            new EternatiteArmorItem(
                    ArmorMaterials.NETHERITE,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .durability(700)
            )
    );
    public static final Item ETERNATITE_LEGGINGS = registerItem("eternatite_leggings",
            new EternatiteArmorItem(
                    ArmorMaterials.NETHERITE,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .durability(700)
            )
    );
    public static final Item ETERNATITE_BOOTS = registerItem("eternatite_boots",
            new EternatiteArmorItem(
                    ArmorMaterials.NETHERITE,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties()
                            .stacksTo(1)
                            .fireResistant()
                            .durability(700)
            )
    );

    // TOOLs
    public static final Item ETERNATITE_SWORD = registerItem("eternatite_sword",
            new SwordItem(EternatiteTier.INSTANCE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(EternatiteTier.INSTANCE, 3, -2.4f))));

    public static final Item ETERNATITE_PICKAXE = registerItem("eternatite_pickaxe",
            new PickaxeItem(EternatiteTier.INSTANCE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(EternatiteTier.INSTANCE, 1, -2.8f))));

    public static final Item ETERNATITE_AXE = registerItem("eternatite_axe",
            new AxeItem(EternatiteTier.INSTANCE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(EternatiteTier.INSTANCE, 5.0f, -3.0f))));

    public static final Item ETERNATITE_SHOVEL = registerItem("eternatite_shovel",
            new ShovelItem(EternatiteTier.INSTANCE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(EternatiteTier.INSTANCE, 1.5f, -3.0f))));

    public static final Item ETERNATITE_HOE = registerItem("eternatite_hoe",
            new HoeItem(EternatiteTier.INSTANCE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(EternatiteTier.INSTANCE, -4.0f, 0.0f))));

    public static final Item BERRY_CRUSHER = registerItem("berry_crusher",
            new BerryCrusherItem(new Item.Properties())
    );
    public static final Item POKE_LENS = registerItem("poke_lens", new PokeLens(new Item.Properties().stacksTo(1)));


    // HMs
    public static final Item SURF = registerItem("surf", new Item(new Item.Properties().stacksTo(1)));
    public static final Item FLASH_HM = registerItem("flash", new FlashItem(new Item.Properties().stacksTo(1)));

    // --- BADGES (Kanto) ---
    public static final Item KANTO_BOULDER_BADGE = registerItem("kanto_boulder_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_CASCADE_BADGE = registerItem("kanto_cascade_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_THUNDER_BADGE = registerItem("kanto_thunder_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_RAINBOW_BADGE = registerItem("kanto_rainbow_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_SOUL_BADGE = registerItem("kanto_soul_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_MARSH_BADGE = registerItem("kanto_marsh_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_VOLCANO_BADGE = registerItem("kanto_volcano_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_EARTH_BADGE = registerItem("kanto_earth_badge", new Item(new Item.Properties().stacksTo(1)));
    public static final Item KANTO_CHAMPION_BADGE = registerItem("kanto_champion_badge", new Item(new Item.Properties().stacksTo(1)));

    // --- INCENSES (Breeding) ---
    public static final Item FULL_INCENSE = registerItem("full_incense", new Item(new Item.Properties()));
    public static final Item LAX_INCENSE = registerItem("lax_incense", new Item(new Item.Properties()));
    public static final Item LUCK_INCENSE = registerItem("luck_incense", new Item(new Item.Properties()));
    public static final Item ODD_INCENSE = registerItem("odd_incense", new Item(new Item.Properties()));
    public static final Item PURE_INCENSE = registerItem("pure_incense", new Item(new Item.Properties()));
    public static final Item ROCK_INCENSE = registerItem("rock_incense", new Item(new Item.Properties()));
    public static final Item ROSE_INCENSE = registerItem("rose_incense", new Item(new Item.Properties()));
    public static final Item SEA_INCENSE = registerItem("sea_incense", new Item(new Item.Properties()));
    public static final Item WAVE_INCENSE = registerItem("wave_incense", new Item(new Item.Properties()));

    // --- POKEMON DROPS ---
    public static final Item FIBER = registerItem("fiber", new PokemonDropItem("normal", normalChance, new Item.Properties()));
    public static final Item NORMALIA = registerItem("normalia", new PokemonDropItem("normal", rareChance, new Item.Properties()));

    public static final Item MAGMA_CHARCOAL = registerItem("magma_charcoal", new PokemonDropItem("fire", normalChance, new Item.Properties()));
    public static final Item SUPERNOVA_CORE = registerItem("supernova_core", new PokemonDropItem("fire", rareChance, new Item.Properties()));

    public static final Item PURIFIED_WATER = registerItem("purified_water", new PokemonDropItem("water", normalChance, new Item.Properties()));
    public static final Item SCALE = registerItem("scale", new PokemonDropItem("water", rareChance, new Item.Properties()));

    public static final Item SCREW = registerItem("screw", new PokemonDropItem("electric", normalChance, new Item.Properties()));
    public static final Item FLASHING_LIGHTNING = registerItem("flashing_lightning", new PokemonDropItem("electric", rareChance, new Item.Properties()));

    public static final Item LEAF = registerItem("leaf", new PokemonDropItem("grass", normalChance, new Item.Properties()));
    public static final Item NATURE_ESSENCE = registerItem("nature_essence", new PokemonDropItem("grass", rareChance, new Item.Properties()));

    public static final Item THIN_ICE = registerItem("thin_ice", new PokemonDropItem("ice", normalChance, new Item.Properties()));
    public static final Item ETERNAL_SNOW_FLAKE = registerItem("eternal_snow_flake", new PokemonDropItem("ice", rareChance, new Item.Properties()));

    public static final Item SCROLL = registerItem("scroll", new PokemonDropItem("fighting", normalChance, new Item.Properties()));
    public static final Item COMBAT_SPIRIT = registerItem("combat_spirit", new PokemonDropItem("fighting", rareChance, new Item.Properties()));

    public static final Item VENOM = registerItem("venom", new PokemonDropItem("poison", normalChance, new Item.Properties()));
    public static final Item VENOM_BARB = registerItem("venom_barb", new PokemonDropItem("poison", rareChance, new Item.Properties()));

    public static final Item MUD = registerItem("mud", new PokemonDropItem("ground", normalChance, new Item.Properties()));
    public static final Item DENSE_DIRT = registerItem("dense_dirt", new PokemonDropItem("ground", rareChance, new Item.Properties()));

    public static final Item LIGHT_FEATHER = registerItem("light_feather", new PokemonDropItem("flying", normalChance, new Item.Properties()));
    public static final Item SKY_CORE = registerItem("sky_core", new PokemonDropItem("flying", rareChance, new Item.Properties()));

    public static final Item SPOON = registerItem("spoon", new PokemonDropItem("psychic", normalChance, new Item.Properties()));
    public static final Item MYSTIC_ARTIFACT = registerItem("mystic_artifact", new PokemonDropItem("psychic", rareChance, new Item.Properties()));

    public static final Item SEED = registerItem("seed", new PokemonDropItem("bug", normalChance, new Item.Properties()));
    public static final Item HORN = registerItem("horn", new PokemonDropItem("bug", rareChance, new Item.Properties()));

    public static final Item ROCK = registerItem("rock", new PokemonDropItem("rock", normalChance, new Item.Properties()));
    public static final Item SANDBAG = registerItem("sandbag", new PokemonDropItem("rock", rareChance, new Item.Properties()));

    public static final Item DARK_ESSENCE = registerItem("dark_essence", new PokemonDropItem("ghost", normalChance, new Item.Properties()));
    public static final Item SOUL = registerItem("soul", new PokemonDropItem("ghost", rareChance, new Item.Properties()));

    public static final Item DRAGON_WING = registerItem("dragon_wing", new PokemonDropItem("dragon", normalChance, new Item.Properties()));
    public static final Item DRAGON_FANG = registerItem("dragon_fang", new PokemonDropItem("dragon", rareChance, new Item.Properties()));

    public static final Item DARK_POWDER = registerItem("dark_powder", new PokemonDropItem("dark", normalChance, new Item.Properties()));
    public static final Item DARK_ORB = registerItem("dark_orb", new PokemonDropItem("dark", rareChance, new Item.Properties()));

    public static final Item COINS = registerItem("coins", new PokemonDropItem("steel", normalChance, new Item.Properties()));
    public static final Item CONTROLLER = registerItem("controller", new PokemonDropItem("steel", rareChance, new Item.Properties()));

    public static final Item ENHANCED_POWDER = registerItem("enhanced_powder", new PokemonDropItem("fairy", normalChance, new Item.Properties()));
    public static final Item FAIRY_SOUL = registerItem("fairy_soul", new PokemonDropItem("fairy", rareChance, new Item.Properties()));


    public static final Item POKE_LURER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ColisaoCobblemon.MOD_ID, "poke_lurer"),
            new BlockItem(POKE_LURER, new Item.Properties())
    );

    // Leaves
    public static final Item BABIRI_LEAVES = registerItem("babiri_leaves", new Item(new Item.Properties()));
    public static final Item CHARTI_LEAVES = registerItem("charti_leaves", new Item(new Item.Properties()));
    public static final Item CHILAN_LEAVES = registerItem("chilan_leaves", new Item(new Item.Properties()));
    public static final Item CHOPLE_LEAVES = registerItem("chople_leaves", new Item(new Item.Properties()));
    public static final Item COBA_LEAVES = registerItem("coba_leaves", new Item(new Item.Properties()));
    public static final Item COLBUR_LEAVES = registerItem("colbur_leaves", new Item(new Item.Properties()));
    public static final Item HABAN_LEAVES = registerItem("haban_leaves", new Item(new Item.Properties()));
    public static final Item KASIB_LEAVES = registerItem("kasib_leaves", new Item(new Item.Properties()));
    public static final Item KEBIA_LEAVES = registerItem("kebia_leaves", new Item(new Item.Properties()));
    public static final Item OCCA_LEAVES = registerItem("occa_leaves", new Item(new Item.Properties()));
    public static final Item PASSHO_LEAVES = registerItem("passho_leaves", new Item(new Item.Properties()));
    public static final Item PAYAPA_LEAVES = registerItem("payapa_leaves", new Item(new Item.Properties()));
    public static final Item RINDO_LEAVES = registerItem("rindo_leaves", new Item(new Item.Properties()));
    public static final Item ROSELI_LEAVES = registerItem("roseli_leaves", new Item(new Item.Properties()));
    public static final Item SHUCA_LEAVES = registerItem("shuca_leaves", new Item(new Item.Properties()));
    public static final Item TANGA_LEAVES = registerItem("tanga_leaves", new Item(new Item.Properties()));
    public static final Item WACAN_LEAVES = registerItem("wacan_leaves", new Item(new Item.Properties()));
    public static final Item YACHE_LEAVES = registerItem("yache_leaves", new Item(new Item.Properties()));

    // Pokemon Egg (Breeding)
    public static final Item POKEMON_EGG = registerItem("pokemon_egg", new CobblemonEggItem(new Item.Properties()));

    // KEYs
    public static final Item LEGENDARY_KEY = registerItem("legendary_key", new Item(new Item.Properties()));
    public static final Item MEGA_KEY = registerItem("mega_key", new Item(new Item.Properties()));
    public static final Item MEGA_EQUIPMENT_KEY = registerItem("mega_equipment_key", new Item(new Item.Properties()));
    public static final Item SHINY_KEY = registerItem("shiny_key", new Item(new Item.Properties()));
    public static final Item SKIN_KEY = registerItem("skin_key", new Item(new Item.Properties()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("colisao-cobblemon", name), item);
    }

    public static void registerModItems() {
        ColisaoCobblemon.LOGGER.info("Registrando itens do Colisão Cobblemon");
    }
}