package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@LuaWrapped
public class CommonEvents implements Events {

    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerTick> PLAYER_TICK;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerDeath> PLAYER_DEATH;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockInteract> BLOCK_INTERACT;

    static {
        // player gets ticked
        PLAYER_TICK = new SimpleEventType<>(Identifier.of("allium:common/player_tick"));
        // player dies
        PLAYER_DEATH = new SimpleEventType<>(Identifier.of("allium:common/player_death"));
        // player interacts (right-clicks) with a block
        BLOCK_INTERACT = new SimpleEventType<>(Identifier.of("allium:common/block_interact"));
    }

    public static class CommonEventHandlers {

        public interface PlayerTick {
            void onPlayerTick(PlayerEntity player);
        }

        public interface PlayerDeath {
            void onPlayerDeath(PlayerEntity player, DamageSource source);
        }

        public interface PlayerBlockInteract {
            ActionResult onPlayerBlockInteraction(
                BlockState state,
                World world,
                BlockPos pos,
                PlayerEntity player,
                Hand hand,
                BlockHitResult hit
            );
        }
    }
}
