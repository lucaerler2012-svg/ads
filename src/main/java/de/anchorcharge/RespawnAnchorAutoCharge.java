package de.anchorcharge;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Setzt du einen Respawn-Anker, wird er automatisch mit Glowstone aufgeladen
 * (bis zur vollen Ladung 4), sofern du Glowstone in der Hotbar hast.
 */
public final class RespawnAnchorAutoCharge {

    private static BlockPos[] candidates;
    private static int timeout;
    private static BlockPos target;
    private static int previousSlot = -1;

    private RespawnAnchorAutoCharge() {}

    public static void register() {
        // Merkt sich, wo ein Respawn-Anker gesetzt wurde.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() && player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR)) {
                BlockPos clicked = hit.getBlockPos();
                // Je nach Zielblock landet der Anker auf dem Block selbst (z.B. Gras) oder daneben.
                candidates = new BlockPos[] { clicked, clicked.relative(hit.getDirection()) };
                timeout = 10;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(RespawnAnchorAutoCharge::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            reset();
            return;
        }

        if (target == null) {
            if (candidates == null) {
                return;
            }
            if (timeout-- <= 0) {
                reset();
                return;
            }
            for (BlockPos pos : candidates) {
                BlockState state = client.level.getBlockState(pos);
                if (state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
                    target = pos;
                    previousSlot = client.player.getInventory().getSelectedSlot();
                    break;
                }
            }
            if (target == null) {
                return;
            }
        }

        BlockState state = client.level.getBlockState(target);
        if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) >= 4) {
            finish(client);
            return;
        }

        Inventory inv = client.player.getInventory();
        int glowSlot = -1;
        for (int slot = 0; slot < 9; slot++) {
            if (inv.getItem(slot).is(Items.GLOWSTONE)) {
                glowSlot = slot;
                break;
            }
        }
        if (glowSlot < 0) {
            finish(client);
            return;
        }

        inv.setSelectedSlot(glowSlot);
        client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
    }

    private static void finish(Minecraft client) {
        if (client.player != null && previousSlot >= 0) {
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
        reset();
    }

    private static void reset() {
        candidates = null;
        target = null;
        previousSlot = -1;
        timeout = 0;
    }
}
