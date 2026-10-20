package jaxon.ric.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides Minecraft 26.3's harmless false-positive support-block INFO line. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SuppressStandingOnAirLogMixin {
    @Redirect(method = "forceSendPlayerSupportBlocks", at = @At(value = "INVOKE", target =
            "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void ric$suppressSupportBlockInfo(Logger logger, String message, Object playerName) {
        // Keep the server's block resynchronization logic intact; only its
        // repetitive console line is suppressed on 26.3.
    }
}
