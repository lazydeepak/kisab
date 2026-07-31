# ADR-0001: Kisab v1 Product Boundary

## Status
Accepted

## Context
Kisab needs a clear initial boundary so that it can develop a product slice without contaminating the shared foundation with product-specific concepts.

## Decision
Kisab will own farm domain concepts and the first user journey for v1. The foundation will remain responsible for technical concerns such as secure session persistence and platform integration only.

## Consequences
- Kisab can iterate quickly on product value.
- The foundation remains neutral and reusable.
- Future cross-product capabilities can be extracted only after real evidence appears in multiple products.
