# Failed-auth / anonymous rate-limiting — backlog

Tracks remaining work after the rate-limiting-bypass fix (gate throttling on the
authentication outcome instead of header presence; cluster-aware, failure-aware).

## Status: implementation complete

- Shipped: `ClientIpResolver`, `FailedAuthRateLimitKeys`, `FailedAuthRateLimiter`,
  `AnonymousRateLimiter`, rewritten `RateLimitingInterceptor`, `AuthenticationLoaderFilter`
  failed-auth recording, `ExceptionHandlerFilter` 429 + `Retry-After` mapping, and the
  `FAILED_AUTH_RATE_LIMITING_ENABLED` config (default off) plus per-IP/per-user and
  anonymous per-IP tunables.
- Removed the in-memory resilience4j limiter (`RateLimiterProvider`); throttling now goes
  through the shared Bucket4j store.
- Build green: full reactor `clean install` passes; web-module unit tests pass.

## Open follow-ups

### Rollout
- [ ] Commit the change and open the PR (keep the commit message generic — no customer or
      internal specifics).
- [ ] Enable `FAILED_AUTH_RATE_LIMITING_ENABLED=true` per landscape only after the grace
      period; it defaults off so nothing throttles until operators opt in.
- [ ] Document the new environment variables (the enable flag + per-IP/per-user capacity and
      refill-per-hour, anonymous per-IP capacity and refill-per-hour) for operators.

### Thresholds
- [ ] Coordinate the default capacity/refill values with the related legacy-URL and global
      rate-limit efforts so limits don't conflict; all defaults are conservative and
      overridable via env.

### Hardening / known boundary conditions
- [ ] Trust boundary of the true-client-IP header: keying on it assumes the edge router
      injects/overwrites it. If a caller can spoof it, per-IP buckets can be evaded. Confirm
      the header is stripped from inbound and set only by the trusted edge; document the
      assumption.
- [ ] Decide eviction/TTL behavior for the shared bucket rows under sustained attack volume
      (keys are namespaced and collision-safe; verify cleanup keeps the table bounded).

### Verification once deployed with the flag on
- [ ] Repeated invalid bearer token → throttled (429 + `Retry-After`), shared across instances.
- [ ] Repeated wrong Basic Auth (same IP/user) → throttled; the upstream token exchange is not
      hammered (pre-check short-circuits before the live call).
- [ ] Valid authenticated caller → unaffected.
- [ ] `/public/**` with invalid/absent `Authorization` → still rate-limited.

### Optional test coverage
- [ ] `AnonymousRateLimiterTest` and `AnonymousRateLimitKeysTest` (mirror the failed-auth
      limiter/keys tests) — not yet added.
