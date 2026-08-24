from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MESH = ROOT / "src/main/mobs/endermite/java/net/mysith/client/CorruptedSilverfishMesh.java"
RENDERER = ROOT / "src/main/mobs/endermite/java/net/mysith/client/CorruptedSilverfishRenderer.java"
LIVING_CRYSTAL = ROOT / "src/main/java/com/Momik/usless_mobs/block/LivingCrystalBlock.java"


def test_corrupted_silverfish_uses_supported_resource_location_factory():
    for source_path in (MESH, RENDERER):
        source = source_path.read_text(encoding="utf-8")
        assert "new ResourceLocation(" not in source
        assert "ResourceLocation.tryBuild(" in source


def test_living_crystal_documents_intentional_legacy_lifecycle_override():
    source = LIVING_CRYSTAL.read_text(encoding="utf-8")
    marker = '@SuppressWarnings("deprecation")\n    @Override\n    public void onPlace('
    assert marker in source
