package com.hbm.blocks.generic;

import java.util.List;
import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.RefStrings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

/** Мёртвая высокая трава с 7 уровнями заражения, перенесена с 1.12.2 (WasteGrassTall). */
public class BlockWasteGrassTall extends BlockBush {

	protected final String texBase;

	@SideOnly(Side.CLIENT) protected IIcon[] icons;

	public BlockWasteGrassTall(String texBase) {
		super(Material.vine);
		this.texBase = texBase;
		float f = 0.4F;
		this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.8F, 0.5F + f);
	}

	@Override
	protected boolean canPlaceBlockOn(Block block) {
		return block == ModBlocks.waste_earth || block == ModBlocks.waste_mycelium || block == ModBlocks.waste_dirt
				|| block == Blocks.grass || block == Blocks.dirt || block == Blocks.farmland;
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		return null;
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
