package de.moritzschmale.Showcase;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;


public class ShowcasePlayerListener extends PlayerListener {
	@Override
	public void onPlayerInteract(PlayerInteractEvent event){
		if(event.hasBlock()){
			ShowcaseItem showItem = ShowcaseMain.instance.getItemByBlock(event.getClickedBlock());
			ShowcasePlayer player = ShowcasePlayer.getPlayer(event.getPlayer());
			if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
				if(!event.getPlayer().isSneaking()){
					return;
				}
				if(event.hasBlock()&&showItem == null){
					if(event.getItem()==null)
					{
						player.sendMessage(ChatColor.RED+"You have got to hold something in your hand!");
						event.setCancelled(true);
						return;
					}
					if(event.getClickedBlock().getType().equals(Material.GLASS)){
						event.setCancelled(true);
						if(!isSafePlace(event.getClickedBlock())){
							player.sendMessage(ChatColor.RED+"This is not a safe place for your item. It will fall down.");
							return;
						}
						if(player.hasPermission("showcase.basic", false)&&!player.hasPermission("showcase.infinite", true)&&!player.hasPermission("showcase.finite", false)){
							Location loc = event.getClickedBlock().getLocation();
							Material mat = event.getItem().getType();
							short data = event.getItem().getDurability();
							addShowcase(loc, mat, data, player.getPlayer(), ShowcaseType.BASIC, 1, 0);
							player.sendMessage(ChatColor.GREEN+"Item "+mat+" showcased.");
							player.resetDialog();
						} else {
							printTypeMenu(event.getPlayer());
							player.setDialogState(1);
							player.setRequestedItem(event.getItem().clone());
							player.setRequestedBlock(event.getClickedBlock());
						}
					}
				} else if(showItem!=null){
					if(showItem.getPlayer().equals(event.getPlayer().getName())){
						showItem.giveItemsBack();
						showItem.remove();
						ShowcaseMain.instance.showcasedItems.remove(showItem);
						event.getPlayer().sendMessage(ChatColor.RED+"Removed Showcased item.");
					} else {
						event.getPlayer().sendMessage(ChatColor.RED+"This is "+showItem.getPlayer()+"'s Showcase!");
					}
					event.setCancelled(true);
				}
			} else if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)){
				if(showItem!=null&&showItem.getType().toString().contains("SHOP")){
					if(!player.hasReadPrice()||!player.standsOnReadPosition()){
						String print = ChatColor.YELLOW+"An item costs "+ChatColor.WHITE+showItem.getPricePerItem()+"\n";
						player.setHasReadPrice(true);
						player.setReadPriceLocation(player.getPlayer().getLocation());
						if(showItem.getType().equals(ShowcaseType.FINITE_SHOP)){
							print+=ChatColor.YELLOW+"There are "+ChatColor.WHITE+showItem.getItemAmount();
							print+=ChatColor.YELLOW+" items in stock.";
						} else if(showItem.getType().equals(ShowcaseType.INFINITE_SHOP)){
							print+=ChatColor.YELLOW+"This shop has unlimited items.";
						}
						player.sendMessage(print);
					} else {
						if(player.standsOnReadPosition()){
							//User wants to buy
							ItemStack stack = showItem.getItem().getItemStack().clone();
							stack.setAmount(1);
							if(player.withdraw(showItem.getPricePerItem())){
								switch(showItem.getType()){
								case INFINITE_SHOP:
									if(player.hasPermission("showcase.buy.infinite", false)){
										player.getPlayer().getInventory().addItem(stack);
										player.sendMessage("You bought an item");
									} else {
										player.sendMessage("You can't buy from infinite showcases!");
									}
									break;
								case FINITE_SHOP:
									if(player.hasPermission("showcase.buy.finite", false)){
										player.getPlayer().getInventory().addItem(stack);
										showItem.setItemAmount(showItem.getItemAmount()-1);
										ShowcasePlayer owner = ShowcasePlayer.getPlayer(showItem.getPlayer());
										owner.giveMoney(showItem.getPricePerItem());
										player.sendMessage("You bought an item. Still in stock: "+showItem.getItemAmount());
									} else {
										player.sendMessage("You can't buy from finite showcases!");
									}
									break;
								}
							} else {
								player.sendMessage("You have not enough money.");
							}
						} else {
							player.setHasReadPrice(false);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event){
		Item item = event.getItem();
		for(ShowcaseItem compare:ShowcaseMain.instance.showcasedItems){
			if(compare.getItem().equals(item)){
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@Override
	public void onPlayerChat(PlayerChatEvent event){
		ShowcasePlayer player = ShowcasePlayer.getPlayer(event.getPlayer());
		if(player.getDialogState()>=1){
			event.setCancelled(true);
			player.sendMessage(event.getMessage());
			if(player.getDialogState()==1){
				ShowcaseType type = ShowcaseType.NONE;
				String message = event.getMessage().toLowerCase();
				if(message.equals("basic")){
					type = ShowcaseType.BASIC;
				} else if(message.equals("infinite")){
					type = ShowcaseType.INFINITE_SHOP;
				} else if(message.equals("finite")){
					type = ShowcaseType.FINITE_SHOP;
				}
				if(type.equals(ShowcaseType.NONE)){
					player.sendMessage("Invalid answer. Aborting.");
					player.resetDialog();
					return;
				}
				if(message.equals("basic")&&!player.hasPermission("showcase.basic", false)
						|| message.equals("infinite")&&!player.hasPermission("showcase.infinite", true)
						|| message.equals("finite")&&!player.hasPermission("showcase.finite", false)){
					player.sendMessage("Insufficient permissions. Aborting.");
					player.resetDialog();
					return;
				}
				player.setRequestedType(type);
				player.setDialogState(2);
				if(type.equals(ShowcaseType.BASIC)){
					Location loc = player.getRequestedBlock().getLocation();
					Material mat = player.getRequestedItem().getType();
					short data = player.getRequestedItem().getDurability();
					addShowcase(loc, mat, data, player.getPlayer(), type, 1, 0);
					player.sendMessage(ChatColor.GREEN+"Item "+mat+" showcased.");
					player.resetDialog();
				} else {
					printPriceMenu(event.getPlayer());
				}
			} else if(player.getDialogState()==2){
				double price = 0;
				try{
					price = Double.valueOf(event.getMessage());
				} catch(Exception e){
					price = 0;
				}
				player.setRequestedPrice(price);
				player.setDialogState(3);
				if(player.getRequestedType().equals(ShowcaseType.INFINITE_SHOP)){
					Location loc = player.getRequestedBlock().getLocation();
					Material mat = player.getRequestedItem().getType();
					short data = player.getRequestedItem().getDurability();
					addShowcase(loc, mat, data, event.getPlayer(), ShowcaseType.INFINITE_SHOP, -1, price);
					player.sendMessage(ChatColor.GREEN+"Setup of Infinite Showcase successful.");
					player.resetDialog();
				} else {
					printAmountMenu(event.getPlayer());
				}
			} else if(player.getDialogState()==3){
				int amount = 0;
				try{
					amount = Integer.valueOf(event.getMessage());
				} catch(Exception e){
					amount = 0;
				}
				ItemStack stack = player.getRequestedItem();
				if(amount>player.getAmountOfType(stack.getType(), stack.getDurability())){
					amount = player.getAmountOfType(stack.getType(), stack.getDurability());
				}
				if(amount<=0){
					amount = 1;
				}
				Location loc = player.getRequestedBlock().getLocation();
				Material mat = player.getRequestedItem().getType();
				short data = player.getRequestedItem().getDurability();
				addShowcase(loc, mat, data, player.getPlayer(), player.getRequestedType(), amount, player.getRequestedPrice());
				player.sendMessage(ChatColor.GREEN+"Finite Shop Showcase setup successful.");
				player.remove(mat, data, amount);
			}
		}
	}
	
	public void addShowcase(Location loc, Material material, short data, Player owner, ShowcaseType type, int amount, double price){
		ItemStack stack = new ItemStack(material,1,data);
		Item item = loc.getWorld().dropItemNaturally(loc, stack);
		ShowcaseItem shit = new ShowcaseItem(item, loc, owner.getName()); //Lol, it's ShIt for short :D
		ShowcaseMain.instance.showcasedItems.add(shit);
		shit.setItemAmount(amount);
		shit.setPricePerItem(price);
		shit.setType(type);
	}
	
	public void printTypeMenu(Player p){
		ShowcasePlayer player = ShowcasePlayer.getPlayer(p);
		String print = ChatColor.GOLD+"======[ "+ChatColor.YELLOW+"Showcase Type Selection"+ChatColor.GOLD+" ]======\n";
		if(player.hasPermission("showcase.basic", false)){
			print+=ChatColor.YELLOW+"(basic)"+ChatColor.WHITE+" Basic Showcase. Just displays an item\n";
		}
		if(player.hasPermission("showcase.finite", false)){
			print+=ChatColor.YELLOW+"(finite)"+ChatColor.WHITE+" Fininite Shop Showcase. You can sell your items there\n";
		}
		if(player.hasPermission("showcase.infinite", true)){
			print+=ChatColor.YELLOW+"(infinite)"+ChatColor.WHITE+" Infinite Shop Showcase. Others can buy unlimited items\n";
		}
		print += ChatColor.GOLD+"======[ "+ChatColor.YELLOW+"Enter Selection"+ChatColor.GOLD+" ]======";
		player.sendMessage(print);
	}
	
	public void printPriceMenu(Player p){
		ShowcasePlayer player = ShowcasePlayer.getPlayer(p);
		ShowcaseType type = player.getRequestedType();
		String print = ChatColor.YELLOW+"You want a "+type.toString().toLowerCase()+" Showcase.\n";
		print+=ChatColor.YELLOW+"Please enter the price per item:";
		player.sendMessage(print);
	}
	
	public void printAmountMenu(Player p){
		ShowcasePlayer player = ShowcasePlayer.getPlayer(p);
		String print = ChatColor.YELLOW+"You want "+player.getRequestedPrice()+ChatColor.YELLOW+" dollars per item.\n";
		print+=ChatColor.YELLOW+"Please enter the desired amount of items:\n";
		print+=ChatColor.YELLOW+"(You have got "+ChatColor.WHITE;
		ItemStack stack = player.getRequestedItem();
		print+=player.getAmountOfType(stack.getType(), stack.getDurability());
		print+=ChatColor.YELLOW+" of "+ChatColor.WHITE+stack.getType()+ChatColor.YELLOW+")\n";
		player.sendMessage(print);
	}
	
	public boolean isSafePlace(Block glass){
		Block below = glass.getFace(BlockFace.DOWN);
		Material nonsafe[] = {
			Material.GLASS,
			Material.AIR,
			Material.RED_MUSHROOM,
			Material.RED_ROSE,
			Material.REDSTONE_WIRE,
			Material.REDSTONE_TORCH_ON,
			Material.REDSTONE_TORCH_OFF,
			Material.YELLOW_FLOWER,
			Material.TORCH,
			Material.DIODE_BLOCK_ON,
			Material.DIODE_BLOCK_OFF,
			Material.WOOD_DOOR,
			Material.IRON_DOOR,
			Material.WOOD_PLATE,
			Material.STONE_PLATE,
			Material.STONE_BUTTON,
			Material.DOUBLE_STEP,
			Material.WATER,
			Material.LAVA,
			Material.STATIONARY_LAVA,
			Material.STATIONARY_WATER,
			Material.WOOD_STAIRS,
			Material.COBBLESTONE_STAIRS,
			Material.STEP,
			Material.SIGN_POST,
			Material.WALL_SIGN,
			Material.FENCE,
			Material.CACTUS,
			Material.LEVER,
			Material.LADDER,
			Material.RAILS,
			Material.POWERED_RAIL,
			Material.DETECTOR_RAIL,
			Material.SAPLING,
			Material.BED_BLOCK,
			Material.WEB,
			Material.BROWN_MUSHROOM,
			Material.FIRE,
			Material.MOB_SPAWNER,
			Material.SEEDS,
			Material.SOIL,
			Material.SNOW,
			Material.SUGAR_CANE_BLOCK,
			Material.PORTAL,
			Material.CAKE_BLOCK,
		};
		for(Material t:nonsafe){
			if(t.equals(below.getType())){
				return false;
			}
		}
		return true;
	}
}
