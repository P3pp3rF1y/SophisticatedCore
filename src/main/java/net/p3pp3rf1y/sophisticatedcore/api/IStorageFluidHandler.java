package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public interface IStorageFluidHandler extends ResourceHandler<FluidResource> {
	default int insert(TagKey<Fluid> fluidTag, int amount, Fluid fallbackFluid, TransactionContext tx) {
		return insert(fluidTag, amount, fallbackFluid, tx, false);
	}

	default int insert(TagKey<Fluid> fluidTag, int amount, Fluid fallbackFluid, TransactionContext tx, boolean ignoreInOutLimit) {
		for (int index = 0; index < size(); index++) {
			FluidResource resource = getResource(index);
			if (resource.is(fluidTag)) {
				return insert(index, FluidResource.of(resource.getFluid()), amount, tx, ignoreInOutLimit);
			}
		}
		return insert(FluidResource.of(fallbackFluid), amount, tx, ignoreInOutLimit);
	}

	default int insert(FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		int inserted = 0;
		for (int index = 0; index < size(); index++) {
			inserted += insert(index, resource, amount - inserted, tx, ignoreInOutLimit);
			if (inserted >= amount) {
				return inserted;
			}
		}
		return inserted;
	}

	int insert(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit);

	@Override
	default int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
		return insert(index, resource, amount, tx, false);
	}

	default int extract(int index, TagKey<Fluid> resourceTag, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		FluidResource resource = getResource(index);
		if (resource.is(resourceTag)) {
			return extract(index, resource, amount, tx, ignoreInOutLimit);
		}
		return 0;
	}

	default int extract(TagKey<Fluid> resourceTag, int amount, TransactionContext tx, boolean ignoreInOutLimit) {
		int extracted = 0;
		for (int index = 0; index < size(); index++) {
			extracted += extract(index, resourceTag, amount - extracted, tx, ignoreInOutLimit);
			if (extracted >= amount) {
				return extracted;
			}
		}
		return extracted;
	}

	int extract(int index, FluidResource resource, int amount, TransactionContext tx, boolean ignoreInOutLimit);

	@Override
	default int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
		return extract(index, resource, amount, tx, false);
	}
}
