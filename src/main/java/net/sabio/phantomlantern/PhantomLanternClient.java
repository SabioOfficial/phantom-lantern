package net.sabio.phantomlantern;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.sabio.phantomlantern.network.PhantomStepPayload;
import org.lwjgl.glfw.GLFW;

public class PhantomLanternClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, "phantom-lantern")
    );

    public static KeyMapping PHANTOM_STEP_KEY;

    @Override
    public void onInitializeClient() {
        PHANTOM_STEP_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.phantom-lantern.phantom_step",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        CATEGORY
                )
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (PHANTOM_STEP_KEY.consumeClick()) {
                ClientPlayNetworking.send(new PhantomStepPayload());
            }
        });
    }
}
