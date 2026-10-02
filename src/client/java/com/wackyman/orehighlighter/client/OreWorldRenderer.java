package com.wackyman.orehighlighter.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
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

    private static final ByteBufferBuilder ALLOCATOR =
            new ByteBufferBuilder(
                    RenderType.SMALL_BUFFER_SIZE
            );

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(
                    1f,
                    1f,
                    1f,
                    1f
            );

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    private static BufferBuilder buffer;

    private static MappableRingBuffer vertexBuffer;

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private OreWorldRenderer() {
    }

    public static void initialize() {

        /*
         * Extraction phase.
         *
         * The actual ore list has already been collected by
         * OreHighlighterClient, so there is nothing to scan here.
         */
        LevelRenderEvents.END_EXTRACTION.register(
                OreWorldRenderer::extractOres
        );

        /*
         * Drawing phase.
         */
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                OreWorldRenderer::renderAndDraw
        );
    }

    private static void extractOres(
            LevelExtractionContext context
    ) {

        if (highlights.isEmpty()) {
            return;
        }

        /*
         * Create a fresh BufferBuilder for this frame.
         */
        buffer =
                new BufferBuilder(
                        ALLOCATOR,
                        ORE_LINES_THROUGH_WALLS.getVertexFormatMode(),
                        ORE_LINES_THROUGH_WALLS.getVertexFormat()
                );

        /*
         * We cannot use the normal camera position from the world
         * render context here because extraction and drawing are
         * separate phases.
         *
         * The actual vertex generation is therefore performed in
         * renderAndDraw(), where the LevelRenderContext is available.
         */
    }

    private static void renderAndDraw(
            LevelRenderContext context
    ) {

        if (highlights.isEmpty()) {
            cleanupBuffer();

            return;
        }

        /*
         * The BufferBuilder needs to be created here because this
         * is where we have access to the camera/render pose.
         */
        if (buffer == null) {

            buffer =
                    new BufferBuilder(
                            ALLOCATOR,
                            ORE_LINES_THROUGH_WALLS.getVertexFormatMode(),
                            ORE_LINES_THROUGH_WALLS.getVertexFormat()
                    );
        }

        renderOres(
                context,
                buffer
        );

        MeshData builtBuffer;

        try {
            builtBuffer =
                    buffer.buildOrThrow();
        } catch (Exception exception) {

            buffer = null;

            return;
        }

        MeshData.DrawState drawState =
                builtBuffer.drawState();

        VertexFormat format =
                drawState.format();

        int vertexBufferSize =
                drawState.vertexCount()
                        * format.getVertexSize();

        if (
                vertexBuffer == null
                        || vertexBuffer.size() < vertexBufferSize
        ) {

            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer =
                    new MappableRingBuffer(
                            () ->
                                    OreHighlighterClient.MOD_ID
                                            + " ore highlight vertex buffer",
                            GpuBuffer.USAGE_VERTEX
                                    | GpuBuffer.USAGE_MAP_WRITE,
                            vertexBufferSize
                    );
        }

        /*
         * Upload vertex data to the GPU.
         *
         * This happens before creating our render pass.
         */
        CommandEncoder commandEncoder =
                RenderSystem
                        .getDevice()
                        .createCommandEncoder();

        try (
                GpuBuffer.MappedView mappedView =
                        commandEncoder.mapBuffer(
                                vertexBuffer
                                        .currentBuffer()
                                        .slice(
                                                0,
                                                builtBuffer
                                                        .vertexBuffer()
                                                        .remaining()
                                        ),
                                false,
                                true
                        )
        ) {

            mappedView.data();

            org.lwjgl.system.MemoryUtil.memCopy(
                    builtBuffer.vertexBuffer(),
                    mappedView.data()
            );
        }

        draw(
                Minecraft.getInstance(),
                builtBuffer,
                drawState,
                vertexBuffer.currentBuffer(),
                format
        );

        /*
         * Rotate the ring buffer so the GPU can finish using the
         * previous buffer while the next frame gets another one.
         */
        vertexBuffer.rotate();

        builtBuffer.close();

        buffer = null;
    }

    private static void renderOres(
            LevelRenderContext context,
            BufferBuilder builder
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
            BufferBuilder buffer,

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

        line(
                buffer,
                matrix,
                minX,
                minY,
                minZ,
                maxX,
                minY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                minY,
                minZ,
                maxX,
                minY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                minY,
                maxZ,
                minX,
                minY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                minX,
                minY,
                maxZ,
                minX,
                minY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        // Top

        line(
                buffer,
                matrix,
                minX,
                maxY,
                minZ,
                maxX,
                maxY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                maxY,
                minZ,
                maxX,
                maxY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                maxY,
                maxZ,
                minX,
                maxY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                minX,
                maxY,
                maxZ,
                minX,
                maxY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        // Vertical edges

        line(
                buffer,
                matrix,
                minX,
                minY,
                minZ,
                minX,
                maxY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                minY,
                minZ,
                maxX,
                maxY,
                minZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                maxX,
                minY,
                maxZ,
                maxX,
                maxY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );

        line(
                buffer,
                matrix,
                minX,
                minY,
                maxZ,
                minX,
                maxY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );
    }

    private static void line(
            BufferBuilder buffer,
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

        buffer.addVertex(
                matrix,
                x1,
                y1,
                z1
        ).setColor(
                red,
                green,
                blue,
                alpha
        );

        buffer.addVertex(
                matrix,
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

    private static void draw(
            Minecraft client,
            MeshData builtBuffer,
            MeshData.DrawState drawState,
            GpuBuffer vertices,
            VertexFormat format
    ) {

        GpuBuffer indices;

        VertexFormat.IndexType indexType;

        if (
                ORE_LINES_THROUGH_WALLS
                        .getVertexFormatMode()
                        == VertexFormat.Mode.QUADS
        ) {

            builtBuffer.sortQuads(
                    ALLOCATOR,
                    RenderSystem
                            .getProjectionType()
                            .vertexSorting()
            );

            indices =
                    ORE_LINES_THROUGH_WALLS
                            .getVertexFormat()
                            .uploadImmediateIndexBuffer(
                                    builtBuffer.indexBuffer()
                            );

            indexType =
                    builtBuffer
                            .drawState()
                            .indexType();

        } else {

            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                    RenderSystem.getSequentialBuffer(
                            ORE_LINES_THROUGH_WALLS
                                    .getVertexFormatMode()
                    );

            indices =
                    shapeIndexBuffer.getBuffer(
                            drawState.indexCount()
                    );

            indexType =
                    shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms =
                RenderSystem
                        .getDynamicUniforms()
                        .writeTransform(
                                RenderSystem
                                        .getModelViewMatrixCopy(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX
                        );

        RenderTarget mainTarget =
                client.gameRenderer.mainRenderTarget();

        try (
                RenderPass renderPass =
                        RenderSystem
                                .getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () ->
                                                OreHighlighterClient.MOD_ID
                                                        + " ore highlight rendering",

                                        mainTarget
                                                .getColorTextureView(),

                                        Optional.empty(),

                                        mainTarget
                                                .getDepthTextureView(),

                                        OptionalDouble.empty()
                                )
        ) {

            renderPass.setPipeline(
                    ORE_LINES_THROUGH_WALLS
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
                    vertices
            );

            renderPass.setIndexBuffer(
                    indices,
                    indexType
            );

            renderPass.drawIndexed(
                    drawState.indexCount(),
                    1,
                    0,
                    0,
                    0
            );
        }
    }

    private static void cleanupBuffer() {

        if (buffer != null) {

            buffer = null;
        }
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {

        highlights =
                newHighlights;
    }

    public static void close() {

        if (vertexBuffer != null) {

            vertexBuffer.close();

            vertexBuffer = null;
        }

        ALLOCATOR.close();
    }
}
