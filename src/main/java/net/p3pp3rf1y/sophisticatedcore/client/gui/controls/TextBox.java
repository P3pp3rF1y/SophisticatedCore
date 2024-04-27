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

import java.util.function.Consumer;

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

	@Override
	public void setFocused(boolean focused) {
		if (editBox.isFocused() != focused) {
			editBox.setFocused(focused);
		}
		super.setFocused(focused);
	}
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!editBox.isFocused()) {
			return false;
		}
		editBox.keyPressed(keyCode, scanCode, modifiers);
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			onEnterPressed();
		}
		return keyCode != GLFW.GLFW_KEY_ESCAPE;
	}

	protected void onEnterPressed() {
		//noop
	}

	public String getValue() {
		return editBox.getValue();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return editBox.charTyped(codePoint, modifiers);
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		editBox.updateNarration(narrationElementOutput);
	}

	public void setValue(String value) {
		editBox.setValue(value);
	}

	public void setTextColor(int color) {
		editBox.setTextColor(color);
	}

	public void setTextColorUneditable(int color) {
		editBox.setTextColorUneditable(color);
	}

	public void setBordered(boolean bordered) {
		editBox.setBordered(bordered);
	}

	public void setMaxLength(int maxLength) {
		editBox.setMaxLength(maxLength);
	}

	public void setResponder(Consumer<String> responder) {
		editBox.setResponder(responder);
	}

	public void setEditable(boolean editable) {
		editBox.setEditable(editable);
	}

	public boolean isEditable() {
		return editBox.isEditable();
	}
}