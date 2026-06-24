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

import javax.annotation.Nullable;

import java.util.function.Consumer;

public class TextBox extends WidgetBase {
	private final EditBox editBox;
	@Nullable
	private String unfocusedEmptyHint = null;

	public TextBox(Position position, Dimension dimension) {
		super(position, dimension);
		editBox = new EditBox(minecraft.font, position.x(), position.y(), dimension.width(), dimension.height(), Component.empty());
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		// noop
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		editBox.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		if (editBox.getValue().isEmpty() && unfocusedEmptyHint != null && !editBox.isFocused()) {
			int x = editBox.getX() + editBox.getWidth() / 2 + 2/* editBox.isBordered() ? editBox.getX() + 4 : editBox.getX() */;
			int y = editBox.isBordered() ? editBox.getY() + (editBox.getHeight() - 8) / 2 : editBox.getY();
			guiGraphics.drawCenteredString(this.font, unfocusedEmptyHint, x, y, editBox.textColor);
		}
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
		// noop
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

	public void setValueWithoutNotification(String value) {
		Consumer<String> temp = editBox.responder;
		editBox.setResponder(null);
		editBox.setValue(value);
		editBox.setResponder(temp);
	}

	public void setTextColor(int color) {
		editBox.setTextColor(color);
	}

	public void setTextColorUneditable(int color) {
		editBox.setTextColorUneditable(color);
	}

	public void setBordered(boolean bordered) {
		editBox.setBordered(bordered);

		if (!bordered) {
			editBox.setX(x + 1);
			editBox.setY(y + 1);
			editBox.setWidth(getWidth() - 6);
		} else {
			editBox.setX(x);
			editBox.setY(y);
			editBox.setWidth(getWidth());
		}
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

	public void setUnfocusedEmptyHint(String hint) {
		this.unfocusedEmptyHint = hint;
	}

	@Override
	public void setPosition(Position position) {
		super.setPosition(position);
		editBox.setX(position.x());
		editBox.setY(position.y());
		setBordered(editBox.isBordered());
	}

	@Override
	protected void updateDimensions(int width, int height) {
		super.updateDimensions(width, height);
		editBox.setWidth(width);
		setBordered(editBox.isBordered());
	}
}
