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

import betterwithmods.module.hardcore.world.HCBuoy;
import betterwithmods.util.item.Stack;
import net.minecraft.item.ItemStack;

public class IBHandlerBWM extends IBHandler {
	@Override
	public boolean isFloating(ItemStack stack) {
		return HCBuoy.getBuoyancy(stack) > 0.5F;
		/* Stack s = new Stack(stack);
		if (HCBuoy.buoyancy.containsKey(s)) {
			return HCBuoy.buoyancy.get(s) > 0.5F;
		} else {
			return false;
			// return super.isFloating(stack);
		} */
	}
}
