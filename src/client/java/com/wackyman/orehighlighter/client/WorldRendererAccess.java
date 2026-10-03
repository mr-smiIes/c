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
        box.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
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
                    .addVertex(
                            pose,
                            (float) x1,
                            (float) y1,
                            (float) z1
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    )
                    .setNormal(
                            pose,
                            dx,
                            dy,
                            dz
                    );

            consumer
                    .addVertex(
                            pose,
                            (float) x2,
                            (float) y2,
                            (float) z2
                    )
                    .setColor(
                            red,
                            green,
                            blue,
                            alpha
                    )
                    .setNormal(
                            pose,
                            dx,
                            dy,
                            dz
                    );
        });
    }
}
