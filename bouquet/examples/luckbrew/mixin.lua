print("I'm feeling lucky!")

-- We have to be VERY careful about where we register our mixins. If your mixin is client-specific,
-- make sure to specify in mixin.to, as well as checking that the package.environment is "client".

local Items, Potions

-- For registering our recipe in the right location
local BrewingRecipeRegistryMixinBuilder = mixin.to("net.minecraft.recipe.BrewingRecipeRegistry")
BrewingRecipeRegistryMixinBuilder
        :inject("add_brewing_recipes", { -- Get the point at which potions should be registered.
            at = "TAIL",
            method = "registerDefaults(Lnet/minecraft/recipe/BrewingRecipeRegistry$Builder;)V"
        })
        -- Inject returns an event type for us to register to. We could also get this anywhere else in our code using:
        -- `mixin.get(Identifier.of("lucky_brew:add_brewing_recipes"))`.
        :register(script, function(builder, ci)
            if (not Items) then
                -- import classes here to not cause nasty crashes.
                Items = require("net.minecraft.item.Items")
                Potions = require("net.minecraft.potion.Potions")
            end
            -- Register our lucky little potion
            builder:registerPotionRecipe(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK)
        end)

BrewingRecipeRegistryMixinBuilder:build()