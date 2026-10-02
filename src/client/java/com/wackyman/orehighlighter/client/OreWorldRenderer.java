package com.wackyman.orehighlighter.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
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
     * Line pipeline with depth disabled.
     *
     * This is what allows the ore outlines to remain visible
     * through solid blocks.
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
     * GPU staging buffer.
     *
     * StagedVertexBuffer handles the CPU -> GPU upload for us.
     */
    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(
                    () -> "Ore Highlighter Buffer",
                    RenderType.SMALL_BUFFER_SIZE
            );

    /*
     * Render state.
     */
    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    /*
     * Dynamic uniform values.
     */
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

    /**
     * Registers the world rendering callback.
     */
    public static void initialize() {

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                OreWorldRenderer::render
        );
    }

    /**
     * Updates the list of ores to render.
     */
    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {

        if (newHighlights == null || newHighlights.isEmpty()) {
            highlights = List.of();
            return;
        }

        /*
         * Make our own immutable copy so the renderer cannot
         * accidentally render a list while another part of
         * the mod is modifying it.
         */
        highlights = List.copyOf(newHighlights);
    }

    /**
     * Called during world rendering.
     */
    private static void render(
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

        /*
         * Create one draw for this frame.
         */
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

        /*
         * Write all ore boxes into the staged buffer.
         */
        renderOres(
                context,
                draw
        );

        /*
         * Upload generated vertices.
         */
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

        /*
         * Finish this frame's staged-buffer work.
         */
        STAGED_BUFFER.endFrame();
    }

    /**
     * Generates the actual ore geometry.
     */
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
         * Convert world coordinates into camera-relative
         * coordinates.
         */
        poseStack.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        VertexConsumer buffer =
                STAGED_BUFFER.getVertexBuilder(draw);

        /*
         * Draw every highlighted ore.
         *
         * OreHighlighterClient already enforces the maximum
         * of 100 highlighted ores.
         */
        for (
                OreHighlighterClient.Highlight highlight
                        : highlights
        ) {

            BlockPos pos =
                    highlight.pos();

            drawBox(
                    poseStack.last().pose(),
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

    /**
     * Draws the twelve edges of one Minecraft block.
     */
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
         * Bottom square.
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
         * Top square.
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
         * Four vertical edges.
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

    /**
     * Writes one complete line.
     *
     * IMPORTANT:
     * LINES_SNIPPET requires:
     *
     * POSITION
     * COLOR
     * NORMAL
     * LINE_WIDTH
     *
     * Every vertex is therefore fully populated before the
     * next vertex is started.
     */
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
         * First endpoint.
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

        /*
         * Second endpoint.
         */
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

    /**
     * Sends the generated geometry to the GPU.
     */
    private static void draw(
            Minecraft client,
            StagedVertexBuffer.ExecuteInfo info,
            RenderPipeline pipeline
    ) {

        /*
         * Dynamic transformation uniforms.
         */
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

        /*
         * Create the render pass.
         */
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

            /*
             * Use the compiled pipeline.
             */
            renderPass.setPipeline(
                    RenderSystem.getCompiledPipeline(
                            pipeline
                    )
            );

            /*
             * Bind Minecraft's normal rendering uniforms.
             */
            RenderSystem.bindDefaultUniforms(
                    renderPass
            );

            renderPass.setUniform(
                    "DynamicTransforms",
                    dynamicTransforms
            );

            /*
             * Bind our vertex buffer.
             */
            renderPass.setVertexBuffer(
                    0,
                    info.vertexBuffer().slice()
            );

            /*
             * Bind the index buffer.
             */
            renderPass.setIndexBuffer(
                    info.indexBuffer(),
                    info.indexType()
            );

            /*
             * Draw the lines.
             */
            renderPass.drawIndexed(
                    info.indexCount(),
                    1,
                    info.firstIndex(),
                    info.baseVertex(),
                    0
            );
        }
    }

    /**
     * Releases GPU resources.
     */
    public static void close() {
        STAGED_BUFFER.close();
        highlights = List.of();
    }
}
