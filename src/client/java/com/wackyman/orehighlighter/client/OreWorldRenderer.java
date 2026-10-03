package com.wackyman.orehighlighter.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class OreWorldRenderer {

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private OreWorldRenderer() {
    }

    public static void initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(
                OreWorldRenderer::extract
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                OreWorldRenderer::render
        );
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {
        if (newHighlights == null || newHighlights.isEmpty()) {
            highlights = List.of();
            return;
        }

        highlights = List.copyOf(newHighlights);
    }

    private static void extract(
            LevelExtractionContext context
    ) {
        highlights =
                OreHighlighterClient.getHighlights();
    }

    private static void render(
            LevelRenderContext context
    ) {
        if (highlights.isEmpty()) {
            return;
        }

        PoseStack matrices =
                context.poseStack();

        Vec3 camera =
                context.levelState()
                        .cameraRenderState
                        .pos;

        matrices.pushPose();

        for (
                OreHighlighterClient.Highlight highlight
                        : highlights
        ) {
            BlockPos pos =
                    highlight.pos();

            AABB box =
                    new AABB(
                            pos.getX() - camera.x,
                            pos.getY() - camera.y,
                            pos.getZ() - camera.z,
                            pos.getX() + 1.0 - camera.x,
                            pos.getY() + 1.0 - camera.y,
                            pos.getZ() + 1.0 - camera.z
                    );

            WorldRendererAccess.drawBox(
                    matrices,
                    context.bufferSource(),
                    box,
                    highlight.r(),
                    highlight.g(),
                    highlight.b(),
                    1.0f
            );
        }

        matrices.popPose();

        context.bufferSource().endBatch();
    }

    public static void close() {
        highlights = List.of();
    }
}
