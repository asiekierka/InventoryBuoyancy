/*
 * Copyright (C) 2017 Adrian Siekierka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package pl.asie.inventorybuoyancy;

import com.google.common.collect.Sets;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;

@Mod(
		modid = "inventorybuoyancy",
		name = "pl.asie.inventorybuoyancy.InventoryBuoyancy",
		version = "${version}",
		dependencies = "after:betterwithmods"
)
public class InventoryBuoyancy {
	private final Random random = new Random();
	private IBHandler handler;

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
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

		MinecraftForge.EVENT_BUS.register(this);
	}

	private final int[] invLayout = {
			9, 10, 11, 12, 13, 14, 15, 16, 17,
			18, 19, 20, 21, 22, 23, 24, 25, 26,
			27, 28, 29, 30, 31, 32, 33, 34, 35,
			0, 1, 2, 3, 4, 5, 6, 7, 8
	};

	private void move(int from, int to, EntityPlayer player, ItemStack stack) {
		ItemStack cstack = stack.copy();
		stack.setCount(0);
		player.inventory.setInventorySlotContents(from, ItemStack.EMPTY);
		player.inventory.setInventorySlotContents(to, cstack);
		((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, from, player.inventory.getStackInSlot(from)));
		((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-2, to, player.inventory.getStackInSlot(to)));
		player.inventory.markDirty();
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.player.isCreative()) {
			// FIXME: Creative mode + this can cause dupe bugs/state corruption???
			return;
		}

		if (event.phase == TickEvent.Phase.END && !event.player.getEntityWorld().isRemote &&
				event.player.isInWater() && (event.player.getEntityWorld().getTotalWorldTime() % 5) == 0) {
			int changes = 0;

			for (int i = 9; i < invLayout.length; i++) {
				ItemStack stack = event.player.inventory.getStackInSlot(invLayout[i]);
				if (stack.isEmpty()) {
					continue;
				}

				if (handler.isFloating(stack)) {
					ItemStack stackAbove = event.player.inventory.getStackInSlot(invLayout[i - 9]);
					if (stackAbove.isEmpty()) {
						move(invLayout[i], invLayout[i - 9], event.player, stack);
						changes++;
					} else {
						boolean moved = false;
						int offset = random.nextInt(2);
						for (int ii = offset; ii < offset + 2 && !moved; ii++) {
							switch (ii % 2) {
								case 0:
									if ((i % 9) > 0) {
										stackAbove = event.player.inventory.getStackInSlot(invLayout[i - 1 - 9]);
										if (stackAbove.isEmpty()) {
											move(invLayout[i], invLayout[i - 1 - 9], event.player, stack);
											moved = true;
										}
									}
									break;
								case 1:
									if ((i % 9) < 8) {
										stackAbove = event.player.inventory.getStackInSlot(invLayout[i + 1 - 9]);
										if (stackAbove.isEmpty()) {
											move(invLayout[i], invLayout[i + 1 - 9], event.player, stack);
											moved = true;
										}
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

			if (changes > 0) {
				if (event.player.openContainer instanceof ContainerPlayer) {
					event.player.openContainer.detectAndSendChanges();
				}
			}
		}
	}
}
