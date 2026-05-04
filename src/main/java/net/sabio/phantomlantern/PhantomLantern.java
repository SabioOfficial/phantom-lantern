package net.sabio.phantomlantern;

import net.fabricmc.api.ModInitializer;

import net.sabio.phantomlantern.logic.PhantomLanternLogic;
import net.sabio.phantomlantern.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomLantern implements ModInitializer {
	public static final String MOD_ID = "phantom-lantern";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		ModItems.registerEvents();
		PhantomLanternLogic.register();
	}
}