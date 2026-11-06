package com.hbm.blocks.generic;

import com.hbm.hazard.HazardSystem;

import net.minecraft.block.BlockIce;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Заражённый лёд, перенесён с 1.12.2 (WasteIce): облучает того, кто на нём стоит. */
public class BlockWasteIce extends BlockIce {

	public BlockWasteIce() {
		super();
	}

	@Override
	public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
		if(!world.isRemote && entity instanceof EntityLivingBase)
			HazardSystem.applyHazards(new ItemStack(this), (EntityLivingBase) entity);
	}
}
