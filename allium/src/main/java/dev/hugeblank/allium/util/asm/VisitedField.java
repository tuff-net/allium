package dev.hugeblank.allium.util.asm;


// Lnet/minecraft/world/World;blockEntityTickers:Ljava/util/List;
public record VisitedField(VisitedClass owner, int access, String name, String descriptor, String signature, Object value) implements VisitedMember {
    @Override
    public String unmappedMixinDescriptor() {
        return owner().getType().getDescriptor() + name + ":" + descriptor;
    }
}
