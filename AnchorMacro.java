package dk.anchormacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tryk på tasten (standard Z): sørger for en totem i offhand, placerer et respawn anchor der hvor
 * du kigger, og fylder det med glowstone. Selve sprængningen gør du selv.
 * Kræver et respawn anchor og glowstone i hotbaren.
 */
public class AnchorMacro implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("anchormacro");

    private enum State { IDLE, PLACE, CHARGE }

    private static KeyBinding placeKey;

    private State state = State.IDLE;
    private int timer, chargesDone, prevSlot, anchorSlot, glowSlot;
    private BlockPos anchorPos;
    private BlockHitResult placeHit;

    @Override
    public void onInitializeClient() {
        AnchorConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("anchormacro", "main"));
        placeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anchormacro.place", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        LOGGER.info("Anchor Macro indlæst");
    }

    private void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Anchor Macro] " + s), true);
    }

    private int findHotbar(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /** Flytter en totem fra inventory/hotbar ind i offhand. */
    private boolean equipTotem(MinecraftClient mc) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? 36 + i : i;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40,
                        SlotActionType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }

    private void trigger(MinecraftClient mc) {
        if (state != State.IDLE) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            msg(mc, "Kig på en blok");
            return;
        }
        anchorSlot = findHotbar(mc, Items.RESPAWN_ANCHOR);
        glowSlot = findHotbar(mc, Items.GLOWSTONE);
        if (anchorSlot < 0) { msg(mc, "Intet respawn anchor i hotbar"); return; }
        if (glowSlot < 0) { msg(mc, "Ingen glowstone i hotbar"); return; }

        if (AnchorConfig.requireTotem && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            if (!(AnchorConfig.autoEquipTotem && equipTotem(mc))) {
                msg(mc, "Du har ingen totem");
                return;
            }
        }

        placeHit = hit;
        prevSlot = mc.player.getInventory().getSelectedSlot();
        timer = 0;
        state = State.PLACE;
    }

    private void finish(MinecraftClient mc) {
        if (AnchorConfig.restoreSlot && mc.player != null) mc.player.getInventory().setSelectedSlot(prevSlot);
        state = State.IDLE;
    }

    private void onTick(MinecraftClient mc) {
        while (placeKey.wasPressed()) trigger(mc);

        if (state == State.IDLE) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            state = State.IDLE;
            return;
        }

        if (state == State.PLACE) {
            mc.player.getInventory().setSelectedSlot(anchorSlot);
            BlockPos target = placeHit.getBlockPos();
            BlockPos pos = mc.world.getBlockState(target).isReplaceable() ? target : target.offset(placeHit.getSide());

            ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeHit);
            if (!result.isAccepted()) {
                msg(mc, "Kunne ikke placere anchor");
                finish(mc);
                return;
            }
            anchorPos = pos;
            chargesDone = 0;
            timer = 0;
            state = State.CHARGE;
            return;
        }

        // CHARGE: én glowstone pr. tick
        BlockState bs = mc.world.getBlockState(anchorPos);
        if (!bs.isOf(Blocks.RESPAWN_ANCHOR)) {
            if (++timer > 6) {
                msg(mc, "Anchoret blev ikke placeret");
                finish(mc);
            }
            return;
        }
        // Sikkerhed: et fuldt anchor (4) må aldrig få mere glowstone, ellers sprænger det
        int current = bs.get(RespawnAnchorBlock.CHARGES);
        if (chargesDone >= AnchorConfig.charges || current >= 4) {
            finish(mc);
            return;
        }
        mc.player.getInventory().setSelectedSlot(glowSlot);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(anchorPos), Direction.UP, anchorPos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        chargesDone++;
    }
}
