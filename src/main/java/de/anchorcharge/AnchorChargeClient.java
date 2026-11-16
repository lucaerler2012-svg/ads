package de.anchorcharge;

import net.fabricmc.api.ClientModInitializer;

public class AnchorChargeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RespawnAnchorAutoCharge.register();
    }
}
