package powercraft.transport.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.tools.Diagnostic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import powercraft.api.PC_Direction;
import powercraft.api.PC_Utils;
import powercraft.api.block.PC_TileEntityScriptable;
import powercraft.api.gres.PC_Gres;
import powercraft.api.gres.PC_GresBaseWithInventory;
import powercraft.api.gres.PC_IGresGui;
import powercraft.api.gres.PC_IGresGuiOpenHandler;
import powercraft.api.network.PC_PacketHandler;
import powercraft.api.script.PC_FakeDiagnostic;
import powercraft.transport.block.PCtr_PacketSetEntitySpeed;
import powercraft.transport.gui.PCtr_GuiBeltScriptable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SuppressWarnings("boxing")
public class PCtr_TileEntityBeltScriptable extends PC_TileEntityScriptable implements PC_IGresGuiOpenHandler {
	
	private static final HashMap<String, Integer> replacements = new HashMap<String, Integer>();
	private static final String[] entryVectors = new String[]{"itemOver", "redstoneToggeled"};
	
	public static final int EXT_OUT_FRONT_COUNT = 0;
	public static final int EXT_OUT_RIGHT_COUNT = 1;
	public static final int EXT_OUT_BACK_COUNT = 2;
	public static final int EXT_OUT_LEFT_COUNT = 3;
	public static final int EXT_IN_OUT_DIR = 4;
	public static final int EXT_IN_TYPE = 5;
	public static final int EXT_IN_ID = 6;
	public static final int EXT_IN_ITEMDAMAGE = 7;
	public static final int EXT_IN_ITEMSTACKSIZE = 8;
	//Entity Type
	public static final int TYPE_ITEMSTACK = 0;
	
	public static final int DIR_NORTH = 0;
	public static final int DIR_EAST = 1;
	public static final int DIR_SOUTH = 2;
	public static final int DIR_WEST = 3;
	
	private static final PC_Direction[] DIR_TO_PCDIR = {PC_Direction.NORTH, PC_Direction.EAST, PC_Direction.SOUTH, PC_Direction.WEST};
	
	static{
		replacements.put("front", EXT_OUT_FRONT_COUNT);
		replacements.put("right", EXT_OUT_RIGHT_COUNT);
		replacements.put("back", EXT_OUT_BACK_COUNT);
		replacements.put("left", EXT_OUT_LEFT_COUNT);
		replacements.put("dir", EXT_IN_OUT_DIR);
		replacements.put("type", EXT_IN_TYPE);
		replacements.put("id", EXT_IN_ID);
		replacements.put("itemdamage", EXT_IN_ITEMDAMAGE);
		replacements.put("itemstacksize", EXT_IN_ITEMSTACKSIZE);
		
		replacements.put("item", TYPE_ITEMSTACK);
		
		replacements.put("north", DIR_NORTH);
		replacements.put("east", DIR_EAST);
		replacements.put("south", DIR_SOUTH);
		replacements.put("west", DIR_WEST);
		replacements.put("test.test.a", DIR_WEST);
		replacements.put("test.b", DIR_WEST);
	}
	
	public PCtr_TileEntityBeltScriptable(){
		super(16);
		setSource(";A MiniScript powered Belt");
	}
	
	private void moveEntity(Entity entity){
		NBTTagCompound compound = PC_Utils.getNBTTagOf(entity);
		PC_Direction dir = DIR_TO_PCDIR[(compound.getInteger("dir")%4+4)%4];
		entity.motionX = dir.offsetX!=0?dir.offsetX*0.2:((Math.floor(entity.posX)+0.5)-entity.posX);
		entity.motionZ = dir.offsetZ!=0?dir.offsetZ*0.2:((Math.floor(entity.posZ)+0.5)-entity.posZ);
		if(PC_Utils.isServer()){
			PC_PacketHandler.sendToAllAround(new PCtr_PacketSetEntitySpeed(compound, entity.getEntityId()), this.worldObj.getWorldInfo().getVanillaDimension(), this.xCoord, this.yCoord, this.zCoord, 16);
		}
	}
	
