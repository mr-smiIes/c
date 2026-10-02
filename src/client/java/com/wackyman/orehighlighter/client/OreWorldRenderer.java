package com.wackyman.orehighlighter.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalDouble;

import org.lwjgl.system.MemoryUtil;

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
                            .withDepthTestFunction(
                                    DepthTestFunction.NO_DEPTH_TEST
                            )
                            .build()
            );

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(1f, 1f, 1f, 1f);

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    private static final ByteBufferBuilder ALLOCATOR =
            new ByteBufferBuilder(
                    RenderType.SMALL_BUFFER_SIZE
            );

    private static BufferBuilder buffer;

    private static MappableRingBuffer vertexBuffer;

    private static List<OreHighlighterClient.Highlight> highlights =
            List.of();

    private OreWorldRenderer() {
    }

    public static void initialize() {

        LevelRenderEvents.BEFORE_TRANSLUCENT.register(
                OreWorldRenderer::render
        );
    }

    public static void setHighlights(
            List<OreHighlighterClient.Highlight> newHighlights
    ) {
        highlights = newHighlights;
    }

    private static void render(
            LevelRenderContext context
    ) {

        if (highlights.isEmpty()) {
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        if (client.level == null) {
            return;
        }

        if (buffer == null) {
            buffer = new BufferBuilder(
                    ALLOCATOR,
                    ORE_LINES_THROUGH_WALLS.getVertexFormatMode(),
                    ORE_LINES_THROUGH_WALLS.getVertexFormat()
            );
        }

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
                    buffer,

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

        drawBuffer(
                client,
                ORE_LINES_THROUGH_WALLS
        );
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

        line(
                matrix,
                buffer,
                minX, minY, minZ,
                maxX, minY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                minX, minY, maxZ,
                minX, minY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                minX, minY, minZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );

        line(
                matrix,
                buffer,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );
    }

    private static void line(
            Matrix4fc matrix,
            BufferBuilder buffer,

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

    private static void drawBuffer(
            Minecraft client,
            RenderPipeline pipeline
    ) {

        var builtBuffer =
                buffer.buildOrThrow();

        var drawParameters =
                builtBuffer.drawState();

        VertexFormat format =
                drawParameters.format();

        int vertexBufferSize =
                drawParameters.vertexCount()
                        * format.getVertexSize();

        if (
                vertexBuffer == null
                        || vertexBuffer.size()
                        < vertexBufferSize
        ) {

            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer =
                    new MappableRingBuffer(
                            () ->
                                    OreHighlighterClient.MOD_ID
                                            + " ore highlight",

                            com.mojang.blaze3d.buffers.GpuBuffer
                                    .USAGE_VERTEX
                                    |
                                    com.mojang.blaze3d.buffers.GpuBuffer
                                            .USAGE_MAP_WRITE,

                            vertexBufferSize
                    );
        }

        var commandEncoder =
                RenderSystem
                        .getDevice()
                        .createCommandEncoder();

        try (
                var mapped =
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

            MemoryUtil.memCopy(
                    builtBuffer.vertexBuffer(),
                    mapped.data()
            );
        }

        draw(
                client,
                pipeline,
                builtBuffer,
                drawParameters,
                vertexBuffer.currentBuffer(),
                format
        );

        builtBuffer.close();

        vertexBuffer.rotate();

        buffer = null;
    }

    private static void draw(
            Minecraft client,
            RenderPipeline pipeline,
            com.mojang.blaze3d.vertex.MeshData builtBuffer,
            com.mojang.blaze3d.vertex.MeshData.DrawState drawParameters,
            com.mojang.blaze3d.buffers.GpuBuffer vertices,
            VertexFormat format
    ) {

        com.mojang.blaze3d.buffers.GpuBuffer indices;

        VertexFormat.IndexType indexType;

        if (
                pipeline.getVertexFormatMode()
                        == VertexFormat.Mode.QUADS
        ) {

            builtBuffer.sortQuads(
                    ALLOCATOR,
                    RenderSystem
                            .getProjectionType()
                            .vertexSorting()
            );

            indices =
                    pipeline
                            .getVertexFormat()
                            .uploadImmediateIndexBuffer(
                                    builtBuffer.indexBuffer()
                            );

            indexType =
                    drawParameters.indexType();

        } else {

            RenderSystem.AutoStorageIndexBuffer
                    shapeIndexBuffer =
                    RenderSystem.getSequentialBuffer(
                            pipeline.getVertexFormatMode()
                    );

            indices =
                    shapeIndexBuffer.getBuffer(
                            drawParameters.indexCount()
                    );

            indexType =
                    shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms =
                RenderSystem
                        .getDynamicUniforms()
                        .writeTransform(
                                RenderSystem.getModelViewMatrix(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX
                        );

        RenderTarget target =
                client.getMainRenderTarget();

        try (
                RenderPass renderPass =
                        RenderSystem
                                .getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () ->
                                                OreHighlighterClient.MOD_ID
                                                        + " ore highlight rendering",

                                        target
                                                .getColorTextureView(),

                                        OptionalInt.empty(),

                                        target
                                                .getDepthTextureView(),

                                        OptionalDouble.empty()
                                )
        ) {

            renderPass.setPipeline(
                    pipeline
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
                    0,
                    0,
                    drawParameters.indexCount(),
                    1
            );
        }
    }

    public static void close() {

        ALLOCATOR.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
