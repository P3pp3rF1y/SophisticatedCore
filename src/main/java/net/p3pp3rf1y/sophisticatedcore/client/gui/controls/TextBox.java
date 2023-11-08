package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.lwjgl.glfw.GLFW;

public class TextBox extends WidgetBase {
	private final EditBox editBox;

	public TextBox(Position position, Dimension dimension) {
		super(position, dimension);
		editBox = new EditBox(minecraft.font, position.x(), position.y(), dimension.width(), dimension.height(), Component.empty());
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		editBox.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		poseStack.popPose();
	}

	public void setFocus(boolean focused) {
		if (editBox.isFocused() != focused) {
			editBox.setFocused(focused);
		}
		setFocused(focused);
	}

	@Override
	public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
		if (!editBox.isFocused()) {
			return false;
		}
		editBox.keyPressed(pKeyCode, pScanCode, pModifiers);
		if (pKeyCode == GLFW.GLFW_KEY_ENTER) {
			onEnterPressed();
		}
		return pKeyCode != GLFW.GLFW_KEY_ESCAPE;
	}

	protected void onEnterPressed() {
		//noop
	}

	public String getValue() {
		return editBox.getValue();
	}

	@Override
	public boolean charTyped(char pCodePoint, int pModifiers) {
		return editBox.charTyped(pCodePoint, pModifiers);
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		editBox.updateNarration(pNarrationElementOutput);
	}

	public void setValue(String value) {
		editBox.setValue(value);
	}
}
