package com.decathlon.idp_core.domain.model.entity;

import java.util.UUID;

/// Lightweight projection carrying only the identifying fields of an
/// [Entity] — `id`, `templateIdentifier`, `identifier`, `name` — without its
/// properties or relations.
///
/// **Business purpose:** Some callers only need to resolve which entity a
/// business key (`templateIdentifier` + `identifier`) refers to, including
/// its technical UUID, without paying the cost of hydrating and mapping the
/// full property/relation graph. The primary consumer is graph traversal
/// (`EntityGraphService#getEntityGraph`), which resolves the root entity's
/// identity here and then loads its relationship graph via a dedicated batch
/// query — the properties/relations on the root [Entity] would otherwise be
/// discarded, so fetching them was pure overhead (perf #131).
public record EntityIdentity(UUID id, String templateIdentifier, String identifier, String name) {
}
