package com.wackyman.orehighlighter.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

public final class WorldRendererAccess {

    private WorldRendererAccess() {
    }

    public static void drawBox(
            PoseStack matrices,
            MultiBufferSource bufferSource,
            AABB box,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        LevelRenderer.renderLineBox(
                matrices,
                bufferSource.getBuffer(RenderType.lines()),
                box,
                red,
                green,
                blue,
                alpha
        );
    }
}
