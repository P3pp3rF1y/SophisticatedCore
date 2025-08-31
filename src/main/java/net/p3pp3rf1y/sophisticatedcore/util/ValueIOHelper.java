package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Consumer;

public class ValueIOHelper {
	private static final Logger SLF4J_LOGGER = LoggerFactory.getLogger(SophisticatedCore.MOD_ID + "_slf4j");

	private ValueIOHelper() {
	}

	public static CompoundTag collectOutputToTag(HolderLookup.Provider registries, Consumer<ValueOutput> saveConsumer) {
		try (ProblemReporter.ScopedCollector collector = new ProblemReporter.ScopedCollector(SLF4J_LOGGER)) {
			TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(collector, registries);
			saveConsumer.accept(tagvalueoutput);
			return tagvalueoutput.buildResult();
		}
	}

	public static ValueInput inputFromCompoundTag(HolderLookup.Provider registries, CompoundTag tag) {
		try (ProblemReporter.ScopedCollector collector = new ProblemReporter.ScopedCollector(SLF4J_LOGGER)) {
			return TagValueInput.create(collector, registries, tag);
		}
	}

	public static <T> void saveList(ValueOutput out, String key, Collection<T> elements, Codec<T> codec) {
		if (elements.isEmpty()) {
			return;
		}

		ValueOutput.TypedOutputList<T> list = out.list(key, codec);
		for (T element : elements) {
			list.add(element);
		}
	}
}
