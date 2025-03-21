
local CactusBlockMixin = mixin.to("net.minecraft.block.CactusBlock")
addFlower = CactusBlockMixin:inject("add_flower", {
    method = { "randomTick(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)V" },
    at = { {
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
        } }
}, {
    mixin.getLocal("Lnet/minecraft/util/math/BlockPos;", "ordinal", 0),
    mixin.getLocal("I", "ordinal", 0)
})

CactusBlockMixin:build()