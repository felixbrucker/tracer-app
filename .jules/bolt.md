## 2026-03-31 - Pre-compile Regex Patterns in High-Frequency Parsing Streams
**Learning:** Instantiating `.toRegex()` inside functions that process stdout lines during active Ping, Traceroute, and MTR streams creates excessive Regex allocations and compilation overhead on every single output line.
**Action:** Extract Regex pattern instances to a static `companion object` so they are compiled once during application lifecycle.

## 2026-11-02 - Avoid Unrolling Standard Collection Functions for Micro-Optimizations in Compose
**Learning:** Replacing idiomatic collection operations (`count`, `mapNotNull`, `minOrNull`, `average`) with manual single-pass accumulators inside Composable components reduces code readability and was rejected as an unnecessary micro-optimization.
**Action:** Focus optimizations on true architectural bottlenecks, unneeded recompositions, redundant work on hot streaming paths, or avoid changes if no non-micro bottleneck exists.
