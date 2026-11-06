package com.hbm.blocks.generic;

import java.util.List;
import java.util.Random;

import com.hbm.lib.RefStrings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Тонкий слой заражённого снега с 7 уровнями заражения, перенесён с 1.12.2 (BlockPowder/waste_snow). */
public class BlockWastePowder extends Block {

	protected final String texBase;
	protected int potionTicks = 2 * 60 * 20;
	protected int potionAmp = 14;

	@SideOnly(Side.CLIENT) protected IIcon[] icons;

	public BlockWastePowder(Material mat, String texBase) {
		super(mat);
		this.texBase = texBase;
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
	}

	@Override public boolean isOpaqueCube() { return false; }
	@Override public boolean renderAsNormalBlock() { return false; }
	@Override public boolean isReplaceable(IBlockAccess world, int x, int y, int z) { return true; }
	@Override public Item getItemDropped(int meta, Random rand, int fortune) { return null; }

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y - 1, z);
		if(block == Blocks.ice || block == Blocks.packed_ice) return false;
		if(block.isLeaves(world, x, y - 1, z) && !block.isAir(world, x, y - 1, z)) return true;
		return block.isOpaqueCube() && block.getMaterial().blocksMovement();
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block b) {
		if(!this.canPlaceBlockAt(world, x, y, z)) world.setBlockToAir(x, y, z);
	}

	@Override
	public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
		BlockWasteLevel.applyWalkEffect(world, entity, potionTicks, potionAmp);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		BlockWasteLevel.applyWalkEffect(world, entity, potionTicks, potionAmp);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		return side == 1 ? true : super.shouldSideBeRendered(world, x, y, z, side);
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