	@Override
	public void onEntityCollidedWithBlock(Entity entity) {
		if(!(Math.floor(entity.posX)==this.xCoord && Math.floor(entity.posY)==this.yCoord && Math.floor(entity.posZ)==this.zCoord)){
			return;
		}
		if(PC_Utils.isClient()){
			moveEntity(entity);
			return;
		}
		NBTTagCompound compound = PC_Utils.getNBTTagOf(entity);
		int x = compound.getInteger("lastX");
		int y = compound.getInteger("lastY");
		int z = compound.getInteger("lastZ");
		if(x==this.xCoord && y==this.yCoord && z==this.zCoord){
			moveEntity(entity);
			return;
		}
		compound.setInteger("lastX", this.xCoord);
		compound.setInteger("lastY", this.yCoord);
		compound.setInteger("lastZ", this.zCoord);
		int[] ext = getExt();
		ext[EXT_OUT_FRONT_COUNT] = 0;
		ext[EXT_OUT_RIGHT_COUNT] = 0;
		ext[EXT_OUT_BACK_COUNT] = 0;
		ext[EXT_OUT_LEFT_COUNT] = 0;
		switch(PC_Utils.getEntityMovement2D(entity)){
		case EAST:
			ext[EXT_IN_OUT_DIR] = DIR_EAST;
			break;
		case NORTH:
			ext[EXT_IN_OUT_DIR] = DIR_NORTH;
			break;
		case SOUTH:
			ext[EXT_IN_OUT_DIR] = DIR_SOUTH;
			break;
		case WEST:
			ext[EXT_IN_OUT_DIR] = DIR_WEST;
			break;
		default:
			return;
		}
		if(entity instanceof EntityItem){
			ItemStack is = ((EntityItem) entity).getEntityItem();
			ext[EXT_IN_TYPE] = TYPE_ITEMSTACK;
			ext[EXT_IN_ID] = Item.getIdFromItem(is.getItem());
			ext[EXT_IN_ITEMDAMAGE] = is.getItemDamage();
			ext[EXT_IN_ITEMSTACKSIZE] = is.stackSize;
		}else{
			return;
		}
		invoke(0); // entry: itemOver
		int direction = ext[EXT_IN_OUT_DIR];
		int sum = ext[EXT_OUT_FRONT_COUNT]+ext[EXT_OUT_RIGHT_COUNT]+ext[EXT_OUT_BACK_COUNT]+ext[EXT_OUT_LEFT_COUNT];
		if(sum!=0){
			if(sum==ext[EXT_OUT_RIGHT_COUNT]){
				direction += 1;
			}else if(sum==ext[EXT_OUT_BACK_COUNT]){
				direction += 2;
			}else if(sum==ext[EXT_OUT_LEFT_COUNT]){
				direction += 3;
			}else if(sum!=ext[EXT_OUT_FRONT_COUNT]){
				//
			}
		}
		compound.setInteger("dir", (direction%4+4)%4);
		moveEntity(entity);
	}
	
	@Override
	public HashMap<String, Integer> getReplacements(){
		return replacements;
	}
	
	@Override
	protected String[] getEntryVectors() {
		return entryVectors;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public PC_IGresGui openClientGui(EntityPlayer player, NBTTagCompound nbtTagCompound) {
		List<Diagnostic<? extends Void>> diagnostics;
		if(nbtTagCompound.hasKey("diagnostics")){
			diagnostics = new ArrayList<Diagnostic<? extends Void>>();
			NBTTagList list = (NBTTagList)nbtTagCompound.getTag("diagnostics");
			for(int i=0; i<list.tagCount(); i++){
				NBTTagCompound compound =list.getCompoundTagAt(i);
				diagnostics.add(PC_FakeDiagnostic.fromCompound(compound));
			}
		}else{
			diagnostics = null;
		}
		return new PCtr_GuiBeltScriptable(this, nbtTagCompound.getString("source"), diagnostics);
	}

	@Override
	public PC_GresBaseWithInventory openServerGui(EntityPlayer player) {
		return null;
	}
	
	@Override
	public NBTTagCompound sendOnGuiOpenToClient(EntityPlayer player) {
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		if(getSource()!=null)
			nbtTagCompound.setString("source", getSource());
		if(this.diagnostic!=null){
			List<Diagnostic<? extends Void>> diagnostics = this.diagnostic.getDiagnostics();
			NBTTagList list = new NBTTagList();
			for(Diagnostic<? extends Void> dgc:diagnostics){
				list.appendTag(PC_FakeDiagnostic.toCompound(dgc));
			}
			nbtTagCompound.setTag("diagnostics", list);
		}
		return nbtTagCompound;
	}

	public void sendSaveMessage(String text) {
		NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("type", 1);
		tagCompound.setString("source", text);
		sendMessage(tagCompound);
	}

	public void sendErrorMessage(){
		if(this.diagnostic!=null){
			List<Diagnostic<? extends Void>> diagnostics = this.diagnostic.getDiagnostics();
			NBTTagList list = new NBTTagList();
			for(Diagnostic<? extends Void> dgc:diagnostics){
				list.appendTag(PC_FakeDiagnostic.toCompound(dgc));
			}
			NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger("type", 2);
			tagCompound.setTag("diagnostics", list);
			sendMessage(tagCompound);
		}
	}
	
	@SideOnly(Side.CLIENT)
	private static void handleDiagnostic(NBTTagList list){
		PCtr_GuiBeltScriptable gui = PC_Gres.getCurrentClientGui(PCtr_GuiBeltScriptable.class);
		if(gui!=null){
			List<Diagnostic<? extends Void>> diagnostics = new ArrayList<Diagnostic<? extends Void>>();
			for(int i=0; i<list.tagCount(); i++){
				NBTTagCompound compound =list.getCompoundTagAt(i);
				diagnostics.add(PC_FakeDiagnostic.fromCompound(compound));
			}
			gui.setErrors(diagnostics);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onClientMessage(EntityPlayer player, NBTTagCompound tagCompound) {
		switch(tagCompound.getInteger("type")){
		case 2:
			handleDiagnostic((NBTTagList)tagCompound.getTag("diagnostics"));
			break;
		default:
			onMessage(player, tagCompound);
			break;
		}
	}

	@Override
	public void onMessage(EntityPlayer player, NBTTagCompound tagCompound) {
		switch(tagCompound.getInteger("type")){
		case 1:
			String text = tagCompound.getString("source");
			setSource(text);
			sendErrorMessage();
			break;
		case 2:
			break;
		default:
			break;
		}
	}
	
}
