package io.github.kgriff0n.util;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record PositionOverride(Vec3 position, Vec2 rotation, String world) {
}
