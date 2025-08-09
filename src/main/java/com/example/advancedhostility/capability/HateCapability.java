package com.example.advancedhostility.capability;

import com.example.advancedhostility.AdvancedHostility;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HateCapability {
    public static final Capability<IHate> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(AdvancedHostility.MOD_ID, "hate");

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final LazyOptional<IHate> instance = LazyOptional.of(Hate::new);
        @NotNull @Override public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == INSTANCE ? instance.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() {
            CompoundTag nbt = new CompoundTag();
            instance.ifPresent(hate -> hate.getHateMap().forEach((uuid, value) -> nbt.putDouble(uuid.toString(), value)));
            return nbt;
        }
        @Override public void deserializeNBT(CompoundTag nbt) {
            instance.ifPresent(hate -> {
                hate.clear();
                for (String key : nbt.getAllKeys()) {
                    try {
                        hate.getHateMap().put(UUID.fromString(key), nbt.getDouble(key));
                    } catch (IllegalArgumentException e) {
                        AdvancedHostility.LOGGER.warn("Could not deserialize invalid UUID from hate capability NBT: {}", key);
                    }
                }
            });
        }
    }
}
