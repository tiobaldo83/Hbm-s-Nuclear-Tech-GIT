package com.hbm.blocks.generic;

import java.util.List;

import com.hbm.lib.RefStrings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Сыпучий заражённый блок (песок, гравий) с 7 уровнями заражения, перенесён с 1.12.2 (WasteSand). */
public class BlockWasteLevelFalling extends BlockFalling {

	protected final String texBase;
	protected int potionTicks = 0;
	protected int potionAmp = 0;

	@SideOnly(Side.CLIENT) protected IIcon[] icons;

	public BlockWasteLevelFalling(Material mat, String texBase) {
		super(mat);
		this.texBase = texBase;
	}

	public BlockWasteLevelFalling setWalkRadiation(int ticks, int amplifier) {
		this.potionTicks = ticks;
		this.potionAmp = amplifier;
		return this;
	}

	@Override
	public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
		BlockWasteLevel.applyWalkEffect(world, entity, potionTicks, potionAmp);
	}

	@Override
	public int damageDropped(int meta) {
		return meta;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for(int i = 0; i < BlockWasteLevel.LEVELS; i++) list.add(new ItemStack(item, 1, i));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		icons = new IIcon[BlockWasteLevel.LEVELS];
		for(int i = 0; i < icons.length; i++) icons[i] = reg.registerIcon(RefStrings.MODID + ":" + texBase + "_" + i);
		this.blockIcon = icons[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return icons[Math.abs(meta) % icons.length];
	}
}
