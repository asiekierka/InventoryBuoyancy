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

import net.minecraft.item.ItemStack;

public final class ItemUtils {
	private ItemUtils() {

	}

	public static boolean canMerge(ItemStack source, ItemStack target) {
		return equals(source, target, false, true, true);
	}

	public static boolean equals(ItemStack source, ItemStack target, boolean matchStackSize, boolean matchDamage, boolean matchNBT) {
		return equals(source, target, matchStackSize, matchDamage, matchNBT, matchNBT);
	}

	public static boolean equals(ItemStack source, ItemStack target, boolean matchStackSize, boolean matchDamage, boolean matchNBT, boolean matchCaps) {
		if (source == target) {
			return true;
		} else if (source.isEmpty()) {
			return target.isEmpty();
		} else {
			if (source.getItem() != target.getItem()) {
				return false;
			}

			if (matchStackSize && source.getCount() != target.getCount()) {
				return false;
			}

			if (matchDamage && source.getItemDamage() != target.getItemDamage()) {
				return false;
			}

			if (matchNBT) {
				if (source.hasTagCompound() != target.hasTagCompound()) {
					return false;
				} else if (source.hasTagCompound() && !source.getTagCompound().equals(target.getTagCompound())) {
					return false;
				}
			}

			if (matchCaps) {
				if (!source.areCapsCompatible(target)) {
					return false;
				}
			}

			return true;
		}
	}
}
