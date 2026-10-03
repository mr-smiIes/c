package com.wackyman.orehighlighter.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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

    /*
     * Minecraft 26.3's own LINES_SNIPPET uses:
     *
     * POSITION_COLOR_NORMAL_LINE_WIDTH
     * PrimitiveTopology.LINES
     *
     * We keep the vanilla line shaders, but remove the depth test
     * so ore outlines can be seen through blocks.
     */
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

    /*
     * The buffer format is explicitly the exact 26.3 line format.
     *
     * This is intentionally NOT obtained from the pipeline at runtime.
     */
    private static final VertexFormat LINE_VERTEX_FORMAT =
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH;

    private static final PrimitiveTopology LINE_PRIMITIVE =
            PrimitiveTopology.LINES;

    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(
                    () -> "Ore Highlighter Buffer",
                    RenderType.SMALL_BUFFER_SIZE
            );

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f
            );

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    private OreWorldRenderer() {
    }

    public static void initialize() {

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                OreWorldRenderer::render
        );
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {

        if (
                newHighlights == null
                        || newHighlights.isEmpty()
        ) {
            highlights = List.of();
            return;
        }

        highlights = List.copyOf(newHighlights);
    }

    private static void render(
            LevelRenderContext context
    ) {

        if (highlights.isEmpty()) {
            return;
        }

        /*
         * Use the exact Minecraft 26.3 line vertex format.
         */
        StagedVertexBuffer.Draw draw =
                STAGED_BUFFER.appendDraw(
                        LINE_VERTEX_FORMAT,
                        LINE_PRIMITIVE
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
                    ORE_LINES_THROUGH_WALLS
            );
        }

        STAGED_BUFFER.endFrame();
    }

    private static void renderOres(
            LevelRenderContext context,
            StagedVertexBuffer.Draw draw
    ) {

        PoseStack poseStack =
                context.poseStack();

        Vec3 camera =
                context.levelState()
                        .cameraRenderState
                        .pos;

        poseStack.pushPose();

        /*
         * Convert world coordinates into camera-relative coordinates.
         */
        poseStack.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        VertexConsumer buffer =
                STAGED_BUFFER.getVertexBuilder(draw);

        Matrix4fc matrix =
                poseStack.last().pose();

        for (
                OreHighlighterClient.Highlight highlight
                        : highlights
        ) {

            BlockPos pos =
                    highlight.pos();

            drawBox(
                    matrix,
                    buffer,

                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),

                    pos.getX() + 1.0f,
                    pos.getY() + 1.0f,
                    pos.getZ() + 1.0f,

                    highlight.r(),
                    highlight.g(),
                    highlight.b(),
                    1.0f
            );
        }

        poseStack.popPose();
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

        /*
         * Bottom.
         */
        line(
                buffer,
                matrix,
                minX, minY, minZ,
                maxX, minY, minZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                minX, minY, maxZ,
                minX, minY, minZ,
                red, green, blue, alpha
        );

        /*
         * Top.
         */
        line(
                buffer,
                matrix,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        /*
         * Vertical edges.
         */
        line(
                buffer,
                matrix,
                minX, minY, minZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                buffer,
                matrix,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );
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

        /*
         * Minecraft 26.3's line format is:
         *
         * Position
         * Color
         * Normal
         * LineWidth
         *
         * Every vertex MUST provide all four.
         */

        buffer.addVertex(
                matrix,
                x1,
                y1,
                z1
        )
        .setColor(
                red,
                green,
                blue,
                alpha
        )
        .setNormal(
                0.0f,
                1.0f,
                0.0f
        )
        .setLineWidth(
                2.0f
        );

        buffer.addVertex(
                matrix,
                x2,
                y2,
                z2
        )
        .setColor(
                red,
                green,
                blue,
                alpha
        )
        .setNormal(
                0.0f,
                1.0f,
                0.0f
        )
        .setLineWidth(
                2.0f
        );
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

        highlights = List.of();
    }
}
