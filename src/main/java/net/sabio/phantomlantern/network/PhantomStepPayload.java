package net.sabio.phantomlantern.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sabio.phantomlantern.PhantomLantern;
import org.jspecify.annotations.NonNull;

public record PhantomStepPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhantomStepPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, "phantom_step"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhantomStepPayload> CODEC = StreamCodec.unit(new PhantomStepPayload());

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
