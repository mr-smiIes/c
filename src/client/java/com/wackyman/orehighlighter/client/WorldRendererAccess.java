package com.wackyman.orehighlighter.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

public final class WorldRendererAccess {
    private WorldRendererAccess() {}

    public static void drawBox(
            MatrixStack matrices,
            VertexConsumer consumer,
            Box box,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        net.minecraft.client.render.WorldRenderer.drawBox(
                matrices, consumer, box, red, green, blue, alpha
        );
    }
}
