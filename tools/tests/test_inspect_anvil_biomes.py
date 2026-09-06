from tools.inspect_anvil_biomes import biome_cells


def test_uniform_biome_section_expands_to_64_cells():
    chunk = {
        "xPos": -2,
        "zPos": 3,
        "sections": [{"Y": -1, "biomes": {"palette": ["usless_mobs:big_underwater_cave"]}}],
    }

    cells = list(biome_cells(chunk))

    assert len(cells) == 64
    assert cells[0] == ("usless_mobs:big_underwater_cave", -32, -16, 48)
    assert cells[-1] == ("usless_mobs:big_underwater_cave", -20, -4, 60)


def test_packed_two_entry_palette_preserves_cell_axes_and_filter():
    # Bit 0 selects the second biome at cell x=0, y=0, z=0.
    chunk = {
        "xPos": 1,
        "zPos": 2,
        "sections": [{
            "Y": 2,
            "biomes": {
                "palette": ["minecraft:deep_ocean", "usless_mobs:deep_ocean"],
                "data": [1],
            },
        }],
    }

    cells = list(biome_cells(chunk, {"usless_mobs:deep_ocean"}))

    assert cells == [("usless_mobs:deep_ocean", 16, 32, 32)]
