print("I'm feeling lucky!")

-- We have to be VERY careful about where we register our mixins. If your mixin is client-specific,
-- make sure to specify in mixin.to, as well as checking that the package.environment is "client".

-- For registering our recipe in the right location
local BrewingRecipeRegistryMixinBuilder = mixin.to("net.minecraft.recipe.BrewingRecipeRegistry")
BrewingRecipeRegistryMixinBuilder:inject("add_brewing_recipes", { -- Get the point at which potions should be registered.
    at = "TAIL",
    method = "registerDefaults(Lnet/minecraft/recipe/BrewingRecipeRegistry$Builder;)V"
})
-- Inject returns an event type for us to register to.
-- However it is better practice to register mixin events in another entrypoint.
:register(script, function(builder, ci)
    local Items = require("net.minecraft.item.Items")
    local Potions = require("net.minecraft.potion.Potions")
    print("registering lucky potion!")
    -- Register our lucky little potion
    builder:registerPotionRecipe(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK)
end)


BrewingRecipeRegistryMixinBuilder:build() -- Don't forget to actually build the mixin!