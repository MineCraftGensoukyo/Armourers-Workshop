package moe.plushie.armourers_workshop.compatibility.forge;

import moe.plushie.armourers_workshop.api.common.ICapabilityType;
import moe.plushie.armourers_workshop.api.common.IRegistryKey;
import moe.plushie.armourers_workshop.api.common.ITagRepresentable;
import moe.plushie.armourers_workshop.core.capability.SkinWardrobe;
import moe.plushie.armourers_workshop.init.ModConstants;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractForgeCapabilityManager {

    @CapabilityInject(SkinWardrobe.class)
    public static Capability<SkinWardrobe> WARDROBE_KEY;

    public static <T> IRegistryKey<ICapabilityType<T>> register(String name, Class<T> type, Function<Entity, Optional<T>> factory) {
        if (type == SkinWardrobe.class) {
            return ObjectUtils.unsafeCast(createWardrobeCapabilityType(name, ObjectUtils.unsafeCast(factory)));
        }
        throw new AssertionError();
    }

    private static IRegistryKey<ICapabilityType<SkinWardrobe>> createWardrobeCapabilityType(String registryName, Function<Entity, Optional<SkinWardrobe>> provider) {
        ResourceLocation name = ModConstants.key(registryName);
        ICapabilityType<SkinWardrobe> capabilityType = entity -> entity.getCapability(WARDROBE_KEY).resolve();
        return new RegistryObjectProxy<>(name, SkinWardrobe.class, provider, capabilityType, () -> WARDROBE_KEY);
    }

    public static class RegistryObjectProxy<N extends Tag, T extends ITagRepresentable<N>> implements IRegistryKey<ICapabilityType<T>> {

        final ResourceLocation registryName;
        final Supplier<Capability<T>> capability;

        final Class<T> type;
        final ICapabilityType<T> capabilityType;
        final Function<Entity, Optional<T>> factory;

        protected RegistryObjectProxy(ResourceLocation registryName, Class<T> type, Function<Entity, Optional<T>> factory, ICapabilityType<T> capabilityType, Supplier<Capability<T>> capability) {
            this.type = type;
            this.factory = factory;
            this.capability = capability;
            this.capabilityType = capabilityType;
            this.registryName = registryName;
            this.setupBus();
        }

        public void setupBus() {
            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntityCapability);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCapability);
        }

        public void registerCapability(FMLLoadCompleteEvent event) {
            CapabilityManager.INSTANCE.register(type, new Capability.IStorage<T>() {
                @Override
                public Tag writeNBT(Capability<T> capability, T object, Direction arg) {
                    return null;
                }

                @Override
                public void readNBT(Capability<T> capability, T object, Direction arg, Tag arg2) {
                }
            }, () -> null);
        }

        public void attachEntityCapability(AttachCapabilitiesEvent<Entity> event) {
            Optional<T> value = factory.apply(event.getObject());
            if (!value.isPresent()) {
                return;
            }
            event.addCapability(registryName, new CapabilityProviderProxy<N, T>(value.get()) {
                @Override
                public Capability<T> getCapability() {
                    return capability.get();
                }
            });
        }

        @Override
        public ICapabilityType<T> get() {
            return capabilityType;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return registryName;
        }
    }

    public static abstract class CapabilityProviderProxy<N extends Tag, T extends ITagRepresentable<N>> implements ICapabilityProvider, INBTSerializable<N> {

        protected final T value;

        protected CapabilityProviderProxy(T value) {
            this.value = value;
        }

        @Override
        public N serializeNBT() {
            return value.serializeNBT();
        }

        @Override
        public void deserializeNBT(N nbt) {
            value.deserializeNBT(nbt);
        }

        @NotNull
        @Override
        public <I> LazyOptional<I> getCapability(@NotNull Capability<I> cap, @Nullable Direction arg) {
            return getCapability().orEmpty(cap, LazyOptional.of(() -> value));
        }

        public abstract Capability<T> getCapability();
    }
}
