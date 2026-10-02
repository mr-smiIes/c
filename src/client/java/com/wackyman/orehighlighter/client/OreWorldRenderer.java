package com.wackyman.orehighlighter.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class OreWorldRenderer {

    private static final RenderPipeline ORE_LINES_THROUGH_WALLS =
            RenderPipelines.register(
                    RenderPipeline.builder(
                            RenderPipelines.LINES_SNIPPET
                    )
                    .withLocation(
                            Identifier.fromNamespaceAndPath(
                                    OreHighlighterClient.MOD_ID,
                                    "pipeline/ore_lines_through_walls"
                            )
                    )
                    .withDepthStencilState(
                            Optional.empty()
                    )
                    .build()
            );

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(1f, 1f, 1f, 1f);

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(
                    () -> "Ore Highlighter Buffer",
                    RenderType.SMALL_BUFFER_SIZE
            );

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private OreWorldRenderer() {
    }

    public static void initialize() {

        LevelExtractionEvents.END_EXTRACTION.register(
                context -> {
                    // World data is already collected by the client scanner.
                }
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                OreWorldRenderer::renderAndDraw
        );
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {
        highlights = newHighlights;
    }

    private static void renderAndDraw(
            LevelRenderContext context
    ) {

        if (highlights.isEmpty()) {
            return;
        }

        RenderPipeline pipeline =
                ORE_LINES_THROUGH_WALLS;

        VertexFormat format =
                pipeline.getVertexFormatBinding(0);

        if (format == null) {
            return;
        }

        PrimitiveTopology primitive =
                pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw =
                STAGED_BUFFER.appendDraw(
                        format,
                        primitive,
                        primitive == PrimitiveTopology.QUADS
                                ? RenderSystem
                                        .getProjectionType()
                                        .vertexSorting()
                                : null
                );

        renderOres(
                context,
                draw
        );

        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info =
                STAGED_BUFFER.getExecuteInfo(draw);

        if (info != null) {
            draw(
                    Minecraft.getInstance(),
                    info,
                    pipeline
            );
        }

        STAGED_BUFFER.endFrame();
    }

    private static void renderOres(
            LevelRenderContext context,
            StagedVertexBuffer.Draw draw
    ) {

        PoseStack matrices =
                context.poseStack();

        Vec3 camera =
                context.levelState()
                        .cameraRenderState
                        .pos;

        matrices.pushPose();

        matrices.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        VertexConsumer builder =
                STAGED_BUFFER.getVertexBuilder(draw);

        for (
                OreHighlighterClient.Highlight highlight
                        : highlights
        ) {

            BlockPos pos =
                    highlight.pos();

            drawBox(
                    matrices.last().pose(),
                    builder,

                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),

                    pos.getX() + 1,
                    pos.getY() + 1,
                    pos.getZ() + 1,

                    highlight.r(),
                    highlight.g(),
                    highlight.b(),
                    1.0f
            );
        }

        matrices.popPose();
    }

    private static void drawBox(
            Matrix4fc matrix,
            VertexConsumer buffer,

            float minX,
            float minY,
            float minZ,

            float maxX,
            float maxY,
            float maxZ,

            float red,
            float green,
            float blue,
            float alpha
    ) {

        // Bottom
        line(buffer, matrix,
                minX, minY, minZ,
                maxX, minY, minZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                minX, minY, maxZ,
                minX, minY, minZ,
                red, green, blue, alpha);

        // Top
        line(buffer, matrix,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                red, green, blue, alpha);

        // Vertical edges
        line(buffer, matrix,
                minX, minY, minZ,
                minX, maxY, minZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha);

        line(buffer, matrix,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha);
    }

    private static void line(
        VertexConsumer buffer,
        Matrix4fc matrix,

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
    int color = (
            ((int) (alpha * 255.0f) & 0xFF) << 24
                    | ((int) (red * 255.0f) & 0xFF) << 16
                    | ((int) (green * 255.0f) & 0xFF) << 8
                    | ((int) (blue * 255.0f) & 0xFF)
    );

    buffer.addVertex(
            x1,
            y1,
            z1,
            color,
            0.0f,
            0.0f,
            0,
            0,
            0.0f,
            1.0f,
            0.0f
    ).setLineWidth(2.0f);

    buffer.addVertex(
            x2,
            y2,
            z2,
            color,
            0.0f,
            0.0f,
            0,
            0,
            0.0f,
            1.0f,
            0.0f
    ).setLineWidth(2.0f);
}
    private static void draw(
            Minecraft client,
            StagedVertexBuffer.ExecuteInfo info,
            RenderPipeline pipeline
    ) {

        GpuBufferSlice dynamicTransforms =
                RenderSystem
                        .getDynamicUniforms()
                        .writeTransform(
                                RenderSystem.getModelViewMatrixCopy(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX
                        );

        RenderTarget mainTarget =
                client.gameRenderer.mainRenderTarget();

        var colorTexture =
                mainTarget.getColorTextureView();

        if (colorTexture == null) {
            return;
        }

        try (
                RenderPass renderPass =
                        RenderSystem
                                .getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () ->
                                                OreHighlighterClient.MOD_ID
                                                        + " ore highlight rendering",

                                        colorTexture,

                                        Optional.empty(),

                                        mainTarget
                                                .getDepthTextureView(),

                                        OptionalDouble.empty()
                                )
        ) {

            renderPass.setPipeline(
                    RenderSystem.getCompiledPipeline(
                            pipeline
                    )
            );

            RenderSystem.bindDefaultUniforms(
                    renderPass
            );

            renderPass.setUniform(
                    "DynamicTransforms",
                    dynamicTransforms
            );

            renderPass.setVertexBuffer(
                    0,
                    info.vertexBuffer().slice()
            );

            renderPass.setIndexBuffer(
                    info.indexBuffer(),
                    info.indexType()
            );

            renderPass.drawIndexed(
                    info.indexCount(),
                    1,
                    info.firstIndex(),
                    info.baseVertex(),
                    0
            );
        }
    }

    public static void close() {
        STAGED_BUFFER.close();
    }
}
