package dev.mcexec.client.mixin;

import dev.mcexec.client.XrayRender;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Filters only the copied snapshot read by Minecraft's chunk mesh compiler.
 * Actual client-world blocks and authoritative server blocks remain unchanged.
 * Using the shared region also makes the neighbouring block-face tests see air.
 */
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void mcexec$block(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (XrayRender.shouldHide(cir.getReturnValue())) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }

    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void mcexec$fluid(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (!XrayRender.enabled()) return;
        // Compiled block state reflects the chosen render filter.
        var state = ((RenderSectionRegion) (Object) this).getBlockState(pos);
        if (state.isAir()) cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }
}
