package com.wackyman.orehighlighter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;

public final class WorldRendererAccess {

    private WorldRendererAccess() {
    }

    public static void drawBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            AABB box,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;

        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        line(consumer, pose, minX, minY, minZ, maxX, minY, minZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, minY, minZ, maxX, minY, maxZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, minY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        line(consumer, pose, minX, minY, maxZ, minX, minY, minZ,
                red, green, blue, alpha);

        line(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, maxY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, maxY, maxZ, minX, maxY, maxZ,
                red, green, blue, alpha);
        line(consumer, pose, minX, maxY, maxZ, minX, maxY, minZ,
                red, green, blue, alpha);

        line(consumer, pose, minX, minY, minZ, minX, maxY, minZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ,
                red, green, blue, alpha);
        line(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
        line(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ,
                red, green, blue, alpha);
    }

    private static void line(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);

        float length =
                (float) Math.sqrt(
                        dx * dx +
                        dy * dy +
                        dz * dz
                );

        if (length == 0.0f) {
            return;
        }

        dx /= length;
        dy /= length;
        dz /= length;

        consumer
                .addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, dx, dy, dz);

        consumer
                .addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, dx, dy, dz);
    }
}
