package com.wackyman.orehighlighter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;

public final class WorldRendererAccess {

    private WorldRendererAccess() {
    }

    public static void renderHighlights(
            LevelRenderContext context
    ) {

        if (
                !OreHighlighterClient.isOreHighlightEnabled()
                        || OreHighlighterClient.getHighlights().isEmpty()
        ) {
            return;
        }

        PoseStack matrices =
                context.poseStack();

        var collector =
                context.submitNodeCollector();

        double cameraX =
                context.levelState()
                        .cameraRenderState
                        .pos
                        .x;

        double cameraY =
                context.levelState()
                        .cameraRenderState
                        .pos
                        .y;

        double cameraZ =
                context.levelState()
                        .cameraRenderState
                        .pos
                        .z;

        for (
                OreHighlighterClient.Highlight highlight :
                OreHighlighterClient.getHighlights()
        ) {

            BlockPosData pos =
                    new BlockPosData(
                            highlight.pos().getX() - cameraX,
                            highlight.pos().getY() - cameraY,
                            highlight.pos().getZ() - cameraZ
                    );

            float red = highlight.r();
            float green = highlight.g();
            float blue = highlight.b();

            AABB box =
                    new AABB(
                            pos.x,
                            pos.y,
                            pos.z,
                            pos.x + 1.0,
                            pos.y + 1.0,
                            pos.z + 1.0
                    );

            collector.submitCustomGeometry(
                    matrices,
                    RenderTypes.lines(),
                    (pose, consumer) ->
                            drawBox(
                                    pose,
                                    consumer,
                                    box,
                                    red,
                                    green,
                                    blue,
                                    1.0f
                            )
            );
        }
    }

    private static void drawBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            AABB box,
            float red,
            float green,
            float blue,
            float alpha
    ) {

        line(consumer, pose,
                box.minX, box.minY, box.minZ,
                box.maxX, box.minY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.minY, box.minZ,
                box.maxX, box.minY, box.maxZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.minY, box.maxZ,
                box.minX, box.minY, box.maxZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.minX, box.minY, box.maxZ,
                box.minX, box.minY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.minX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.maxY, box.maxZ,
                box.minX, box.maxY, box.maxZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.minX, box.maxY, box.maxZ,
                box.minX, box.maxY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.minX, box.minY, box.minZ,
                box.minX, box.maxY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.minY, box.minZ,
                box.maxX, box.maxY, box.minZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.maxX, box.minY, box.maxZ,
                box.maxX, box.maxY, box.maxZ,
                red, green, blue, alpha);

        line(consumer, pose,
                box.minX, box.minY, box.maxZ,
                box.minX, box.maxY, box.maxZ,
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
    }

    private record BlockPosData(
            double x,
            double y,
            double z
    ) {
    }
}
