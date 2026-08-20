package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

/**
 * Identity of an AE2 sort-grouping family - either a Mekanism machine family (a regular machine plus
 * its Basic/Advanced/Elite/Ultimate Factory tiers, see {@link FactoryFamilyResolver}) or a
 * tier-prefixed item family with no non-tiered base identity of its own, e.g. Tier Installers (see
 * {@link TieredItemFamilyResolver}). {@code sortName}/{@code sortModId} anchor the whole family's
 * position in the overall NAME/MOD ordering, and {@code rank} orders members within the family
 * (ascending Lines count for machines, ascending {@code BaseTier} ordinal for tiered items).
 */
public record SortFamily(String sortName, String sortModId, int rank) {
}
