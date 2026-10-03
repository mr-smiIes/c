
package com.wackyman.orehighlighter.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OreMenuScreen extends Screen {

    private Button oreButton;

    public OreMenuScreen() {
        super(Component.literal("ORE HIGHLIGHTER"));
    }

    @Override
    protected void init() {

        int buttonWidth = 300;

        int buttonLeft =
                (this.width - buttonWidth) / 2;

        oreButton = Button.builder(
                getOreButtonText(),
                button -> {

                    OreHighlighterClient
                            .setOreHighlightEnabled(
                                    !OreHighlighterClient
                                            .isOreHighlightEnabled()
                            );

                    button.setMessage(
                            getOreButtonText()
                    );
                }
        ).bounds(
                buttonLeft,
                this.height / 2 - 10,
                buttonWidth,
                20
        ).build();

        this.addRenderableWidget(
                oreButton
        );
    }

    private Component getOreButtonText() {

        return Component.literal(
                "ORE HIGHLIGHT   [" +
                        (
                                OreHighlighterClient
                                        .isOreHighlightEnabled()
                                        ? "ON"
                                        : "OFF"
                        ) +
                        "]"
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {

        int panelWidth = 360;
        int panelHeight = 190;

        int left =
                (this.width - panelWidth) / 2;

        int top =
                (this.height - panelHeight) / 2;

        int right =
                left + panelWidth;

        int bottom =
                top + panelHeight;

        /*
         * Draw the GUI background FIRST.
         *
         * This is important because super.extractRenderState()
         * extracts the buttons/widgets. If the panel is drawn
         * after super(), it covers the button.
         */

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xB0101016
        );

        graphics.fill(
                left + 4,
                top + 4,
                right + 4,
                bottom + 4,
                0x55000000
        );

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xE91A1B22
        );

        graphics.fill(
                left,
                top,
                right,
                top + 3,
                0xFF63D8FF
        );

        graphics.fill(
                left,
                bottom - 2,
                right,
                bottom,
                0xFF272A35
        );

        /*
         * Title.
         */

        Component title =
                Component.literal(
                        "ORE HIGHLIGHTER"
                );

        graphics.text(
                this.font,
                title,
                this.width / 2
                        - this.font.width(title) / 2,
                top + 22,
                0xFFFFFFFF,
                true
        );

        /*
         * Subtitle.
         */

        Component subtitle =
                Component.literal(
                        "CLIENT-SIDE"
                );

        graphics.text(
                this.font,
                subtitle,
                this.width / 2
                        - this.font.width(subtitle) / 2,
                top + 40,
                0xFF8C93A6,
                false
        );

        /*
         * Description.
         */

        Component description =
                Component.literal(
                        "Colored outlines for nearby ores"
                );

        graphics.text(
                this.font,
                description,
                left + 30,
                top + 70,
                0xFFD9DCE5,
                true
        );

        /*
         * Footer.
         */

        Component footer =
                Component.literal(
                        "K  •  Toggle menu"
                );

        graphics.text(
                this.font,
                footer,
                this.width / 2
                        - this.font.width(footer) / 2,
                bottom - 22,
                0xFF777D8D,
                false
        );

        /*
         * IMPORTANT:
         *
         * Extract the widgets LAST so the button appears
         * above the custom GUI background.
         */
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

