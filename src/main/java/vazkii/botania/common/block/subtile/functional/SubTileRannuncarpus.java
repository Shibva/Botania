/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [May 1, 2014, 6:08:25 PM (GMT)]
 */
package vazkii.botania.common.block.subtile.functional;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ObjectHolder;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.item.IFloatingFlower;
import vazkii.botania.api.item.IFlowerPlaceable;
import vazkii.botania.api.lexicon.LexiconEntry;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.TileEntityFunctionalFlower;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.lexicon.LexiconData;
import vazkii.botania.common.lib.LibMisc;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubTileRannuncarpus extends TileEntityFunctionalFlower {
	@ObjectHolder(LibMisc.MOD_ID + ":rannuncarpus")
	public static TileEntityType<SubTileRannuncarpus> TYPE;

	private static final int RANGE = 2;
	private static final int RANGE_Y = 3;
	private static final int RANGE_PLACE_MANA = 8;
	private static final int RANGE_PLACE = 6;
	private static final int RANGE_PLACE_Y = 6;

	private static final int RANGE_PLACE_MANA_MINI = 3;
	private static final int RANGE_PLACE_MINI = 2;
	private static final int RANGE_PLACE_Y_MINI = 2;

	public SubTileRannuncarpus(TileEntityType<?> type) {
		super(type);
	}

	public SubTileRannuncarpus() {
		this(TYPE);
	}

	@Override
	public void tickFlower() {
		super.tickFlower();

		if(getWorld().isRemote || redstoneSignal > 0)
			return;

		if(ticksExisted % 10 == 0) {
			List<EntityItem> items = getWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(getPos().add(-RANGE, -RANGE_Y, -RANGE), getPos().add(RANGE + 1, RANGE_Y + 1, RANGE + 1)));
			List<BlockPos> validPositions = getCandidatePositions();
			int slowdown = getSlowdownFactor();

			for(EntityItem item : items) {
				if(item.age < 60 + slowdown || !item.isAlive() || item.getItem().isEmpty())
					continue;

				ItemStack stack = item.getItem();
				Item stackItem = stack.getItem();
				if(stackItem instanceof ItemBlock || stackItem instanceof IFlowerPlaceable) {
					if(!validPositions.isEmpty()) {
						BlockPos coords = validPositions.get(getWorld().rand.nextInt(validPositions.size()));
						BlockItemUseContext ctx = new RannuncarpusPlaceContext(getWorld(), stack, coords, EnumFacing.UP, 0, 0, 0);

						boolean success = false;
						if(stackItem instanceof IFlowerPlaceable) {
							success = ((IFlowerPlaceable) stackItem).tryPlace(this, ctx);
						} if(stackItem instanceof ItemBlock) {
							success = ((ItemBlock) stackItem).tryPlace(ctx) == EnumActionResult.SUCCESS;
						}

						if(success) {
							if(ConfigHandler.COMMON.blockBreakParticles.get()) {
								IBlockState state = getWorld().getBlockState(ctx.getPos());
								getWorld().playEvent(2001, coords, Block.getStateId(state));
							}
							validPositions.remove(coords);
							if(mana > 1)
								mana--;
							return;
						}
					}
				}
			}
		}
	}

	public IBlockState getUnderlyingBlock() {
		return getWorld().getBlockState(getPos().down(isFloating() ? 1 : 2));
	}

	private List<BlockPos> getCandidatePositions() {
		int rangePlace = getRange();
		int rangePlaceY = getRangeY();
		BlockPos pos = getPos();
		IBlockState filter = getUnderlyingBlock();
		List<BlockPos> ret = new ArrayList<>();

		for (BlockPos pos_ : BlockPos.getAllInBox(pos.add(-rangePlace, -rangePlaceY, -rangePlace), pos.add(rangePlace, rangePlaceY, rangePlace))) {
			if (filter == getWorld().getBlockState(pos_))
				ret.add(pos_);
		}
		return ret;
	}

	@Override
	public boolean acceptsRedstone() {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void renderHUD(Minecraft mc) {
		super.renderHUD(mc);

		IBlockState filter = getUnderlyingBlock();
		ItemStack recieverStack = new ItemStack(filter.getBlock());
		int color = getColor();

		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		if(!recieverStack.isEmpty()) {
			ITextComponent stackName = recieverStack.getDisplayName();
			int width = 16 + mc.fontRenderer.getStringWidth(stackName.getString()) / 2;
			int x = mc.mainWindow.getScaledWidth() / 2 - width;
			int y = mc.mainWindow.getScaledHeight() / 2 + 30;

			mc.fontRenderer.drawStringWithShadow(stackName.getFormattedText(), x + 20, y + 5, color);
			RenderHelper.enableGUIStandardItemLighting();
			mc.getItemRenderer().renderItemAndEffectIntoGUI(recieverStack, x, y);
			RenderHelper.disableStandardItemLighting();
		}

		GlStateManager.disableLighting();
		GlStateManager.disableBlend();
	}

	@Override
	public RadiusDescriptor getRadius() {
		return new RadiusDescriptor.Square(toBlockPos(), getRange());
	}

	public int getRange() {
		return mana > 0 ? RANGE_PLACE_MANA : RANGE_PLACE;
	}

	public int getRangeY() {
		return RANGE_PLACE_Y;
	}

	@Override
	public int getMaxMana() {
		return 20;
	}

	@Override
	public int getColor() {
		return 0xFFB27F;
	}

	@Override
	public LexiconEntry getEntry() {
		return LexiconData.rannuncarpus;
	}

	public static class Mini extends SubTileRannuncarpus {
		@ObjectHolder(LibMisc.MOD_ID + ":rannuncarpus_chibi")
		public static TileEntityType<SubTileRannuncarpus.Mini> TYPE;

		public Mini() {
			super(TYPE);
		}

		@Override public int getRange() { return mana > 0 ? RANGE_PLACE_MANA_MINI : RANGE_PLACE_MINI; }
		@Override public int getRangeY() { return RANGE_PLACE_Y_MINI; }
	}

	// BlockItemUseContext uses a nullable player field without checking it -.-
	private static class RannuncarpusPlaceContext extends BlockItemUseContext {
		private final EnumFacing[] lookDirs;

		public RannuncarpusPlaceContext(World world, ItemStack stack, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
			super(world, null, stack, pos, side, hitX, hitY, hitZ);
			List<EnumFacing> tmp = Arrays.asList(EnumFacing.values());
			Collections.shuffle(tmp);
			lookDirs = tmp.toArray(new EnumFacing[6]);
		}

		@Nonnull
		@Override
		public EnumFacing getNearestLookingDirection() {
			return getNearestLookingDirections()[0];
		}

		@Nonnull
		@Override
		public EnumFacing[] getNearestLookingDirections() {
		    return lookDirs;
		}
	}

}
