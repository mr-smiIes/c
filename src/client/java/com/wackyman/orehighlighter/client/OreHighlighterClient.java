package com.wackyman.orehighlighter.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreHighlighterClient implements ClientModInitializer {

    public static final String MOD_ID = "orehighlighter";

    private static final int MAX_HIGHLIGHTS = 100;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            MOD_ID,
                            "main"
                    )
            );

    private static KeyMapping openMenuKey;

    private static boolean oreHighlightEnabled = false;

    private static int scanTimer = 0;

    private static final List<Highlight> highlights =
            new ArrayList<>();

    private static final Map<Block, float[]> ORE_COLORS =
            new HashMap<>();

    public record Highlight(
            BlockPos pos,
            float r,
            float g,
            float b
    ) {
    }

    @Override
    public void onInitializeClient() {

        registerOreColors();

        openMenuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.orehighlighter.open_menu",
                        InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_K,
                        CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new OreMenuScreen());
            }

            if (
                    oreHighlightEnabled
                            && client.level != null
                            && client.player != null
            ) {
                if (++scanTimer >= 8) {
                    scanTimer = 0;
                    scanOres(client);
                }
            } else if (!oreHighlightEnabled) {
                highlights.clear();
            }
        });

        LevelRenderEvents.COLLECT_SUBMITS.register(
                WorldRendererAccess::renderHighlights
        );
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

    private static float[] rgb(
            float r,
            float g,
            float b
    ) {
        return new float[]{r, g, b};
    }

    private static void scanOres(Minecraft client) {

        if (
                client.player == null
                        || client.level == null
        ) {
            return;
        }

        BlockPos center =
                client.player.blockPosition();

        List<Highlight> found =
                new ArrayList<>();

        int horizontalRadius = 24;
        int verticalRadius = 16;

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (
                int x = center.getX() - horizontalRadius;
                x <= center.getX() + horizontalRadius;
                x++
        ) {

            for (
                    int y = center.getY() - verticalRadius;
                    y <= center.getY() + verticalRadius;
                    y++
            ) {

                for (
                        int z = center.getZ() - horizontalRadius;
                        z <= center.getZ() + horizontalRadius;
                        z++
                ) {

                    if (found.size() >= MAX_HIGHLIGHTS) {
                        break;
                    }

                    mutable.set(x, y, z);

                    Block block =
                            client.level
                                    .getBlockState(mutable)
                                    .getBlock();

                    float[] color =
                            ORE_COLORS.get(block);

                    if (color != null) {
                        found.add(
                                new Highlight(
                                        mutable.immutable(),
                                        color[0],
                                        color[1],
                                        color[2]
                                )
                        );
                    }
                }

                if (found.size() >= MAX_HIGHLIGHTS) {
                    break;
                }
            }

            if (found.size() >= MAX_HIGHLIGHTS) {
                break;
            }
        }

        highlights.clear();
        highlights.addAll(found);
    }

    public static List<Highlight> getHighlights() {
        return List.copyOf(highlights);
    }

    public static boolean isOreHighlightEnabled() {
        return oreHighlightEnabled;
    }

    public static void setOreHighlightEnabled(
            boolean enabled
    ) {
        oreHighlightEnabled = enabled;
        scanTimer = 0;

        if (!enabled) {
            highlights.clear();
        }
    }
}
