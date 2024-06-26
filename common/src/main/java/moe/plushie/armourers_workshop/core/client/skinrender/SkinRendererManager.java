package moe.plushie.armourers_workshop.core.client.skinrender;

import moe.plushie.armourers_workshop.api.client.model.IModelHolder;
import moe.plushie.armourers_workshop.api.common.IEntityType;
import moe.plushie.armourers_workshop.api.skin.ISkinDataProvider;
import moe.plushie.armourers_workshop.core.client.layer.SkinWardrobeLayer;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderContext;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderData;
import moe.plushie.armourers_workshop.core.entity.EntityProfile;
import moe.plushie.armourers_workshop.init.ModEntityProfiles;
import moe.plushie.armourers_workshop.init.ModLog;
import moe.plushie.armourers_workshop.utils.ModelHolder;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class SkinRendererManager  {

    private static final SkinRendererManager INSTANCE = new SkinRendererManager();

    private final HashMap<IEntityType<?>, EntityProfile> entities = new HashMap<>();

    private final ArrayList<SkinRenderer.Factory<SkinRenderer<?, ?, ?>>> builders = new ArrayList<>();
    private final ArrayList<Pair<Class<?>, SkinRenderer.Plugin<?, ?, ?>>> plugins = new ArrayList<>();

    public static SkinRendererManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        EntityRenderDispatcher entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        if (entityRenderManager == null) {
            // call again later!!!
            RenderSystem.recordRenderCall(this::init);
            return;
        }
        RenderSystem.recordRenderCall(() -> _init(entityRenderManager));
    }

    private void _init(EntityRenderDispatcher entityRenderManager) {
        SkinRendererManager skinRendererManager = getInstance();

        for (EntityRenderer<? extends Player> renderer : entityRenderManager.playerRenderers.values()) {
            if (renderer instanceof LivingEntityRenderer<?, ?>) {
                skinRendererManager.setupRenderer(EntityType.PLAYER, (LivingEntityRenderer<?, ?>) renderer, true);
            }
        }

        entityRenderManager.renderers.forEach((entityType1, entityRenderer) -> {
            if (entityRenderer instanceof LivingEntityRenderer<?, ?>) {
                skinRendererManager.setupRenderer(entityType1, (LivingEntityRenderer<?, ?>) entityRenderer, true);
            }
        });

        // execute the pending tasks.
        entities.forEach((this::_bind));
    }

    public void unbind(IEntityType<?> entityType, EntityProfile entityProfile) {
        ModLog.debug("Detach Entity Renderer '{}'", entityType.getRegistryName());
        entities.remove(entityType);
        // TODO: remove layer in the entity renderer.
    }

    public void bind(IEntityType<?> entityType, EntityProfile entityProfile) {
        ModLog.debug("Attach Entity Renderer '{}'", entityType.getRegistryName());
        entities.put(entityType, entityProfile);
        // try call once _bind to avoid the bind method being called after init.
        _bind(entityType, entityProfile);
    }

    private void _bind(IEntityType<?> entityType, EntityProfile entityProfile) {
        EntityType<?> resolvedEntityType = entityType.get();
        if (resolvedEntityType == null) {
            return;
        }
        EntityRenderDispatcher entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        if (entityRenderManager == null) {
            return;
        }
        // Add our own custom armor layer to the various player renderers.
        if (resolvedEntityType == EntityType.PLAYER) {
            for (EntityRenderer<? extends Player> renderer : entityRenderManager.playerRenderers.values()) {
                if (renderer instanceof LivingEntityRenderer<?, ?>) {
                    setupRenderer(resolvedEntityType, (LivingEntityRenderer<?, ?>) renderer, false);
                }
            }
        }
        // Add our own custom armor layer to everything that has an armor layer
        entityRenderManager.renderers.forEach((entityType1, renderer) -> {
            if (resolvedEntityType.equals(entityType1)) {
                if (renderer instanceof LivingEntityRenderer<?, ?>) {
                    setupRenderer(resolvedEntityType, (LivingEntityRenderer<?, ?>) renderer, false);
                }
            }
        });
    }

    public <T extends SkinRenderer<?, ?, ?>> void registerPlugin(Class<T> targetType, SkinRenderer.Plugin<?, ?, ?> plugin) {
        plugins.add(Pair.of(targetType, plugin));
    }

    public <T extends LivingEntity, V extends EntityModel<T>, M extends IModelHolder<V>> Collection<SkinRenderer.Plugin<T, V, M>> getPlugins(SkinRenderer<T, V, M> renderer) {
        ArrayList<SkinRenderer.Plugin<T, V, M>> plugins1 = new ArrayList<>();
        plugins.forEach(pair -> {
            if (pair.getKey().isInstance(renderer)) {
                plugins1.add(ObjectUtils.unsafeCast(pair.getValue()));
            }
        });
        return plugins1;
    }

    public void registerRenderer(SkinRenderer.Factory<SkinRenderer<?, ?, ?>> builder) {
        builders.add(builder);
    }

    @Nullable
    public <T extends Entity, V extends Model, M extends IModelHolder<V>> SkinRenderer<T, V, M> getRenderer(@Nullable T entity, @Nullable Model entityModel, @Nullable EntityRenderer<?> entityRenderer) {
        if (entity == null) {
            return null;
        }
        EntityType<?> entityType = entity.getType();
        // when the caller does not provide the entity renderer we need to query it from managers.
        if (entityRenderer == null) {
            entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        }
        // when the caller does not provide the entity model we need to query it from entity render.
        if (entityModel == null) {
            entityModel = getModel(entityRenderer);
        }
        return getRenderer(entityType, entityModel, entityRenderer);
    }

    @Nullable
    protected <T extends Entity, V extends Model, M extends IModelHolder<V>> SkinRenderer<T, V, M> getRenderer(EntityType<?> entityType, Model entityModel, EntityRenderer<?> entityRenderer) {
        // in the normal, the entityRenderer only one model type,
        // but some mods(Custom NPC) generate dynamically models,
        // so we need to be compatible with that
        Storage<T, V, M> storage = Storage.of(entityRenderer);
        return storage.computeIfAbsent(entityModel, key -> createRenderer(entityType, entityRenderer, entityModel));
    }


    @Nullable
    protected <T extends Entity, V extends Model, M extends IModelHolder<V>> SkinRenderer<T, V, M> createRenderer(EntityType<?> entityType, EntityRenderer<?> entityRenderer, Model entityModel) {
        EntityProfile entityProfile = ModEntityProfiles.getProfile(entityType);
        if (entityProfile == null) {
            return null;
        }
        for (SkinRenderer.Factory<SkinRenderer<?, ?, ?>> builder : builders) {
            SkinRenderer<?, ?, ?> skinRenderer = builder.create(entityType, entityRenderer, entityModel, entityProfile);
            if (skinRenderer != null) {
                return ObjectUtils.unsafeCast(skinRenderer);
            }
        }
        return null;
    }

    protected EntityModel<?> getModel(EntityRenderer<?> entityRenderer) {
        if (entityRenderer instanceof RenderLayerParent) {
            return ((RenderLayerParent<?, ?>) entityRenderer).getModel();
        }
        return null;
    }

    private <T extends LivingEntity, V extends EntityModel<T>, M extends IModelHolder<V>> void setupRenderer(EntityType<?> entityType, LivingEntityRenderer<T, V> livingRenderer, boolean autoInject) {
        RenderLayer<T, V> armorLayer = null;
        for (RenderLayer<T, V> layerRenderer : livingRenderer.layers) {
            if (layerRenderer instanceof HumanoidArmorLayer<?, ?, ?>) {
                armorLayer = layerRenderer;
            }
            if (layerRenderer instanceof SkinWardrobeLayer) {
                return; // ignore, only one.
            }
        }
        if (autoInject && armorLayer == null) {
            return;
        }
        SkinRenderer<T, V, M> skinRenderer = getRenderer(entityType, livingRenderer.getModel(), livingRenderer);
        if (skinRenderer != null) {
            livingRenderer.addLayer(new SkinWardrobeLayer<>(skinRenderer, livingRenderer));
        }
    }


    public <T extends Entity, V extends Model, M extends IModelHolder<V>> void willRender(T entity, V entityModel, @Nullable EntityRenderer<?> entityRenderer, SkinRenderData renderData, Supplier<SkinRenderContext> context) {
        SkinRenderer<T, V, M> renderer = getRenderer(entity, entityModel, entityRenderer);
        if (renderer != null) {
            SkinRenderContext context1 = context.get();
            renderer.willRender(entity, ModelHolder.of(entityModel), renderData, context1);
            context1.release();
        }
    }

    public <T extends Entity, V extends Model, M extends IModelHolder<V>> void willRenderModel(T entity, V entityModel, @Nullable EntityRenderer<?> entityRenderer, SkinRenderData renderData, Supplier<SkinRenderContext> context) {
        SkinRenderer<T, V, M> renderer = getRenderer(entity, entityModel, entityRenderer);
        if (renderer != null) {
            SkinRenderContext context1 = context.get();
            renderer.willRenderModel(entity, ModelHolder.of(entityModel), renderData, context1);
            context1.release();
        }
    }

    public <T extends Entity, V extends Model, M extends IModelHolder<V>> void didRender(T entity, V entityModel, @Nullable EntityRenderer<?> entityRenderer, SkinRenderData renderData, Supplier<SkinRenderContext> context) {
        SkinRenderer<T, V, M> renderer = getRenderer(entity, entityModel, entityRenderer);
        if (renderer != null) {
            SkinRenderContext context1 = context.get();
            renderer.didRender(entity, ModelHolder.of(entityModel), renderData, context1);
            context1.release();
        }
    }

    public static class Storage<T extends Entity, V extends Model, M extends IModelHolder<V>> {

        private final HashMap<Object, SkinRenderer<T, V, M>> skinRenderers = new HashMap<>();

        public static <T extends Entity, V extends Model, M extends IModelHolder<V>> Storage<T, V, M> of(EntityRenderer<?> entityRenderer) {
            ISkinDataProvider dataProvider = (ISkinDataProvider) entityRenderer;
            Storage<T, V, M> storage = dataProvider.getSkinData();
            if (storage == null) {
                storage = new Storage<>();
                dataProvider.setSkinData(storage);
            }
            return storage;
        }

        public SkinRenderer<T, V, M> computeIfAbsent(Model entityModel, Function<Object, SkinRenderer<T, V, M>> provider) {
            return skinRenderers.computeIfAbsent(getModelClass(entityModel), provider);
        }

        private Class<?> getModelClass(Model model) {
            if (model != null) {
                return model.getClass();
            }
            return Model.class;
        }
    }
}
