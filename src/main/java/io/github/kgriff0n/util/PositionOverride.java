package io.github.kgriff0n.util;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public record PositionOverride(Vec3d position, Vec2f rotation, String world) {
}
