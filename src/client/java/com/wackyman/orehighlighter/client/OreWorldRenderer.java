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

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
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

    /*
     * Vanilla line pipeline, but with no depth/stencil state.
     *
     * Removing the depth test is what allows the ore outlines
     * to remain visible through blocks.
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
     * Exact Minecraft 26.3 line vertex format:
     *
     * Position
     * Color
     * Normal
     * LineWidth
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

    /*
     * The ExecuteInfo produced during extraction.
     *
     * It is uploaded during END_EXTRACTION and consumed later
     * during the actual render event.
     */
    private static StagedVertexBuffer.ExecuteInfo pendingDraw;

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

        /*
         * EXTRACTION PHASE
         *
         * This happens before Minecraft begins drawing render passes.
         * GPU buffer uploads are safe here.
         */
        LevelExtractionEvents.END_EXTRACTION.register(
                OreWorldRenderer::extract
        );

        /*
         * DRAWING PHASE
         *
         * This happens after terrain rendering has started.
         * Only actual draw commands happen here.
         */
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

    /*
     * ============================================================
     * EXTRACTION PHASE
     * ============================================================
     *
     * Build and upload the vertex buffer here.
     *
     * This is intentionally NOT done inside AFTER_TRANSLUCENT_TERRAIN.
     */
    private static void extract(
            LevelExtractionContext context
    ) {

        pendingDraw = null;

        if (highlights.isEmpty()) {
            return;
        }

        StagedVertexBuffer.Draw draw =
                STAGED_BUFFER.appendDraw(
                        LINE_VERTEX_FORMAT,
                        LINE_PRIMITIVE
                );

        renderOres(
                context,
                draw
        );

        /*
         * IMPORTANT:
         *
         * upload() performs GPU buffer commands.
         *
         * END_EXTRACTION is the correct place for this because
         * Minecraft has not entered the render pass yet.
         */
        STAGED_BUFFER.upload();

        pendingDraw =
                STAGED_BUFFER.getExecuteInfo(draw);
    }

    /*
     * ============================================================
     * DRAWING PHASE
     * ============================================================
     */
    private static void render(
            LevelRenderContext context
    ) {

        if (pendingDraw == null) {
            STAGED_BUFFER.endFrame();
            return;
        }

        draw(
                Minecraft.getInstance(),
                pendingDraw,
                ORE_LINES_THROUGH_WALLS
        );

        pendingDraw = null;

        /*
         * Finish this frame's staged-buffer work only after
         * the draw has been submitted.
         */
        STAGED_BUFFER.endFrame();
    }

    private static void renderOres(
            LevelExtractionContext context,
            StagedVertexBuffer.Draw draw
    ) {

        Vec3 camera =
                context.camera()
                        .getPosition();

        /*
         * We still use a PoseStack because VertexConsumer expects
         * transformed coordinates.
         *
         * The extraction context gives us the camera position
         * directly.
         */
        PoseStack poseStack =
                new PoseStack();

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
         * Minecraft 26.3 line vertex order:
         *
         * POSITION
         * COLOR
         * NORMAL
         * LINE_WIDTH
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

        pendingDraw = null;

        STAGED_BUFFER.close();

        highlights = List.of();
    }
}
