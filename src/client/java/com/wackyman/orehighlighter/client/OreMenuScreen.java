package com.wackyman.orehighlighter.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OreMenuScreen extends Screen {
    private ButtonWidget oreButton;

    public OreMenuScreen() {
        super(Text.literal("ORE HIGHLIGHTER"));
    }

    @Override
    protected void init() {
        int panelWidth = 360;
        int buttonWidth = 300;
        int buttonLeft = (this.width - buttonWidth) / 2;

        oreButton = ButtonWidget.builder(getOreButtonText(), button -> {
            OreHighlighterClient.setOreHighlightEnabled(
                    !OreHighlighterClient.isOreHighlightEnabled()
            );
            button.setMessage(getOreButtonText());
        }).dimensions(
                buttonLeft,
                this.height / 2 - 10,
                buttonWidth,
                28
        ).build();

        this.addDrawableChild(oreButton);
    }

    private Text getOreButtonText() {
        return Text.literal(
                "ORE HIGHLIGHT   [" +
                (OreHighlighterClient.isOreHighlightEnabled() ? "ON" : "OFF") +
                "]"
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0101016);

        int panelWidth = 360;
        int panelHeight = 190;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        context.fill(left + 4, top + 4, right + 4, bottom + 4, 0x55000000);
        context.fill(left, top, right, bottom, 0xE91A1B22);
        context.fill(left, top, right, top + 3, 0xFF63D8FF);
        context.fill(left, bottom - 2, right, bottom, 0xFF272A35);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("ORE HIGHLIGHTER"),
                this.width / 2,
                top + 22,
                0xFFFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("CLIENT-SIDE"),
                this.width / 2,
                top + 40,
                0xFF8C93A6
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Colored outlines for nearby ores"),
                left + 30,
                top + 70,
                0xFFD9DCE5
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("K  •  Toggle menu"),
                this.width / 2,
                bottom - 22,
                0xFF777D8D
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
