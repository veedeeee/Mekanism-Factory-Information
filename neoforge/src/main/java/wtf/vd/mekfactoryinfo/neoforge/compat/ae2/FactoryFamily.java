package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

/**
 * Identity of a Mekanism machine "family" (a regular machine plus its Basic/Advanced/Elite/Ultimate
 * Factory tiers) used to group them together when the AE2 terminal Lines-grouping sort toggle is
 * enabled. {@code sortName} and {@code sortModId} are always taken from the family's base
 * (non-Factory) machine block, which acts as the whole group's representative identity for sort
 * position purposes, regardless of which mod actually registered the specific block being sorted.
 */
public record FactoryFamily(String sortName, String sortModId) {
}

