package me.marcronte.colisaocobblemon.features.breeding;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityPool;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import me.marcronte.colisaocobblemon.ModItems;
import me.marcronte.colisaocobblemon.config.GeneralConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class BreedingCalculator {

    private static final Map<Item, String> INCENSE_BABIES = Map.of(
            ModItems.SEA_INCENSE, "azurill",
            ModItems.LAX_INCENSE, "wynaut",
            ModItems.ROSE_INCENSE, "budew",
            ModItems.PURE_INCENSE, "chingling",
            ModItems.ROCK_INCENSE, "bonsly",
            ModItems.ODD_INCENSE, "mimejr",
            ModItems.LUCK_INCENSE, "happiny",
            ModItems.FULL_INCENSE, "munchlax",
            ModItems.WAVE_INCENSE, "mantyke"
    );

    private static final Map<String, Stats> POWER_ITEMS_MAP = Map.of(
            "cobblemon:power_weight", Stats.HP,
            "cobblemon:power_bracer", Stats.ATTACK,
            "cobblemon:power_belt", Stats.DEFENCE,
            "cobblemon:power_lens", Stats.SPECIAL_ATTACK,
            "cobblemon:power_band", Stats.SPECIAL_DEFENCE,
            "cobblemon:power_anklet", Stats.SPEED
    );

    public static Pokemon createOffspring(Pokemon parentA, Pokemon parentB) {
        if (!canBreed(parentA, parentB)) return null;

        Pokemon mother = determineMother(parentA, parentB);
        Pokemon father = (mother == parentA) ? parentB : parentA;

        Species childSpecies = determineChildSpecies(mother, father);
        if (childSpecies == null) return null;

        List<String> startingMoves = calculateInheritedMoves(childSpecies, mother, father);

        boolean motherEverstone = isHoldingItem(mother, "cobblemon:everstone");
        boolean fatherEverstone = isHoldingItem(father, "cobblemon:everstone");

        FormData inheritedForm = null;
        String regionalAspect = null;

        if (motherEverstone && isRegionalForm(mother.getForm().getName())) {
            inheritedForm = mother.getForm();
            regionalAspect = getDormantGene(mother.getForm().getName());
        } else if (fatherEverstone && isRegionalForm(father.getForm().getName())) {
            inheritedForm = father.getForm();
            regionalAspect = getDormantGene(father.getForm().getName());
        } else if (!isRegionalForm(mother.getForm().getName()) && !mother.getForm().getName().equalsIgnoreCase("normal")) {
            inheritedForm = mother.getForm();
        }

        String motherSpeciesName = mother.getSpecies().getName().toLowerCase();
        if (motherSpeciesName.equals("sneasler") || motherSpeciesName.equals("overqwil")) {
            regionalAspect = "hisuian";
        } else if (motherSpeciesName.equals("perrserker") || motherSpeciesName.equals("sirfetchd") || motherSpeciesName.equals("mrrime") || motherSpeciesName.equals("runerigus")) {
            regionalAspect = "galarian";
        } else if (motherSpeciesName.equals("clodsire")) {
            regionalAspect = "paldean";
        }

        StringBuilder dna = new StringBuilder(childSpecies.resourceIdentifier.getPath());

        if (regionalAspect != null) {
            dna.append(" ").append(regionalAspect);
        } else if (inheritedForm != null && !inheritedForm.getName().equalsIgnoreCase("normal")) {
            dna.append(" form=").append(inheritedForm.getName().toLowerCase());
        }

        dna.append(" level=1");
        if (!startingMoves.isEmpty()) {
            dna.append(" moves=").append(String.join(",", startingMoves));
        }

        Pokemon offspring = PokemonProperties.Companion.parse(dna.toString()).create();

        if (regionalAspect != null) {
            if (!offspring.getAspects().contains(regionalAspect)) {
                offspring.getAspects().add(regionalAspect);
            }
            if (inheritedForm != null) {
                String shortFormName = inheritedForm.getName().toLowerCase();
                if (!offspring.getAspects().contains(shortFormName)) {
                    offspring.getAspects().add(shortFormName);
                }
            }
        }

        for (String aspect : mother.getAspects()) {
            String asp = aspect.toLowerCase();
            if (asp.equals("normal") ||
                    asp.matches("^(male|female|genderless|shiny|pokerus|fainted|alpha)$") ||
                    isRegionalForm(asp)) {
                continue;
            }
            if (!offspring.getAspects().contains(asp)) {
                offspring.getAspects().add(asp);
            }
        }

        inheritNature(offspring, mother, father);
        inheritAbility(offspring, mother, father);
        inheritIVs(offspring, mother, father);
        rollShiny(offspring, mother, father);

        offspring.setCaughtBall(mother.getCaughtBall());
        offspring.heal();

        return offspring;
    }

    private static boolean canBreed(Pokemon a, Pokemon b) {
        if (a.getPersistentData().getBoolean("castrado") || b.getPersistentData().getBoolean("castrado")) {
            return false;
        }

        if (a.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED) ||
                b.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) {
            return false;
        }

        boolean aIsDitto = a.getSpecies().getName().equalsIgnoreCase("ditto");
        boolean bIsDitto = b.getSpecies().getName().equalsIgnoreCase("ditto");

        if (aIsDitto && bIsDitto) return false;
        if (aIsDitto || bIsDitto) return true;

        if (a.getGender() == Gender.GENDERLESS || b.getGender() == Gender.GENDERLESS) return false;
        if (a.getGender() == b.getGender()) return false;

        Set<EggGroup> groupsA = a.getForm().getEggGroups();
        Set<EggGroup> groupsB = b.getForm().getEggGroups();

        return !Collections.disjoint(groupsA, groupsB);
    }

    private static Pokemon determineMother(Pokemon a, Pokemon b) {
        boolean aIsDitto = a.getSpecies().getName().equalsIgnoreCase("ditto");
        boolean bIsDitto = b.getSpecies().getName().equalsIgnoreCase("ditto");

        if (aIsDitto) return b;
        if (bIsDitto) return a;

        return (a.getGender() == Gender.FEMALE) ? a : b;
    }

    private static Species determineChildSpecies(Pokemon mother, Pokemon father) {
        Species current = mother.getSpecies();

        while (current.getPreEvolution() != null) {
            current = current.getPreEvolution().getSpecies();
        }
        Species baseSpecies = current;
        Species resultSpecies = baseSpecies;

        for (Map.Entry<Item, String> entry : INCENSE_BABIES.entrySet()) {
            String babyName = entry.getValue();
            Item incense = entry.getKey();

            if (baseSpecies.getName().equalsIgnoreCase(babyName)) {
                boolean motherHas = isHolding(mother, incense);
                boolean fatherHas = isHolding(father, incense);

                if (!motherHas && !fatherHas) {
                    Collection<Evolution> evos = baseSpecies.getStandardForm().getEvolutions();
                    if (!evos.isEmpty()) {
                        Evolution firstEvo = evos.iterator().next();
                        String speciesStr = Objects.requireNonNull(firstEvo.getResult().getSpecies());
                        Species evoSpecies = PokemonSpecies.getByName(speciesStr);
                        if (evoSpecies != null) {
                            resultSpecies = evoSpecies;
                        }
                    }
                }
                break;
            }
        }

        int limit = GeneralConfig.get().max_generation;

        while (resultSpecies != null && getGenFromDex(resultSpecies.getNationalPokedexNumber()) > limit) {

            Collection<Evolution> evos = resultSpecies.getStandardForm().getEvolutions();
            if (evos != null && !evos.isEmpty()) {
                Evolution firstEvo = evos.iterator().next();
                String speciesStr = firstEvo.getResult().getSpecies();

                if (speciesStr != null) {
                    resultSpecies = PokemonSpecies.getByName(speciesStr);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return resultSpecies;
    }

    private static boolean isHolding(Pokemon p, Item item) {
        return p.heldItem().getItem() == item;
    }

    private static boolean isHoldingItem(Pokemon p, String itemId) {
        return BuiltInRegistries.ITEM.getKey(p.heldItem().getItem()).toString().equals(itemId);
    }

    private static void inheritNature(Pokemon child, Pokemon mother, Pokemon father) {
        boolean motherStone = isHoldingItem(mother, "cobblemon:everstone");
        boolean fatherStone = isHoldingItem(father, "cobblemon:everstone");

        if (motherStone && fatherStone) {
            child.setNature(new Random().nextBoolean() ? mother.getNature() : father.getNature());
        } else if (motherStone) {
            child.setNature(mother.getNature());
        } else if (fatherStone) {
            child.setNature(father.getNature());
        }
    }

    private static void inheritAbility(Pokemon child, Pokemon mother, Pokemon father) {
        Pokemon sourceParent = mother;
        if (mother.getSpecies().getName().equalsIgnoreCase("ditto")) {
            sourceParent = father;
        }

        AbilityTemplate sourceTemplate = sourceParent.getAbility().getTemplate();
        AbilityPool pool = child.getForm().getAbilities();

        List<PotentialAbility> allPotentials = new ArrayList<>();
        pool.getMapping().values().forEach(allPotentials::addAll);

        PotentialAbility hiddenInChild = null;
        List<AbilityTemplate> normalAbilities = new ArrayList<>();

        for (PotentialAbility pot : allPotentials) {
            if (pot.getPriority() == Priority.LOW) {
                hiddenInChild = pot;
            } else {
                normalAbilities.add(pot.getTemplate());
            }
        }

        Random rand = new Random();

        boolean sourceHasHidden = hiddenInChild != null && hiddenInChild.getTemplate().equals(sourceTemplate);
        boolean sourceHasNormal = normalAbilities.contains(sourceTemplate);

        boolean passAbility = rand.nextInt(100) < 60;

        if (passAbility) {
            if (sourceHasHidden) {
                child.updateAbility(new Ability(sourceTemplate, false, Priority.LOW));
                return;
            }
            else if (sourceHasNormal) {
                child.updateAbility(new Ability(sourceTemplate, false, Priority.NORMAL));
                return;
            }
        }

        if (!normalAbilities.isEmpty()) {
            AbilityTemplate selected = normalAbilities.get(rand.nextInt(normalAbilities.size()));
            child.updateAbility(new Ability(selected, false, Priority.NORMAL));
        }
    }

    private static void inheritIVs(Pokemon child, Pokemon mother, Pokemon father) {
        IVs childIVs = child.getIvs();
        List<Stats> availableStats = new ArrayList<>(List.of(
                Stats.HP, Stats.ATTACK, Stats.DEFENCE,
                Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED
        ));

        Pokemon destinyKnotHolder = null;
        if (isHoldingItem(mother, "cobblemon:destiny_knot")) {
            destinyKnotHolder = mother;
        }
        if (destinyKnotHolder == null && isHoldingItem(father, "cobblemon:destiny_knot")) {
            destinyKnotHolder = father;
        }
        if (isHoldingItem(mother, "cobblemon:destiny_knot") && isHoldingItem(father, "cobblemon:destiny_knot")) {
            destinyKnotHolder = new Random().nextBoolean() ? mother : father;
        }

        Stats motherPowerStat = getPowerItemStat(mother.heldItem());
        Stats fatherPowerStat = getPowerItemStat(father.heldItem());

        Stats powerItemStat = null;
        Pokemon powerItemProvider = null;

        if (destinyKnotHolder != null) {
            if (destinyKnotHolder == mother && fatherPowerStat != null) {
                powerItemStat = fatherPowerStat;
                powerItemProvider = father;
            } else if (destinyKnotHolder == father && motherPowerStat != null) {
                powerItemStat = motherPowerStat;
                powerItemProvider = mother;
            } else {
                Stats holderPower = (destinyKnotHolder == mother) ? motherPowerStat : fatherPowerStat;
                if (holderPower != null) {
                    powerItemStat = holderPower;
                    powerItemProvider = destinyKnotHolder;
                }
            }
        } else {
            if (motherPowerStat != null) {
                powerItemStat = motherPowerStat;
                powerItemProvider = mother;
            } else if (fatherPowerStat != null) {
                powerItemStat = fatherPowerStat;
                powerItemProvider = father;
            }
        }


        int inheritedCount = 0;
        int maxInheritance = (destinyKnotHolder != null) ? 5 : 3;

        if (powerItemStat != null && powerItemProvider != null) {
            int val = powerItemProvider.getIvs().getOrDefault(powerItemStat);
            childIVs.set(powerItemStat, val);

            availableStats.remove(powerItemStat);
            inheritedCount++;
        }

        Collections.shuffle(availableStats);
        Random rand = new Random();

        while (inheritedCount < maxInheritance && !availableStats.isEmpty()) {
            Stats stat = availableStats.remove(0);

            if (destinyKnotHolder != null) {
                int val = destinyKnotHolder.getIvs().getOrDefault(stat);
                childIVs.set(stat, val);
            } else {
                Pokemon donor = rand.nextBoolean() ? mother : father;
                childIVs.set(stat, donor.getIvs().getOrDefault(stat));
            }
            inheritedCount++;
        }
    }

    private static List<String> calculateInheritedMoves(Species childSpecies, Pokemon mother, Pokemon father) {
        Set<String> motherMoves = getKnownMoveNames(mother);
        Set<String> fatherMoves = getKnownMoveNames(father);

        List<MoveTemplate> inheritedMoves = new ArrayList<>();
        var learnset = childSpecies.getStandardForm().getMoves();

        for (List<MoveTemplate> movesAtLevel : learnset.getLevelUpMoves().values()) {
            for (MoveTemplate template : movesAtLevel) {
                String moveName = template.getName().toLowerCase();
                if (motherMoves.contains(moveName) && fatherMoves.contains(moveName)) {
                    if (!inheritedMoves.contains(template)) inheritedMoves.add(template);
                }
            }
        }

        for (MoveTemplate template : learnset.getEggMoves()) {
            String moveName = template.getName().toLowerCase();
            if (motherMoves.contains(moveName) || fatherMoves.contains(moveName)) {
                if (!inheritedMoves.contains(template)) inheritedMoves.add(template);
            }
        }

        if (inheritedMoves.isEmpty()) return new ArrayList<>();

        List<MoveTemplate> finalMoves = new ArrayList<>();
        List<MoveTemplate> lvl1Moves = learnset.getLevelUpMoves().get(1);
        if (lvl1Moves != null) {
            finalMoves.addAll(lvl1Moves);
        }

        for (MoveTemplate template : inheritedMoves) {
            finalMoves.removeIf(m -> m.getName().equalsIgnoreCase(template.getName()));
            finalMoves.add(template);
        }

        if (finalMoves.size() > 4) {
            finalMoves = finalMoves.subList(finalMoves.size() - 4, finalMoves.size());
        }

        List<String> moveNames = new ArrayList<>();
        for (MoveTemplate tmpl : finalMoves) {
            moveNames.add(tmpl.getName().toLowerCase().replace(" ", "_"));
        }

        return moveNames;
    }

    private static Set<String> getKnownMoveNames(Pokemon pokemon) {
        Set<String> possibleMoves = new HashSet<>();

        for (Move move : pokemon.getMoveSet().getMoves()) {
            if (move != null) possibleMoves.add(move.getTemplate().getName().toLowerCase());
        }

        Species currentSpecies = pokemon.getSpecies();
        while (currentSpecies != null) {
            var learnset = currentSpecies.getStandardForm().getMoves();

            for (Map.Entry<Integer, List<MoveTemplate>> entry : learnset.getLevelUpMoves().entrySet()) {
                if (pokemon.getLevel() >= entry.getKey()) {
                    for (MoveTemplate tmpl : entry.getValue()) possibleMoves.add(tmpl.getName().toLowerCase());
                }
            }

            for (MoveTemplate tmpl : learnset.getEggMoves()) possibleMoves.add(tmpl.getName().toLowerCase());
            for (MoveTemplate tmpl : learnset.getTmMoves()) possibleMoves.add(tmpl.getName().toLowerCase());

            if (currentSpecies.getPreEvolution() != null) {
                currentSpecies = currentSpecies.getPreEvolution().getSpecies();
            } else {
                currentSpecies = null;
            }
        }

        return possibleMoves;
    }

    private static Stats getPowerItemStat(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return POWER_ITEMS_MAP.get(id);
    }

    private static void rollShiny(Pokemon child, Pokemon mother, Pokemon father) {
        Random rand = new Random();
        int chance = 4096;
        child.setShiny(rand.nextInt(chance) == 0);
    }

    private static int getGenFromDex(int dex) {
        if (dex <= 151) return 1;
        if (dex <= 251) return 2;
        if (dex <= 386) return 3;
        if (dex <= 493) return 4;
        if (dex <= 649) return 5;
        if (dex <= 721) return 6;
        if (dex <= 809) return 7;
        if (dex <= 905) return 8;
        return 9;
    }

    private static boolean isRegionalForm(String formName) {
        if (formName == null) return false;
        String s = formName.toLowerCase();
        return s.contains("hisui") || s.contains("alola") || s.contains("galar") || s.contains("paldea");
    }

    private static String getDormantGene(String formName) {
        String s = formName.toLowerCase();
        if (s.contains("hisui")) return "hisuian";
        if (s.contains("alola")) return "alolan";
        if (s.contains("galar")) return "galarian";
        if (s.contains("paldea")) return "paldean";
        return s;
    }
}