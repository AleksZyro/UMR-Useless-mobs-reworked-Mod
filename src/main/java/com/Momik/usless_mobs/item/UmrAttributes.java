package com.Momik.usless_mobs.item;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Single source of truth for AttributeModifier UUIDs and construction.
 * Use deterministic UUIDs derived from (namespace, slot, kind) so identical
 * concepts always resolve to the same UUID without hard-coded literals.
 */
public final class UmrAttributes {
    private UmrAttributes() {}

    public static UUID uuid(String namespace, String kind) {
        return UUID.nameUUIDFromBytes((namespace + ":" + kind).getBytes(StandardCharsets.UTF_8));
    }

    public static UUID uuid(String namespace, EquipmentSlot slot, String kind) {
        return uuid(namespace, slot.getName() + "_" + kind);
    }

    public static AttributeModifier addition(UUID id, String name, double value) {
        return new AttributeModifier(id, name, value, AttributeModifier.Operation.ADDITION);
    }

    public static AttributeModifier multiplyTotal(UUID id, String name, double value) {
        return new AttributeModifier(id, name, value, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static AttributeModifier addition(String namespace, EquipmentSlot slot, String kind, String name, double value) {
        return addition(uuid(namespace, slot, kind), name, value);
    }

    public static AttributeModifier multiplyTotal(String namespace, EquipmentSlot slot, String kind, String name, double value) {
        return multiplyTotal(uuid(namespace, slot, kind), name, value);
    }
}
