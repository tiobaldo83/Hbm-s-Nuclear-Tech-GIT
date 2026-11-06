package com.hbm.entity.effect;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.dim.orbit.BiomeGenOrbit;
import com.hbm.config.WorldConfig;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.logic.EntityExplosionChunkloading;
import com.hbm.world.WorldUtil;
import com.hbm.world.biome.BiomeGenCraterBase;

import net.minecraft.block.Block;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class EntityFalloutRain extends EntityExplosionChunkloading {

	private boolean firstTick = true; // Of course Vanilla has it private in Entity...
	private boolean salted = false;
	public EntityFalloutRain(World p_i1582_1_) {
		super(p_i1582_1_);
		this.setSize(4, 20);
		this.ignoreFrustumCheck = true;
		this.isImmuneToFire = true;
	}

	public EntityFalloutRain(World p_i1582_1_, int maxAge) {
		super(p_i1582_1_);
		this.setSize(4, 20);
		this.isImmuneToFire = true;
	}

	private int tickDelay = BombConfig.fDelay;

	@Override
	public void onUpdate() {

		if(!worldObj.isRemote) {

			long start = System.currentTimeMillis();

			if(firstTick) {
				if(chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) gatherChunks();
				firstTick = false;
			}

			if(tickDelay == 0) {
				tickDelay = BombConfig.fDelay;

				while(System.currentTimeMillis() < start + BombConfig.mk5) {
					if(!chunksToProcess.isEmpty()) {
						long chunkPos = chunksToProcess.remove(chunksToProcess.size() - 1); // Just so it doesn't shift the whole list every time
						int chunkPosX = (int) (chunkPos & Integer.MAX_VALUE);
						int chunkPosZ = (int) (chunkPos >> 32 & Integer.MAX_VALUE);
						boolean biomeModified = false;
						for(int x = chunkPosX << 4; x < (chunkPosX << 4) + 16; x++) {
							for(int z = chunkPosZ << 4; z < (chunkPosZ << 4) + 16; z++) {
								double percent = Math.hypot(x - posX, z - posZ) * 100 / getScale();
								stomp(x, z, percent);
								BiomeGenBase biome = getBiomeChange(percent, getScale(), worldObj.getBiomeGenForCoords(x, z));
								if(biome != null) {
									WorldUtil.setBiome(worldObj, x, z, biome);
									biomeModified = true;
								}
							}
						}
						if(biomeModified) WorldUtil.syncBiomeChange(worldObj, chunkPosX << 4, chunkPosZ << 4);

					} else if (!outerChunksToProcess.isEmpty()) {
						long chunkPos = outerChunksToProcess.remove(outerChunksToProcess.size() - 1);
						int chunkPosX = (int) (chunkPos & Integer.MAX_VALUE);
						int chunkPosZ = (int) (chunkPos >> 32 & Integer.MAX_VALUE);
						boolean biomeModified = false;
						for(int x = chunkPosX << 4; x < (chunkPosX << 4) + 16; x++) {
							for(int z = chunkPosZ << 4; z < (chunkPosZ << 4) + 16; z++) {
								double distance = Math.hypot(x - posX, z - posZ);
								if(distance <= getScale()) {
									double percent = distance * 100 / getScale();
									stomp(x, z, percent);
									BiomeGenBase biome = getBiomeChange(percent, getScale(), worldObj.getBiomeGenForCoords(x, z));
									if(biome != null) {
										WorldUtil.setBiome(worldObj, x, z, biome);
										biomeModified = true;
									}
								}
							}
						}
						if(biomeModified) WorldUtil.syncBiomeChange(worldObj, chunkPosX << 4, chunkPosZ << 4);

					} else {
						this.clearChunkLoader();
						this.setDead();
						break;
					}
				}
			}

			tickDelay--;
		}
	}

	public static BiomeGenBase getBiomeChange(double dist, int scale, BiomeGenBase original) {
		if(!WorldConfig.enableCraterBiomes) return null;
		if(original instanceof BiomeGenOrbit) return null; // Don't alter orbit biomes
		if(scale >= 150 && dist < 15)
			return BiomeGenCraterBase.craterInnerBiome;
		if(scale >= 100 && dist < 55 && original != BiomeGenCraterBase.craterInnerBiome)
			return BiomeGenCraterBase.craterBiome;
		if(scale >= 25 && original != BiomeGenCraterBase.craterInnerBiome && original != BiomeGenCraterBase.craterBiome)
			return BiomeGenCraterBase.craterOuterBiome;
		return null;
	}

	private final List<Long> chunksToProcess = new ArrayList<>();
	private final List<Long> outerChunksToProcess = new ArrayList<>();

	// Is it worth the effort to split this into a method that can be called over multiple ticks? I'd say it's fast enough anyway...
	private void gatherChunks() {
		Set<Long> chunks = new LinkedHashSet<>(); // LinkedHashSet preserves insertion order
		Set<Long> outerChunks = new LinkedHashSet<>();
		int outerRange = getScale();
		// Basically defines something like the step size, but as indirect proportion. The actual angle used for rotation will always end up at 360° for angle == adjustedMaxAngle
		// So yea, I mathematically worked out that 20 is a good value for this, with the minimum possible being 18 in order to reach all chunks
		int adjustedMaxAngle = 20 * outerRange / 32; // step size = 20 * chunks / 2
		for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
			Vec3 vector = Vec3.createVectorHelper(outerRange, 0, 0);
			vector.rotateAroundY((float) (angle * Math.PI / 180.0 / (adjustedMaxAngle / 360.0))); // Ugh, mutable data classes (also, ugh, radians; it uses degrees in 1.18; took me two hours to debug)
			outerChunks.add(ChunkCoordIntPair.chunkXZ2Int((int) (posX + vector.xCoord) >> 4, (int) (posZ + vector.zCoord) >> 4));
		}
		for (int distance = 0; distance <= outerRange; distance += 8) for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
			Vec3 vector = Vec3.createVectorHelper(distance, 0, 0);
			vector.rotateAroundY((float) (angle * Math.PI / 180.0 / (adjustedMaxAngle / 360.0)));
			long chunkCoord = ChunkCoordIntPair.chunkXZ2Int((int) (posX + vector.xCoord) >> 4, (int) (posZ + vector.zCoord) >> 4);
			if (!outerChunks.contains(chunkCoord)) chunks.add(chunkCoord);
		}

		chunksToProcess.addAll(chunks);
		outerChunksToProcess.addAll(outerChunks);
		Collections.reverse(chunksToProcess); // So it starts nicely from the middle
		Collections.reverse(outerChunksToProcess);
	}

	/*
	 * Пороги расстояния как в 1.12.2 (EntityFalloutRain.setScale): там s0..s6 = 0.8..0.1 * радиус,
	 * здесь dist уже в процентах от радиуса, поэтому пороги в процентах.
	 */
	private static final double S0 = 80, S1 = 65, S2 = 50, S3 = 40, S4 = 30, S5 = 20, S6 = 10;

	/** Сколько слоёв камня заражать на этом расстоянии (1.12.2 getMaxStoneDepth). */
	private static int getMaxStoneDepth(double dist) {
		if(dist > S1) return 0;
		if(dist > S2) return 1;
		if(dist > S3) return 2;
		if(dist > S4) return 3;
		if(dist > S5) return 4;
		if(dist > S6) return 5;
		return 6;
	}

	/** Уровень заражения 0..6 по расстоянию со случайным разбросом (1.12.2 placeBlockFromDist). */
	private int levelFromDist(double dist) {
		double ranDist = dist * (1D + rand.nextDouble() * 0.2D);
		if(ranDist > S1) return 0;
		if(ranDist > S2) return 1;
		if(ranDist > S3) return 2;
		if(ranDist > S4) return 3;
		if(ranDist > S5) return 4;
		if(ranDist > S6) return 5;
		return 6;
	}

	private void placeFromDist(double dist, Block block, int x, int y, int z) {
		setBlock(x, y, z, block, levelFromDist(dist));
	}

	/** Уровень селлафита для камня (1.12.2): -1 = погашенный селлафит, 0..5 = селлафит ... селлафит-кориум. */
	private int sellafieldLevel(double dist, int stoneDepth, int maxStoneDepth) {
		double ranDist = dist * (1D + rand.nextDouble() * 0.1D);
		if(ranDist > S1 || stoneDepth == maxStoneDepth) return -1;
		if(ranDist > S2 || stoneDepth == maxStoneDepth - 1) return 0;
		if(ranDist > S3 || stoneDepth == maxStoneDepth - 2) return 1;
		if(ranDist > S4 || stoneDepth == maxStoneDepth - 3) return 2;
		if(ranDist > S5 || stoneDepth == maxStoneDepth - 4) return 3;
		if(ranDist > S6 || stoneDepth == maxStoneDepth - 5) return 4;
		return 5;
	}

	/**
	 * Заражение столбца блоков по правилам 1.12.2: трава, земля, песок, гравий, снег, лёд, песчаник,
	 * глина превращаются в заражённые версии с уровнем по расстоянию, камень — в селлафит по глубине.
	 * Блоки, которых нет в правилах 1.12.2 (руды NTM, особые блоки 1.7.10), обрабатываются как раньше —
	 * через hbmFallout.json (FalloutConfigJSON).
	 */
	private void stomp(int x, int z, double dist) {

		// за пределами 80% радиуса заражение редеет (1.12.2)
		if(dist > S0 && rand.nextFloat() > 0.05F + (5F * (float) (S0 / dist) - 4F)) return;

		int maxStoneDepth = getMaxStoneDepth(dist);
		int stoneDepth = 0;
		boolean reachedStone = false;
		boolean lastReachedStone;
		int contactHeight = -1;
		int solidDepth = 0;

		for(int y = 255; y >= 0; y--) {

			Block b = worldObj.getBlock(x, y, z);
			Material mat = b.getMaterial();
			int meta = worldObj.getBlockMetadata(x, y, z);
			lastReachedStone = reachedStone;

			if(mat != Material.air && contactHeight == -1) contactHeight = Math.min(y + 1, 255);

			if(reachedStone && mat != Material.air) {
				stoneDepth++;
			} else {
				reachedStone = mat == Material.rock;
			}
			if(reachedStone && stoneDepth > maxStoneDepth) break;

			if(mat == Material.air || mat.isLiquid() || b == ModBlocks.fallout || b == ModBlocks.salted_fallout) continue;

			if(b == ModBlocks.volcano_core) {
				worldObj.setBlock(x, y, z, ModBlocks.volcano_rad_core, meta, 3);
				continue;
			}

			if(b == Blocks.bedrock || b == ModBlocks.ore_bedrock_oil || b == ModBlocks.ore_bedrock) {
				if(worldObj.isAirBlock(x, y + 1, z)) setBlock(x, y + 1, z, ModBlocks.toxic_block);
				break;
			}

			// слой осадков на поверхности (1.7.10, с поддержкой солёной бомбы)
			Block ab = worldObj.getBlock(x, y + 1, z);
			if(y == contactHeight - 1 && (ab == Blocks.air || (ab.isReplaceable(worldObj, x, y + 1, z) && !ab.getMaterial().isLiquid()))) {
				double d = dist / 100;
				double chance = 0.1 - Math.pow((d - 0.7) * 1.0, 2);
				Block layer = this.salted ? ModBlocks.salted_fallout : ModBlocks.fallout;
				if(chance >= rand.nextDouble() && ModBlocks.fallout.canPlaceBlockAt(worldObj, x, y + 1, z)) setBlock(x, y + 1, z, layer);
			}

			if(dist < 65 && b.isFlammable(worldObj, x, y, z, ForgeDirection.UP)) {
				if(rand.nextInt(5) == 0 && worldObj.getBlock(x, y + 1, z).isAir(worldObj, x, y + 1, z)) setBlock(x, y + 1, z, Blocks.fire);
			}

			if(convert1122(x, y, z, b, meta, dist, stoneDepth, maxStoneDepth, reachedStone, lastReachedStone)) {
				if(b.isNormalCube()) solidDepth++;
				letFall(x, y, z, dist, solidDepth);
				continue;
			}

			// всё остальное — старые правила 1.7.10 из hbmFallout.json
			boolean eval = false;
			for(FalloutEntry entry : FalloutConfigJSON.entries) {
				if(entry.eval(worldObj, x, y, z, b, meta, dist)) {
					if(entry.isSolid()) solidDepth++;
					eval = true;
					break;
				}
			}
			if(!eval && b.isNormalCube()) solidDepth++;

			letFall(x, y, z, dist, solidDepth);

			// очень прочные блоки (бункеры и т.п.) защищают то, что под ними (1.12.2)
			if(b.getExplosionResistance(null) > 300) break;
			// без камня: как в 1.7.10, не глубже трёх твёрдых слоёв
			if(!reachedStone && solidDepth >= 3 + maxStoneDepth) break;
		}
	}

	/** Обрушение висящих блоков у эпицентра (логика 1.7.10). */
	private void letFall(int x, int y, int z, double dist, int depth) {
		Block b = worldObj.getBlock(x, y, z);
		float hardness = b.getBlockHardness(worldObj, x, y, z);
		if(y > 0 && dist < 65 && hardness <= Blocks.stonebrick.getExplosionResistance(null) && hardness >= 0 && !(b instanceof BlockDummyable)) {
			if(worldObj.getBlock(x, y - 1, z) == Blocks.air) {
				for(int i = 0; i <= Math.min(depth, 3); i++) {
					Block block = worldObj.getBlock(x, y + i, z);
					hardness = block.getBlockHardness(worldObj, x, y + i, z);
					if(block.getMaterial() != Material.air && hardness <= Blocks.stonebrick.getExplosionResistance(null) && hardness >= 0) {
						EntityFallingBlockNT entityfallingblock = new EntityFallingBlockNT(worldObj, x + 0.5D, y + 0.5D + i, z + 0.5D, block, worldObj.getBlockMetadata(x, y + i, z));
						entityfallingblock.canDrop = false;
						worldObj.spawnEntityInWorld(entityfallingblock);
					}
				}
			}
		}
	}

	/** Правила превращения блоков из 1.12.2 (EntityFalloutRain.doFallout). true — блок обработан. */
	private boolean convert1122(int x, int y, int z, Block b, int meta, double dist, int stoneDepth, int maxStoneDepth, boolean reachedStone, boolean lastReachedStone) {

		if(b == ModBlocks.waste_leaves) {
			if(!(dist > S1)) worldObj.setBlockToAir(x, y, z);
			return true;
		}
		if(b instanceof BlockLeaves) {
			if(dist > S1) setBlock(x, y, z, ModBlocks.waste_leaves);
			else worldObj.setBlockToAir(x, y, z);
			return true;
		}
		if(b == Blocks.brown_mushroom || b == Blocks.red_mushroom) {
			if(dist < S0) setBlock(x, y, z, ModBlocks.mush);
			return true;
		}
		if(b == Blocks.brown_mushroom_block || b == Blocks.red_mushroom_block) {
			if(dist < S0) setBlock(x, y, z, meta == 10 ? ModBlocks.mush_block_stem : ModBlocks.mush_block);
			return true;
		}
		if(b == Blocks.stone || b == Blocks.cobblestone) {
			int level = sellafieldLevel(dist, stoneDepth, maxStoneDepth);
			if(level < 0) setBlock(x, y, z, ModBlocks.sellafield_slaked);
			else setBlock(x, y, z, ModBlocks.sellafield, level);
			return true;
		}
		// уже заражённый камень становится горячее (1.12.2)
		if(b == ModBlocks.sellafield_slaked) {
			setBlock(x, y, z, ModBlocks.sellafield, 0);
			return true;
		}
		if(b == ModBlocks.sellafield) {
			if(meta < 5) setBlock(x, y, z, ModBlocks.sellafield, meta + 1);
			return true;
		}
		if(b == Blocks.grass) {
			setBlock(x, y, z, ModBlocks.waste_earth);
			return true;
		}
		if(b == Blocks.gravel) {
			placeFromDist(dist, ModBlocks.waste_gravel, x, y, z);
			return true;
		}
		if(b == Blocks.dirt) {
			if(meta == 1) placeFromDist(dist, ModBlocks.waste_gravel, x, y, z);		// каменистая земля
			else if(meta == 2) setBlock(x, y, z, ModBlocks.waste_mycelium);			// подзол
			else placeFromDist(dist, ModBlocks.waste_dirt, x, y, z);
			return true;
		}
		if(b == Blocks.farmland) {
			placeFromDist(dist, ModBlocks.waste_dirt, x, y, z);
			return true;
		}
		if(b == Blocks.snow_layer) {
			placeFromDist(dist, ModBlocks.waste_snow, x, y, z);
			return true;
		}
		if(b == Blocks.snow) {
			placeFromDist(dist, ModBlocks.waste_snow_block, x, y, z);
			return true;
		}
		if(b == Blocks.ice || b == Blocks.packed_ice) {
			setBlock(x, y, z, ModBlocks.waste_ice);
			return true;
		}
		if(b instanceof BlockBush && !(b instanceof BlockMushroom)) {
			Block below = worldObj.getBlock(x, y - 1, z);
			if(below == Blocks.farmland) {
				placeFromDist(dist, ModBlocks.waste_dirt, x, y - 1, z);
				placeFromDist(dist, ModBlocks.waste_grass_tall, x, y, z);
			} else if(below == Blocks.grass) {
				setBlock(x, y - 1, z, ModBlocks.waste_earth);
				placeFromDist(dist, ModBlocks.waste_grass_tall, x, y, z);
			} else if(below == Blocks.mycelium) {
				setBlock(x, y - 1, z, ModBlocks.waste_mycelium);
				setBlock(x, y, z, ModBlocks.mush);
			} else {
				worldObj.setBlockToAir(x, y, z);
			}
			return true;
		}
		if(b == Blocks.mycelium) {
			setBlock(x, y, z, ModBlocks.waste_mycelium);
			return true;
		}
		if(b == Blocks.sandstone) {
			placeFromDist(dist, ModBlocks.waste_sandstone, x, y, z);
			return true;
		}
		if(b == Blocks.hardened_clay || b == Blocks.stained_hardened_clay) {
			placeFromDist(dist, ModBlocks.waste_terracotta, x, y, z);
			return true;
		}
		if(b == Blocks.sand) {
			if(rand.nextInt(60) == 0) setBlock(x, y, z, meta == 1 ? ModBlocks.waste_trinitite_red : ModBlocks.waste_trinitite);
			else placeFromDist(dist, meta == 1 ? ModBlocks.waste_sand_red : ModBlocks.waste_sand, x, y, z);
			return true;
		}
		if(b == Blocks.clay) {
			setBlock(x, y, z, Blocks.hardened_clay);
			return true;
		}
		if(b == Blocks.mossy_cobblestone) {
			setBlock(x, y, z, Blocks.coal_ore);
			return true;
		}
		if(b == Blocks.coal_ore) {
			if(dist < S5) {
				int ra = rand.nextInt(150);
				if(ra < 7) setBlock(x, y, z, Blocks.diamond_ore);
				else if(ra < 10) setBlock(x, y, z, Blocks.emerald_ore);
			}
			return true;
		}
		if(b instanceof BlockLog) {
			if(dist < S1) setBlock(x, y, z, ModBlocks.waste_log, meta & 12);
			return true;
		}
		if(b.getMaterial() == Material.wood && b != ModBlocks.waste_log && b != ModBlocks.waste_planks) {
			if(dist < S1) setBlock(x, y, z, ModBlocks.waste_planks);
			return true;
		}
		if(b == Blocks.vine) {
			worldObj.setBlockToAir(x, y, z);
			return true;
		}
		return false;
	}

	public void setBlock(int x, int y, int z, Block block) {
		setBlock(x, y, z, block, 0);
	}

	public void setBlock(int x, int y, int z, Block block, int meta) {
		worldObj.setBlock(x, y, z, block, meta, 3); //this was supposed to write the position to a list for a multi block update, but forge already has that built-in. whoops.
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(16, 0);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setScale(tag.getInteger("scale"));
		this.salted = tag.getBoolean("salt");
		chunksToProcess.addAll(readChunksFromIntArray(tag.getIntArray("chunks")));
		outerChunksToProcess.addAll(readChunksFromIntArray(tag.getIntArray("outerChunks")));
	}

	private Collection<Long> readChunksFromIntArray(int[] data) {
		List<Long> coords = new ArrayList<>();
		boolean firstPart = true;
		int x = 0;
		for (int coord : data) {
			if (firstPart) x = coord;
			else coords.add(ChunkCoordIntPair.chunkXZ2Int(x, coord));
			firstPart = !firstPart;
		}
		return coords;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setInteger("scale", getScale());
		tag.setBoolean("salt", this.salted);
		tag.setIntArray("chunks", writeChunksToIntArray(chunksToProcess));
		tag.setIntArray("outerChunks", writeChunksToIntArray(outerChunksToProcess));
	}

	private int[] writeChunksToIntArray(List<Long> coords) {
		int[] data = new int[coords.size() * 2];
		for (int i = 0; i < coords.size(); i++) {
			data[i * 2] = (int) (coords.get(i) & Integer.MAX_VALUE);
			data[i * 2 + 1] = (int) (coords.get(i) >> 32 & Integer.MAX_VALUE);
		}
		return data;
	}

	public void setScale(int i) {
		this.dataWatcher.updateObject(16, i);
	}

	public int getScale() {
		int scale = this.dataWatcher.getWatchableObjectInt(16);
		return scale == 0 ? 1 : scale;
	}
	public void setSalted(boolean salt) {
		this.salted = salt;
	}

	public boolean getSalted() {
		boolean salt = this.salted;
		return salt;
	}
}
