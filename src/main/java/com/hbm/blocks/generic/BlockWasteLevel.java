package com.hbm.blocks.generic;

import java.util.List;
import java.util.Random;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.lib.RefStrings;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ContaminationUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * Заражённый блок с 7 уровнями заражения (meta 0..6), перенесён с 1.12.2
 * (waste_dirt, waste_sandstone, waste_terracotta, waste_snow_block и т.п.).
 * Уровень выбирается по расстоянию до эпицентра взрыва (EntityFalloutRain.placeBlockFromDist).
 */
public class BlockWasteLevel extends Block {

	public static final int LEVELS = 7;

	/** путь текстуры без номера уровня, например "contamination/dirt/waste_dirt" */
	protected final String texBase;
	/** true: отдельные текстуры _top_N, _bottom_N, _normal_N (как у песчаника) */
	protected final boolean topBottom;
	/** радиация в чанк за срабатывание (раз в секунду) на первом уровне; на уровне N — (N+1) раз больше. 0 = не излучает */
	protected float chunkRad = 0F;
	/** эффект радиации при ходьбе: длительность в тиках и сила; potionTicks = 0 — без эффекта */
	protected int potionTicks = 0;
	protected int potionAmp = 0;

	@SideOnly(Side.CLIENT) protected IIcon[] icons;
	@SideOnly(Side.CLIENT) protected IIcon[] iconsTop;
	@SideOnly(Side.CLIENT) protected IIcon[] iconsBottom;

	public BlockWasteLevel(Material mat, String texBase, boolean topBottom) {
		super(mat);
		this.texBase = texBase;
		this.topBottom = topBottom;
	}

	public BlockWasteLevel setChunkRad(float rad) {
		this.chunkRad = rad;
		return this;
	}

	public BlockWasteLevel setWalkRadiation(int ticks, int amplifier) {
		this.potionTicks = ticks;
		this.potionAmp = amplifier;
		return this;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		if(chunkRad > 0) world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
	}

	@Override
	public int tickRate(World world) {
		return chunkRad > 0 ? 20 : super.tickRate(world);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if(chunkRad > 0 && !world.isRemote) {
			int meta = world.getBlockMetadata(x, y, z);
			ChunkRadiationManager.proxy.incrementRad(world, x, y, z, chunkRad * (Math.min(meta, LEVELS - 1) + 1));
			world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
		}
	}

	@Override
	public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
		applyWalkEffect(world, entity, potionTicks, potionAmp);
	}

	static void applyWalkEffect(World world, Entity entity, int ticks, int amp) {
		if(ticks <= 0 || world.isRemote || !(entity instanceof EntityLivingBase)) return;
		EntityLivingBase living = (EntityLivingBase) entity;
		if(ContaminationUtil.isRadImmune(living)) return;
		living.addPotionEffect(new PotionEffect(HbmPotion.radiation.id, ticks, amp));
	}

	@Override
	public int damageDropped(int meta) {
		return meta;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for(int i = 0; i < LEVELS; i++) list.add(new ItemStack(item, 1, i));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		icons = new IIcon[LEVELS];
		if(topBottom) {
			iconsTop = new IIcon[LEVELS];
			iconsBottom = new IIcon[LEVELS];
		}
		for(int i = 0; i < LEVELS; i++) {
			if(topBottom) {
				icons[i] = reg.registerIcon(RefStrings.MODID + ":" + texBase + "_normal_" + i);
				iconsTop[i] = reg.registerIcon(RefStrings.MODID + ":" + texBase + "_top_" + i);
				iconsBottom[i] = reg.registerIcon(RefStrings.MODID + ":" + texBase + "_bottom_" + i);
			} else {
				icons[i] = reg.registerIcon(RefStrings.MODID + ":" + texBase + "_" + i);
			}
		}
		this.blockIcon = icons[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		int m = Math.abs(meta) % LEVELS;
		if(topBottom) {
			if(side == 0) return iconsBottom[m];
			if(side == 1) return iconsTop[m];
		}
		return icons[m];
	}
}
