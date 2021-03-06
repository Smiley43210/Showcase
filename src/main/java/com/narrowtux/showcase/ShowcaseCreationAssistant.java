/*
 * Copyright (C) 2011 Moritz Schmale <narrow.m@gmail.com>
 *
 * Showcase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl.html>.
 */

package com.narrowtux.showcase;

import info.somethingodd.bukkit.OddItem.OddItem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.narrowtux.narrowtuxlib.NarrowtuxLib;
import com.narrowtux.narrowtuxlib.assistant.Assistant;
import com.narrowtux.narrowtuxlib.assistant.AssistantAction;
import com.narrowtux.narrowtuxlib.assistant.AssistantPage;
import com.narrowtux.narrowtuxlib.assistant.Icon;
import com.nijikokun.register.narrowtuxlib.payment.Method;

public class ShowcaseCreationAssistant extends Assistant {
	public String type = "";
	public Material material;
	public short data;
	public Location loc;

	public ShowcasePlayer player;
	public Configuration config = Showcase.instance.config;
	public Method method = NarrowtuxLib.getMethod();

	public ShowcaseCreationAssistant(Player p, ItemStack item, Location loc) {
		super(p);

		player = ShowcasePlayer.getPlayer(getPlayer());
		setTitle(Showcase.tr("assistant.creation.title"));
		ShowcaseTypeSelectionPage typeSelectionPage = new ShowcaseTypeSelectionPage(player, this);
		typeSelectionPage.assistant = this;
		if (item == null) {
			addPage(new AssistantPage(this) {
				{
					setTitle(Showcase.tr("creation.item.title"));
					setText(Showcase.tr("creation.item.text"));
				}

				@Override
				public AssistantAction onPageInput(String text) {
					ItemStack result = null;
					OddItem odd = (OddItem) Bukkit.getServer().getPluginManager().getPlugin("OddItem");
					try {
						result = odd.getItemStack(text);
					} catch (IllegalArgumentException e) {
						ShowcaseCreationAssistant.this.sendMessage(Icon.WARNING, "Showcase", Showcase.tr("creation.item.notfound", e.getMessage()));
						return AssistantAction.SILENT_REPEAT;
					}
					material = result.getType();
					data = result.getDurability();
					return AssistantAction.CONTINUE;
				}
			});
		} else {
			material = item.getType();
			data = item.getDurability();
		}
		addPage(typeSelectionPage);
		this.loc = loc;
	}

	@Override
	public void onAssistantCancel() {
		sendMessage(Icon.WARNING, "Showcase", Showcase.tr("assistant.creation.cancel"));
	}

	@Override
	public void onAssistantFinish() {
		ShowcaseProvider provider = Showcase.instance.providers.get(type);
		ShowcaseExtra extra = provider.createShowcase(this);
		if (extra != null) {
			sendMessage(Icon.INFORMATION, "Showcase", Showcase.tr("assistant.creation.finish"));
			ShowcaseItem item = new ShowcaseItem(loc, material, data, getPlayer().getName(), type);
			Showcase.instance.addShowcase(item);
			item.setExtra(extra);
			if (method != null) {
				method.getAccount(getPlayer().getName()).subtract(provider.getPriceForCreation(player));
			}
		} else {
			sendMessage(Icon.WARNING, "Showcase", Showcase.tr("assistant.creation.cancel"));
		}
	}

	@Override
	public boolean useGUI() {
		return false;
	}
}
