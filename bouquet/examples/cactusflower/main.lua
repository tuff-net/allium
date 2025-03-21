local Blocks = require("net.minecraft.block.Blocks")

addFlower:register(script, function(self, state, world, pos, random, ci, blockPos, i)
    if i == 2 then
        world:setBlockState(
                blockPos:up(),
                Blocks.ALLIUM:getDefaultState()
        )
    end
end)