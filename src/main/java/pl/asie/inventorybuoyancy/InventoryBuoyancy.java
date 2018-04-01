/*
 * Copyright (C) 2017, 2018 Adrian Siekierka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package pl.asie.inventorybuoyancy;

import com.google.common.collect.Sets;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

@Mod(
		modid = "inventorybuoyancy",
		name = "WorldBuoyancy",
		version = "${version}",
		acceptedMinecraftVersions = "[1.12,1.13)",
		dependencies = "after:betterwithmods"
)
public class InventoryBuoyancy {
	private final Random random = new Random();
	private int itemMovementSpeed;
	private boolean easyMode, hardMode;
	private boolean liftHopesUp, liftRationaleUp;
	private Configuration config;
	private IBHandler handler;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());

		easyMode = config.getBoolean("omitItemInHand", "general", false, "Exempts the item in the player's hand.");
		hardMode = config.getBoolean("floatBothWays", "general", false, "Makes non-floating items move down as well.");
		itemMovementSpeed = config.getInt("itemMovementSpeed", "general", 5, 1, Integer.MAX_VALUE, "The item movement speed, in ticks per slot.");
		liftHopesUp = config.getBoolean("cannotDisplaceLiquid", "features", true, "Makes it impossible to easily displace liquid.");
		liftRationaleUp = config.getBoolean("inventoryBuoyancy", "features", true, "Inventory is buoyant. Hah.");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		String[] customItems = config.getStringList("customBuoyancyList", "general", new String[0], "Custom list of items to float. If empty, default handler is used.\nUse registry names (with :) or no : for OreDictionary IDs.");

		if (config.hasChanged()) {
			config.save();
		}

		if (customItems.length > 0) {
			handler = new IBHandlerCustom(customItems);
		} else {
			try {
				if (Loader.isModLoaded("betterwithmods")) {
					handler = (IBHandler) Class.forName("pl.asie.inventorybuoyancy.IBHandlerBWM").newInstance();
				} else {
					handler = new IBHandler();
				}
			} catch (Exception e) {
				e.printStackTrace();
				handler = new IBHandler();
			}
		}

		MinecraftForge.EVENT_BUS.register(this);
	}

	private final int[] invLayout = {
			9, 10, 11, 12, 13, 14, 15, 16, 17,
			18, 19, 20, 21, 22, 23, 24, 25, 26,
			27, 28, 29, 30, 31, 32, 33, 34, 35,
			0, 1, 2, 3, 4, 5, 6, 7, 8
	};

	private final int[] invLayoutReverse = {
			0, 1, 2, 3, 4, 5, 6, 7, 8,
			27, 28, 29, 30, 31, 32, 33, 34, 35,
			18, 19, 20, 21, 22, 23, 24, 25, 26,
			9, 10, 11, 12, 13, 14, 15, 16, 17
	};

	private boolean move(int from, int to, EntityPlayer player, ItemStack stack) {
		ItemStack toStack = player.inventory.getStackInSlot(to);
		if (toStack.isEmpty()) {
			ItemStack cstack = stack.copy();
			stack.setCount(0);
			player.inventory.setInventorySlotContents(from, ItemStack.EMPTY);
			player.inventory.setInventorySlotContents(to, cstack);

			((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, from, player.inventory.getStackInSlot(from)));
			((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, to, player.inventory.getStackInSlot(to)));
			player.inventory.markDirty();

			return true;
		} else if (ItemUtils.canMerge(stack, toStack)) {
			int mergeSize = Math.min(stack.getCount(), toStack.getMaxStackSize() - toStack.getCount());
			if (mergeSize <= 0) {
				return false;
			}

			ItemStack cstack = stack.copy();
			cstack.shrink(mergeSize);
			toStack.grow(mergeSize);

			if (cstack.isEmpty()) {
				cstack = ItemStack.EMPTY;
			}

			player.inventory.setInventorySlotContents(from, cstack);
			player.inventory.setInventorySlotContents(to, toStack);

			((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, from, player.inventory.getStackInSlot(from)));
			((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, to, player.inventory.getStackInSlot(to)));
			player.inventory.markDirty();

			return cstack == ItemStack.EMPTY;
		} else {
			return false;
		}
	}

	private int moveItems(EntityPlayer player, int[] layout, Predicate<ItemStack> predicate) {
		int changes = 0;

		for (int i = 9; i < layout.length; i++) {
			if (easyMode && layout[i] == player.inventory.currentItem) {
				continue;
			}

			ItemStack stack = player.inventory.getStackInSlot(layout[i]);
			if (stack.isEmpty()) {
				continue;
			}

			if (predicate.test(stack)) {
				boolean moved = move(layout[i], layout[i - 9], player, stack);
				if (!moved) {
					int offset = random.nextInt(2);
					for (int ii = offset; ii < offset + 2 && !moved; ii++) {
						switch (ii % 2) {
							case 0:
								if ((i % 9) > 0) {
									moved = move(layout[i], layout[i - 1 - 9], player, stack);
								}
								break;
							case 1:
								if ((i % 9) < 8) {
									moved = move(layout[i], layout[i + 1 - 9], player, stack);
								}
								break;
						}
					}

					if (moved) {
						changes++;
					}
				}
			}
		}

		return changes;
	}

	public static boolean isLiquid(IBlockState state) {
		return state.getBlock() instanceof BlockLiquid || state.getBlock() instanceof IFluidBlock;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onWorldLoad(WorldEvent.Load event) {
		if (liftHopesUp) {
			event.getWorld().addEventListener(HopeLifter.INSTANCE);
		}
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (!liftRationaleUp || event.player.isCreative()) {
			// FIXME: Creative mode + this can cause dupe bugs/state corruption???
			return;
		}

		// TODO: Implement gaseous state
		if (event.phase == TickEvent.Phase.END && !event.player.getEntityWorld().isRemote &&
				event.player.isInWater() && (event.player.getEntityWorld().getTotalWorldTime() % itemMovementSpeed) == 0) {
			IBlockState state = event.player.getEntityWorld().getBlockState(event.player.getPosition());
			Fluid f = FluidRegistry.lookupFluidForBlock(state.getBlock());

			int changes = 0;

			if (f != null && f.isGaseous()) {
				changes += moveItems(event.player, invLayoutReverse, handler::isFloating);
				if (hardMode) {
					changes += moveItems(event.player, invLayout, (stack) -> !handler.isFloating(stack));
				}
			} else {
				changes += moveItems(event.player, invLayout, handler::isFloating);
				if (hardMode) {
					changes += moveItems(event.player, invLayoutReverse, (stack) -> !handler.isFloating(stack));
				}
			}
		}
	}
}
