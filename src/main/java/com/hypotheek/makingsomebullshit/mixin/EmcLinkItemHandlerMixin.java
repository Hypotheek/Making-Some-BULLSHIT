package com.hypotheek.makingsomebullshit.mixin;

import com.hypotheek.makingsomebullshit.tracker.LinkSampler;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.math.BigInteger;

/**
 * A link doesn't record which items were imported, only how many, so their EMC value can't be recovered
 * afterwards. This watches the one place insertItem credits the owner and reports the amount to the sampler.
 */
@Mixin(targets = "cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink$ItemHandler", remap = false)
public abstract class EmcLinkItemHandlerMixin {
    // The link this handler belongs to: javac's synthetic reference from an inner class to its outer instance.
    @Shadow(remap = false)
    @Final
    private BlockEntityEMCLink this$0;

    @Redirect(
            method = "insertItem",
            at = @At(value = "INVOKE", target = "Lmoze_intel/projecte/api/capabilities/IKnowledgeProvider;setEmc(Ljava/math/BigInteger;)V"),
            remap = false
    )
    private void makingsomebullshit$recordImportedEmc(IKnowledgeProvider provider, BigInteger newEmc) {
        LinkSampler.recordImportedEmc(this.this$0, newEmc.subtract(provider.getEmc()));
        provider.setEmc(newEmc);
    }
}
