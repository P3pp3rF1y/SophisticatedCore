package net.p3pp3rf1y.sophisticatedcore.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class IrisCompat implements ICompat {
	@Override
	public void setup() {
		IrisApi.getInstance().assignPipeline(BlockHighlightRenderHelper.THICK_HIGHLIGHT_PIPELINE, IrisProgram.BASIC);
	}
}
