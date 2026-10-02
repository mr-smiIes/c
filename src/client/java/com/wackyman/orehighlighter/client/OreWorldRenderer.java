package com.wackyman.orehighlighter.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

public final class OreWorldRenderer {

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private OreWorldRenderer() {
    }

    public static void initialize() {

        LevelExtractionEvents.END_EXTRACTION.register(
                context -> {
                    // Highlights are updated by OreHighlighterClient.
                }
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                context -> render(context)
        );
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {
        highlights = newHighlights;
    }

    private static void render(
            LevelRenderEvents.AfterTranslucentTerrainContext context
    ) {

        if (highlights.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.level == null || client.player == null) {
            return;
        }

        PoseStack matrices = context.poseStack();

        Vec3 camera =
                client.gameRenderer
                        .getMainCamera()
                        .getPosition();

        matrices.pushPose();

        matrices.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        MultiBufferSource.BufferSource buffers =
                client.renderBuffers()
                        .bufferSource();

        VertexConsumer vertexConsumer =
                buffers.getBuffer(
                        RenderType.lines()
                );

        for (
                OreHighlighterClient.Highlight highlight
                        : highlights
        ) {

            BlockPos pos =
                    highlight.pos();

            AABB box =
                    new AABB(
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),

                            pos.getX() + 1,
                            pos.getY() + 1,
                            pos.getZ() + 1
                    );

            drawBox(
                    matrices,
                    vertexConsumer,
                    box,

                    highlight.r(),
                    highlight.g(),
                    highlight.b(),
                    1.0f
            );
        }

        buffers.endBatch(
                RenderType.lines()
        );

        matrices.popPose();
    }

    private static void drawBox(
            PoseStack matrices,
            VertexConsumer consumer,
            AABB box,

            float red,
            float green,
            float blue,
            float alpha
    ) {

        PoseStack.Pose pose =
                matrices.last();

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        line(
                pose,
                consumer,
                minX, minY, minZ,
                maxX, minY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                minX, minY, maxZ,
                minX, minY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                minX, minY, minZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                pose,
                consumer,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );
    }

    private static void line(
            PoseStack.Pose pose,
            VertexConsumer consumer,

            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float red,
            float green,
            float blue,
            float alpha
    ) {

        consumer.addVertex(
                pose,
                x1,
                y1,
                z1
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        consumer.addVertex(
                pose,
                x2,
                y2,
                z2
        ).setColor(
                red,
                green,
                blue,
                alpha
        );
    }
}
