package powercraft.transport;

import net.minecraft.item.ItemStack;
import powercraft.api.PC_Api;
import powercraft.api.PC_Build;
import powercraft.api.PC_Module;
import powercraft.api.network.PC_PacketHandler;
import powercraft.transport.block.PCtr_BlockBeltBidirectional;
import powercraft.transport.block.PCtr_BlockBeltNormal;
import powercraft.transport.block.PCtr_BlockBeltScriptable;
import powercraft.transport.block.PCtr_BlockEjector;
import powercraft.transport.block.PCtr_PacketSetEntitySpeed;
import powercraft.transport.item.PCtr_ItemSlimeBoots;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.InstanceFactory;

@Mod(modid = PCtr_Transport.NAME, name = PCtr_Transport.NAME, version = PCtr_Transport.VERSION, dependencies=PCtr_Transport.DEPENDENCIES)
public class PCtr_Transport extends PC_Module {

	public static final String NAME = POWERCRAFT+"-Transport";
	public static final String VERSION = PC_Build.BUILD_VERSION;
	public static final String DEPENDENCIES = "required-after:"+PC_Api.NAME+"@"+PC_Api.VERSION;
	
	public static final PCtr_Transport INSTANCE = new PCtr_Transport();
	
	public static final PCtr_BlockBeltScriptable ScriptableBelt = new PCtr_BlockBeltScriptable();
	
	public static final PCtr_BlockBeltNormal normalBelt = new PCtr_BlockBeltNormal();
	
	public static final PCtr_BlockBeltBidirectional bidirectionalBelt = new PCtr_BlockBeltBidirectional();
	
	public static final PCtr_BlockEjector EJECTOR = new PCtr_BlockEjector();
	
	public static final PCtr_ItemSlimeBoots SLIME_BOOTS = new PCtr_ItemSlimeBoots();
	
	@InstanceFactory
	public static PCtr_Transport factory(){
		return INSTANCE;
	}

	private PCtr_Transport(){
		PC_PacketHandler.registerPacket(PCtr_PacketSetEntitySpeed.class);
	}
	
	@Override
	public ItemStack getCreativeTabItemStack() {
		return new ItemStack(ScriptableBelt);
	}

}
