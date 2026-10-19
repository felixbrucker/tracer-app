## 2026-03-31 - Pre-compile Regex Patterns in High-Frequency Parsing Streams
**Learning:** Instantiating `.toRegex()` inside functions that process stdout lines during active Ping, Traceroute, and MTR streams creates excessive Regex allocations and compilation overhead on every single output line.
**Action:** Extract Regex pattern instances to a static `companion object` so they are compiled once during application lifecycle.
