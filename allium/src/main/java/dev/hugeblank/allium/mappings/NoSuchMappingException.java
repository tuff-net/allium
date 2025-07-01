package dev.hugeblank.allium.mappings;

import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class NoSuchMappingException extends Exception {
    public NoSuchMappingException(VisitableMappingTree tree, String mapping) {
      super("No such mapping for value " + mapping + ". " + tree.getSrcNamespace() + "<->" + tree.getDstNamespaces().get(0));
    }
}
