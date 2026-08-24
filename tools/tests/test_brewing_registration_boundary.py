from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/Momik/usless_mobs"
MAIN = JAVA / "Usless_mobs.java"
BREWING = JAVA / "registry/ModBrewingRecipes.java"


def test_entrypoint_delegates_brewing_without_owning_recipe_details():
    source = MAIN.read_text(encoding="utf-8")

    assert "ModBrewingRecipes.register();" in source
    assert "BrewingRecipeRegistry" not in source
    assert "PotionUtils" not in source
    assert "potionStack(" not in source


def test_brewing_registry_owns_all_four_recipe_contracts():
    source = BREWING.read_text(encoding="utf-8")

    assert source.count("BrewingRecipeRegistry.addRecipe(") == 4
    assert "ModPotions.ELASTICITY_POTION" in source
    assert "ModPotions.LONG_ELASTICITY_POTION" in source
    assert "ModPotions.GOLDEN_FLOW_POTION" in source
    assert "ModPotions.STRONG_GOLDEN_FLOW_POTION" in source
