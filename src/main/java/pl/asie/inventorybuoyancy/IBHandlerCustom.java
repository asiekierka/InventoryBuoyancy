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
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class IBHandlerCustom extends IBHandler {
	private final Set<Item> itemSet = Collections.newSetFromMap(new IdentityHashMap<>());
	private final TIntSet oreSet = new TIntHashSet();

	public IBHandlerCustom(String[] data) {
		for (String s : data) {
			if (s.indexOf(':') >= 0) {
				ResourceLocation loc = new ResourceLocation(s);
				if (ForgeRegistries.ITEMS.containsKey(loc)) {
					itemSet.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(s)));
				}
			} else {
				oreSet.add(OreDictionary.getOreID(s));
			}
		}
	}

	@Override
	public boolean isFloating(ItemStack stack) {
		if (itemSet.contains(stack.getItem())) {
			return true;
		} else {
			for (int i : OreDictionary.getOreIDs(stack)) {
				if (oreSet.contains(i)) {
					return true;
				}
			}

			return false;
		}
	}
}
