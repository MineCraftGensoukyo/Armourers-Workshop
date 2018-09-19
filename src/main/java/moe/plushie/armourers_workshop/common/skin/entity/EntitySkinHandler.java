package moe.plushie.armourers_workshop.common.skin.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import moe.plushie.armourers_workshop.ArmourersWorkshop;
import moe.plushie.armourers_workshop.api.common.skin.data.ISkinDescriptor;
import moe.plushie.armourers_workshop.api.common.skin.entity.IEntitySkinHandler;
import moe.plushie.armourers_workshop.api.common.skin.entity.ISkinnableEntity;
import moe.plushie.armourers_workshop.api.common.skin.type.ISkinType;
import moe.plushie.armourers_workshop.common.config.ConfigHandler;
import moe.plushie.armourers_workshop.common.library.ILibraryManager;
import moe.plushie.armourers_workshop.common.library.LibraryFile;
import moe.plushie.armourers_workshop.common.library.LibraryFileList;
import moe.plushie.armourers_workshop.common.skin.EntityEquipmentData;
import moe.plushie.armourers_workshop.common.skin.data.SkinDescriptor;
import moe.plushie.armourers_workshop.common.skin.data.SkinIdentifier;
import moe.plushie.armourers_workshop.utils.ModLogger;
import moe.plushie.armourers_workshop.utils.SkinNBTHelper;
import moe.plushie.armourers_workshop.utils.UtilItems;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class EntitySkinHandler implements IEntitySkinHandler {
    
    public static EntitySkinHandler INSTANCE;
    
    private HashMap<Class <? extends EntityLivingBase>, ISkinnableEntity> entityMap;
    
    public static void init() {
        INSTANCE = new EntitySkinHandler();
    }
    
    public EntitySkinHandler() {
        MinecraftForge.EVENT_BUS.register(this);
        entityMap = new HashMap<Class <? extends EntityLivingBase>, ISkinnableEntity>();
        registerEntities();
    }
    
    private void registerEntities() {
        registerEntity(new SkinnableEntityChicken());
        registerEntity(new SkinnableEntityCreeper());
        registerEntity(new SkinnableEntityGhast());
        registerEntity(new SkinnableEntityPlayer());
        registerEntity(new SkinnableEntitySkeleton());
        registerEntity(new SkinnableEntitySlime());
        registerEntity(new SkinnableEntityZombie());
    }
    
    @Override
    public void registerEntity(ISkinnableEntity skinnableEntity) {
        if (skinnableEntity == null) {
            return;
        }
        if (skinnableEntity.getEntityClass() == null) {
            return;
        }
        ModLogger.log(String.format("Registering %s as a skinnable entity.", skinnableEntity.getEntityClass()));
        ArrayList<Class<? extends EntityLivingBase>> classes = skinnableEntity.getEntityClass();
        for (int i = 0; i < classes.size(); i++) {
            entityMap.put(classes.get(i), skinnableEntity);
        }
    }
    
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity().getEntityWorld().isRemote) {
            return;
        }
        
        Entity entity = event.getTarget();
        ExPropsEntityEquipmentData props = ExPropsEntityEquipmentData.getExtendedPropsForEntity(entity);
        if (props != null) {
            props.sendEquipmentDataToPlayer((EntityPlayerMP) event.getEntityPlayer());
        }
    }
    
    @Override
    public boolean isValidEntity(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            if (entityMap.containsKey(entity.getClass())) {
                return true;
            }
        }
        return false;
    }
    
    public ISkinnableEntity geSkinnableEntity(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            return entityMap.get(entity.getClass());
        }
        return null;
    }
    
    public boolean canUseWandOfStyleOnEntity(Entity entity) {
        if (isValidEntity(entity)) {
            ISkinnableEntity skinnableEntity = entityMap.get(entity.getClass());
            return skinnableEntity.canUseWandOfStyle();
        }
        return false;
    }
    
    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        Entity entity = event.getEntity();
        if (isValidEntity(entity)) {
            ISkinnableEntity skinnableEntity = entityMap.get(entity.getClass());
            ExPropsEntityEquipmentData.register(entity, skinnableEntity);
        }
    }
    
    @SubscribeEvent
    public void onLivingDeathEvent(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (isValidEntity(entity)) {
            if (entity.getEntityWorld() != null && !entity.getEntityWorld().isRemote) {
                dropEntitySkins(entity); 
            }
        }
    }
    
    private void dropEntitySkins(Entity entity) {
        if (ConfigHandler.entityDropSkinChance <= 0) {
            return;
        }
        int rnd = entity.getEntityWorld().rand.nextInt(99) + 1;
        if (rnd <= ConfigHandler.entityDropSkinChance) {
            ExPropsEntityEquipmentData entityEquipmentData = ExPropsEntityEquipmentData.getExtendedPropsForEntity(entity);
            if (entityEquipmentData != null) {
                ArrayList<ISkinType> skinTypes = entityEquipmentData.getSkinInventory().getSkinTypes();
                for (int i = 0; i < skinTypes.size(); i++) {
                    ISkinDescriptor skinPointer = entityEquipmentData.getEquipmentData().getSkinPointer(skinTypes.get(i), 0);
                    if (skinPointer != null) {
                        ItemStack stack = SkinNBTHelper.makeEquipmentSkinStack((SkinDescriptor) skinPointer);
                        UtilItems.spawnItemAtEntity(entity, stack);
                    }
                }
            }
        }
    }
    
    public void giveRandomSkin(Entity entity) {
        giveRandomSkin(ExPropsEntityEquipmentData.getExtendedPropsForEntity(entity));
    }
    
    public void giveRandomSkin(ExPropsEntityEquipmentData entityEquipmentData) {
        if (entityEquipmentData == null) {
            return;
        }
        if (ConfigHandler.enitiySpawnWithSkinsChance <= 0) {
            return;
        }

        
        ArrayList<ISkinType> skinTypes = entityEquipmentData.getSkinInventory().getSkinTypes();
        for (int i = 0; i < skinTypes.size(); i++) {
            int rnd = entityEquipmentData.getEntity().getEntityWorld().rand.nextInt(99) + 1;
            if (rnd >= ConfigHandler.enitiySpawnWithSkinsChance) {
                continue;
            }
            ISkinType skinType = skinTypes.get(i);
            LibraryFile libraryFile = getRandomSkinOfType(skinType);
            if (libraryFile == null) {
                continue;
            }
            SkinIdentifier identifier = new SkinIdentifier(0, libraryFile, 0, skinType);
            ItemStack skinStack = SkinNBTHelper.makeEquipmentSkinStack(new SkinDescriptor(identifier));
            
            if (skinStack == null) {
                continue;
            }
            entityEquipmentData.getSkinInventory().setInventorySlotContents(i, skinStack);
        }
    }
    
    public LibraryFile getRandomSkinOfType(ISkinType skinType) {
        ILibraryManager libraryManager = ArmourersWorkshop.getProxy().libraryManager;
        
        LibraryFileList fileList = null;
        if (ArmourersWorkshop.isDedicated()) {
            fileList = libraryManager.getServerPublicFileList();
        } else {
            fileList = libraryManager.getClientPublicFileList();
        }
        
        ArrayList<LibraryFile> typeList = fileList.getCachedFileListForSkinType(skinType);
        if (typeList == null) {
            return null;
        }
        ArrayList<LibraryFile> validFiles = new ArrayList<LibraryFile>();
        for (int i = 0; i < typeList.size(); i++) {
            if (typeList.get(i).filePath.startsWith(ConfigHandler.enitiySpawnSkinTargetPath)) {
                validFiles.add(typeList.get(i));
            }
        }
        //touhou
        Random random = new Random();
        if (!validFiles.isEmpty()) {
            return validFiles.get(random.nextInt(validFiles.size()));
        }
        return null;
    }
    
    @SideOnly(Side.CLIENT)
    public void receivedEquipmentData(EntityEquipmentData equipmentData, int entityId) {
        World world = Minecraft.getMinecraft().world;
        Entity entity = world.getEntityByID(entityId);
        if (entity != null) {
            ExPropsEntityEquipmentData props = ExPropsEntityEquipmentData.getExtendedPropsForEntity(entity);
            if (props != null) {
                props.setEquipmentData(equipmentData);
            }
        }
    }

    public ArrayList<ISkinnableEntity> getRegisteredEntities() {
        ArrayList<ISkinnableEntity> entityList = new ArrayList<ISkinnableEntity>();
        for (int i = 0; i < entityMap.size(); i++) {
            Class <? extends EntityLivingBase> entityClass;
            entityClass = (Class <? extends EntityLivingBase>)entityMap.keySet().toArray()[i];
            ISkinnableEntity entity = entityMap.get(entityClass);
            if (entity != null) {
                entityList.add(entity);
            }
        }
        return entityList;
    }
}
