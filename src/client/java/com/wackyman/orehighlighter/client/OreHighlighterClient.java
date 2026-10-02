package com.wackyman.orehighlighter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreHighlighterClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static boolean oreHighlightEnabled = false;
    private static int scanTimer = 0;

    private record Highlight(BlockPos pos, float r, float g, float b) {}

    private static final List<Highlight> highlights = new ArrayList<>();
    private static final Map<Block, float[]> ORE_COLORS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        registerOreColors();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.orehighlighter.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.orehighlighter"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new OreMenuScreen());
            }

            if (oreHighlightEnabled && client.world != null && client.player != null) {
                if (++scanTimer >= 8) {
                    scanTimer = 0;
                    scanOres(client);
                }
            } else if (!oreHighlightEnabled) {
                highlights.clear();
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!oreHighlightEnabled || highlights.isEmpty()) return;

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) return;

            var camera = context.camera();
            double cameraX = camera.getPos().x;
            double cameraY = camera.getPos().y;
            double cameraZ = camera.getPos().z;

            matrices.push();
            matrices.translate(-cameraX, -cameraY, -cameraZ);

            VertexConsumer vertexConsumer = consumers.getBuffer(RenderLayer.getLines());
            for (Highlight highlight : highlights) {
                WorldRendererAccess.drawBox(
                        matrices,
                        vertexConsumer,
                        new Box(highlight.pos()),
                        highlight.r(),
                        highlight.g(),
                        highlight.b(),
                        1.0f
                );
            }

            matrices.pop();
        });
    }

    private static void registerOreColors() {
        ORE_COLORS.put(Blocks.COAL_ORE, rgb(.10f, .10f, .10f));
        ORE_COLORS.put(Blocks.DEEPSLATE_COAL_ORE, rgb(.16f, .16f, .16f));
        ORE_COLORS.put(Blocks.IRON_ORE, rgb(.95f, .95f, .95f));
        ORE_COLORS.put(Blocks.DEEPSLATE_IRON_ORE, rgb(.85f, .85f, .90f));
        ORE_COLORS.put(Blocks.COPPER_ORE, rgb(1f, .45f, .15f));
        ORE_COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, rgb(1f, .35f, .20f));
        ORE_COLORS.put(Blocks.GOLD_ORE, rgb(1f, .80f, .05f));
        ORE_COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, rgb(1f, .65f, .02f));
        ORE_COLORS.put(Blocks.REDSTONE_ORE, rgb(1f, .05f, .03f));
        ORE_COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, rgb(1f, .03f, .05f));
        ORE_COLORS.put(Blocks.LAPIS_ORE, rgb(.10f, .30f, 1f));
        ORE_COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, rgb(.08f, .22f, .90f));
        ORE_COLORS.put(Blocks.DIAMOND_ORE, rgb(.10f, .95f, 1f));
        ORE_COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, rgb(.05f, .80f, 1f));
        ORE_COLORS.put(Blocks.EMERALD_ORE, rgb(.05f, 1f, .30f));
        ORE_COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, rgb(.03f, .90f, .25f));
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE, rgb(1f, .65f, .02f));
        ORE_COLORS.put(Blocks.NETHER_QUARTZ_ORE, rgb(1f, .85f, .75f));
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS, rgb(.65f, .25f, .15f));
    }

    private static float[] rgb(float r, float g, float b) {
        return new float[]{r, g, b};
    }

    private static void scanOres(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        BlockPos center = client.player.getBlockPos();
        List<Highlight> found = new ArrayList<>();

        int horizontalRadius = 24;
        int verticalRadius = 16;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = center.getX() - horizontalRadius; x <= center.getX() + horizontalRadius; x++) {
            for (int y = center.getY() - verticalRadius; y <= center.getY() + verticalRadius; y++) {
                for (int z = center.getZ() - horizontalRadius; z <= center.getZ() + horizontalRadius; z++) {
                    mutable.set(x, y, z);
                    Block block = client.world.getBlockState(mutable).getBlock();
                    float[] color = ORE_COLORS.get(block);

                    if (color != null) {
                        found.add(new Highlight(
                                mutable.toImmutable(),
                                color[0],
                                color[1],
                                color[2]
                        ));
                    }
                }
            }
        }

        highlights.clear();
        highlights.addAll(found);
    }

    public static boolean isOreHighlightEnabled() {
        return oreHighlightEnabled;
    }

    public static void setOreHighlightEnabled(boolean enabled) {
        oreHighlightEnabled = enabled;
        scanTimer = 0;

        if (!enabled) {
            highlights.clear();
        }
    }
}
