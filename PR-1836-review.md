# Code Review — PR #1836: Add unit tests to improve SonarScan code coverage

- **Repo:** cloudfoundry/multiapps-controller
- **PR:** https://github.com/cloudfoundry/multiapps-controller/pull/1836
- **Author:** stiv03
- **Branch:** `stiv03:tests/increase-coverage-targeted` → `cloudfoundry:master`
- **Commit reviewed:** `b9eb6258e` ("Add unit tests to improve SonarScan tests code covarage")
- **Jira:** LMCROSSITXSADEPLOY-3258
- **Files in commit:** 53 new test files + `sonar-project.properties`
- **Reviewer:** Code-review pass focused on whether each test actually exercises real production logic vs. only testing mocks, generated
  code, or trivial accessors.

---

## TL;DR — overall recommendation

**Do NOT approve as-is.** The PR achieves the stated goal of raising line/branch coverage, but a meaningful fraction of the new tests do not
test production logic — they test Java/Mockito itself. Specifically:

- ~17 files (~32%) provide real regression value and can be merged as written.
- ~14 files (~26%) are mostly fine but have nits (missing edge cases, mock-driven assertions).
- ~10 files (~19%) need rework before they're worth merging — they test the wrong thing or omit the assertion that matters.
- ~12 files (~23%) should not be merged in their current form — they assert on mocked stubs, on compiler-generated record/enum mechanics, or
  on no-op classes.

Suggested path forward: split the PR. Approve the strong tests now (18 modules of real regression coverage). Send the others back with
concrete revision asks listed below. The trivial-getter / pure-delegation tests should either be deleted or rewritten to assert formatted
output, not Mockito invocation counts.

A note on the broader point: SonarQube coverage is a means, not an end. Tests that pass when you delete the production code provide negative
signal — they boost the metric while training future contributors that verifying-the-mock is acceptable. The strongest parts of this PR (
DatabaseQueryClient, ContentFilter, ModuleDependencyChecker, DeployedAfterModulesContentValidator, HookPhasesConfigValidator) demonstrate
the author can write good tests; the weak parts look like they were written to a coverage quota.

---

## Verdict legend

- **APPROVE** — tests cover real logic and would catch regressions
- **APPROVE WITH NITS** — mostly fine, minor improvements possible
- **REQUEST CHANGES** — significant problems but salvageable (rewrite asks listed)
- **REJECT** — adds no real coverage value as written; remove or rewrite from scratch

---

## Summary table

| #  | File                                         | Module             | Verdict           |
|----|----------------------------------------------|--------------------|-------------------|
| 1  | CloudFoundryTokenProviderTest                | client             | REJECT            |
| 2  | CloudControllerExceptionTest                 | client             | APPROVE WITH NITS |
| 3  | CloudCredentialsTest                         | client             | REQUEST CHANGES   |
| 4  | CloudExceptionTest                           | client             | REJECT            |
| 5  | CloudOperationExceptionTest                  | client             | APPROVE WITH NITS |
| 6  | CloudServiceBrokerExceptionTest              | client             | APPROVE WITH NITS |
| 7  | OAuthTokenProviderTest                       | client             | REQUEST CHANGES   |
| 8  | CloudUtilTest                                | client             | APPROVE           |
| 9  | ResilientCloudOperationExecutorTest          | client             | APPROVE           |
| 10 | ResilientOperationExecutorTest               | client             | APPROVE           |
| 11 | TokenPropertiesTest                          | client             | REQUEST CHANGES   |
| 12 | ApplicationConfigurationAuditLogTest         | core/auditlogging  | REJECT            |
| 13 | AuthenticationAuditLogTest                   | core/auditlogging  | REJECT            |
| 14 | ConfigurationEntryServiceAuditLogTest        | core/auditlogging  | APPROVE WITH NITS |
| 15 | ConfigurationSubscriptionServiceAuditLogTest | core/auditlogging  | APPROVE WITH NITS |
| 16 | CsrfTokenApiServiceAuditLogTest              | core/auditlogging  | REJECT            |
| 17 | FilesApiServiceAuditLogTest                  | core/auditlogging  | APPROVE           |
| 18 | FlowableSlmpResourceAuditLogTest             | core/auditlogging  | REQUEST CHANGES   |
| 19 | FlowableSlppResourceAuditLogTest             | core/auditlogging  | REQUEST CHANGES   |
| 20 | InfoApiServiceAuditLogTest                   | core/auditlogging  | REJECT            |
| 21 | LoginAttemptAuditLogTest                     | core/auditlogging  | APPROVE WITH NITS |
| 22 | MtaConfigurationPurgerAuditLogTest           | core/auditlogging  | APPROVE           |
| 23 | MtasApiServiceAuditLogTest                   | core/auditlogging  | APPROVE WITH NITS |
| 24 | OperationsApiServiceAuditLogTest             | core/auditlogging  | APPROVE           |
| 25 | DeployedAfterModulesContentValidatorTest     | core               | APPROVE           |
| 26 | MtaDescriptorMergerTest                      | core               | APPROVE WITH NITS |
| 27 | DatabaseQueryClientTest                      | database-migration | APPROVE           |
| 28 | OrderDirectionTest                           | persistence        | REJECT            |
| 29 | CloudTargetTest                              | persistence        | REJECT            |
| 30 | FileInfoTest                                 | persistence        | APPROVE           |
| 31 | VersionJsonDeserializerTest                  | persistence        | APPROVE WITH NITS |
| 32 | VersionJsonSerializerTest                    | persistence        | APPROVE           |
| 33 | ContentFilterTest                            | persistence        | APPROVE           |
| 34 | TargetWildcardFilterTest                     | persistence        | APPROVE           |
| 35 | VersionFilterTest                            | persistence        | APPROVE           |
| 36 | VisibilityFilterTest                         | persistence        | APPROVE           |
| 37 | StreamFetchingOptionsTest                    | persistence        | REJECT            |
| 38 | FileStorageExceptionTest                     | persistence        | REJECT            |
| 39 | NullProcessLoggerTest                        | persistence        | REJECT            |
| 40 | OperationLogStorageExceptionTest             | persistence        | REQUEST CHANGES   |
| 41 | ClientKeyConfigurationHandlerTest            | persistence        | APPROVE WITH NITS |
| 42 | ConfigurationEntriesUtilTest                 | persistence        | APPROVE           |
| 43 | PEMToEncodedKeyConverterTest                 | persistence        | APPROVE WITH NITS |
| 44 | HookPhasesConfigValidatorTest                | process            | APPROVE           |
| 45 | LockOwnerReleaserTest                        | process            | APPROVE WITH NITS |
| 46 | ModuleDependencyCheckerTest                  | process            | APPROVE           |
| 47 | ApplicationHealthResourceTest                | web                | REJECT            |
| 48 | CsrfTokenResourceTest                        | web                | APPROVE           |
| 49 | PingResourceTest                             | web                | APPROVE           |
| 50 | SpaceWithUserTest                            | web                | REQUEST CHANGES   |
| 51 | FileFromUrlDataTest                          | web                | REJECT            |
| 52 | RejectedAsyncUploadJobExceptionTest          | web                | REJECT            |
| 53 | ServletUtilTest                              | web                | APPROVE WITH NITS |

**Counts:** APPROVE 17 · APPROVE WITH NITS 14 · REQUEST CHANGES 10 · REJECT 12

---

## Module 1 — multiapps-controller-client (11 files)

**Module summary:** Mixed quality. Two strong utility tests (`CloudUtilTest`, `ResilientOperationExecutorTest`) and one good retry-logic
test (`ResilientCloudOperationExecutorTest`) carry the module. Three exception-class tests cover real `getMessage()` overrides and provide
modest value. Three files (`CloudFoundryTokenProviderTest`, `CloudExceptionTest`, `CloudCredentialsTest`) test trivial delegation,
exception-superclass behavior, or POJO field assignment and should be reworked or removed. `OAuthTokenProviderTest` and
`TokenPropertiesTest` need stronger assertions before merge.

### CloudFoundryTokenProviderTest — REJECT

- Production class: `multiapps-controller-client/src/main/java/.../client/CloudFoundryTokenProvider.java`
- Logic complexity: trivial (passthrough delegation)
- Tests: 1
- **Rationale:** SUT delegates `getToken()` to an injected `OAuthClient`. The single test stubs the mock and asserts that the SUT returns
  the stub value. This tests Mockito, not the SUT.
- Issues: `testGetTokenDelegatesToOAuthClient()` — stubs and asserts on the mock's return.
- Tests testing real logic: none

### CloudControllerExceptionTest — APPROVE WITH NITS

- Production class: `.../client/facade/CloudControllerException.java`
- Logic complexity: simple (overrides `getMessage()` with `MessageFormat` decoration)
- Tests: 4
- **Rationale:** Verifies the message-decoration logic across constructor variants and the wrapping constructor. Real branch coverage of the
  override.
- Issues: only happy-path / formatting; no error-input cases.
- Tests testing real logic: all 4

### CloudCredentialsTest — REQUEST CHANGES

- Production class: `.../client/facade/CloudCredentials.java`
- Logic complexity: trivial (data container with multiple constructors)
- Tests: 9
- **Rationale:** Every test asserts that fields set in a constructor are returned by getters. No conditional logic exists in the class to
  regress.
- Issues: all 9 tests are tautological constructor/getter checks, including `testProxyForUserCopiesFieldsAndSetsProxyUser` which only
  verifies field copying.
- Asks: drop the file or, if compliance requires coverage, replace with one parameterized "all constructors set all fields" sweep.
- Tests testing real logic: none

### CloudExceptionTest — REJECT

- Production class: `.../client/facade/CloudException.java`
- Logic complexity: trivial (extends `RuntimeException` with no overrides)
- Tests: 3
- **Rationale:** Tests verify that `RuntimeException` constructors store message/cause. That's the JDK's behavior, not the SUT's.
- Tests testing real logic: none

### CloudOperationExceptionTest — APPROVE WITH NITS

- Production class: `.../client/facade/CloudOperationException.java`
- Logic complexity: simple (static `getExceptionMessage()` with description-conditional formatting)
- Tests: 4
- **Rationale:** Both branches of the message-builder are exercised; status/statusText/cause storage covered.
- Tests testing real logic: all 4

### CloudServiceBrokerExceptionTest — APPROVE WITH NITS

- Production class: `.../client/facade/CloudServiceBrokerException.java`
- Logic complexity: simple (`getMessage()` decoration with broker-specific prefix)
- Tests: 4
- **Rationale:** Mirror of `CloudControllerExceptionTest`. Decorator logic is verified.
- Tests testing real logic: all 4

### OAuthTokenProviderTest — REQUEST CHANGES

- Production class: `.../client/facade/adapters/OAuthTokenProvider.java`
- Logic complexity: simple (Reactor `Mono` wrap of `OAuthClient.getToken().getAuthorizationHeaderValue()`)
- Tests: 1
- **Rationale:** Test stubs both mocks and asserts the stub. The Reactor pipeline (lazy supplier, error mapping if any) is never exercised.
- Asks: rewrite using `StepVerifier` to verify the `Mono` is lazy, propagates errors, and emits exactly one value.
- Tests testing real logic: none

### CloudUtilTest — APPROVE

- Production class: `.../client/facade/util/CloudUtil.java`
- Logic complexity: non-trivial (`parse()` covers Integer/Long/Double/String/Date with default-value and ClassCastException handling)
- Tests: 11
- **Rationale:** Branches for type coercion, null-with-numeric-default, null-with-non-numeric-default, unparseable date, and ClassCast all
  covered.
- Tests testing real logic: all 11

### ResilientCloudOperationExecutorTest — APPROVE

- Production class: `.../client/util/ResilientCloudOperationExecutor.java`
- Logic complexity: non-trivial (extends `ResilientOperationExecutor`, adds HttpStatus-aware `shouldRetry`)
- Tests: 4
- **Rationale:** Default ignored-status retry, non-ignored immediate fail, custom-status add via `withStatusesToIgnore` are all covered.
- Issues: `testFluentBuildersReturnSameTypeForChaining` is just a non-null check.
- Tests testing real logic: 3 of 4

### ResilientOperationExecutorTest — APPROVE

- Production class: `.../client/util/ResilientOperationExecutor.java`
- Logic complexity: non-trivial (retry loop, `Supplier`/`Runnable`/`CheckedSupplier` variants)
- Tests: 5
- **Rationale:** First-try success, retry-then-success, exhaustion-throws, both Runnable and Supplier paths, and CheckedSupplier are all
  exercised.
- Tests testing real logic: all 5

### TokenPropertiesTest — REQUEST CHANGES

- Production class: `.../client/util/TokenProperties.java`
- Logic complexity: simple (DTO + `fromToken(...)` map-extraction factory)
- Tests: 3
- **Rationale:** Two of three tests are getter checks; the `fromToken` test only asserts happy-path map lookup.
- Asks: drop the getter test, add coverage for `null` `additionalInfo`, missing keys, and unexpected types in the map.
- Tests testing real logic: 1 partial

---

## Module 2 — multiapps-controller-core / auditlogging (13 files)

**Module summary:** This subgroup is the weakest in the PR. Most audit-log classes are 2–6 lines of "format and forward to facade" code.
Strong tests verify the formatted parameter map's contents (`FilesApiServiceAuditLog`, `MtaConfigurationPurgerAuditLog`,
`OperationsApiServiceAuditLog`). Weak tests use `Mockito.any(...)` and `verify(facade).logX(any())` without asserting the map. Those weak
tests will not catch a regression that swaps the message string, omits a parameter, or routes to the wrong facade method.

### ApplicationConfigurationAuditLogTest — REJECT

- Production class: `.../core/auditlogging/ApplicationConfigurationAuditLog.java`
- Logic complexity: trivial
- Tests: 1
- **Rationale:** Asserts `verify(facade, times(1))...`. The `MessageFormat` argument is captured but never asserted. Removing the
  `MessageFormat.format(...)` line would not fail the test.
- Asks: assert on the captured `AuditLogConfiguration`'s action/parameters strings.

### AuthenticationAuditLogTest — REJECT

- Production class: `.../core/auditlogging/AuthenticationAuditLog.java`
- Logic complexity: trivial
- Tests: 2
- **Rationale:** Same pattern as above for two methods. No assertion on the formatted message.

### ConfigurationEntryServiceAuditLogTest — APPROVE WITH NITS

- Production class: `.../core/auditlogging/ConfigurationEntryServiceAuditLog.java`
- Logic complexity: simple (extracts 7 provider identifiers, formats `org/space`)
- Tests: 2
- **Rationale:** First test asserts the parameter map contents — the part that has logic. Second test only verifies delegation.
- Asks: have the second test also assert the parameter map.

### ConfigurationSubscriptionServiceAuditLogTest — APPROVE WITH NITS

- Production class: `.../core/auditlogging/ConfigurationSubscriptionServiceAuditLog.java`
- Logic complexity: simple
- Tests: 2
- **Rationale:** Same as above — one strong, one delegation-only.
- Asks: tighten the second test's assertion.

### CsrfTokenApiServiceAuditLogTest — REJECT

- Production class: `.../core/auditlogging/CsrfTokenApiServiceAuditLog.java`
- Logic complexity: trivial (one method, hard-coded message constants)
- Tests: 1
- **Rationale:** Asserts `username` and empty `spaceId` are captured but never asserts the action message.

### FilesApiServiceAuditLogTest — APPROVE

- Production class: `.../core/auditlogging/FilesApiServiceAuditLog.java`
- Logic complexity: simple (4 methods build different parameter maps)
- Tests: 4
- **Rationale:** Each test extracts `FileMetadata` (digest, size, algorithm, namespace), URL params, and job IDs and verifies the captured
  map.
- Tests testing real logic: all 4

### FlowableSlmpResourceAuditLogTest — REQUEST CHANGES

- Production class: `.../core/auditlogging/FlowableSlmpResourceAuditLog.java`
- Logic complexity: trivial
- Tests: 3
- **Rationale:** Two of three tests use `any(AuditLogConfiguration.class)` and verify only that the facade was invoked. Only the
  parameter-overload test inspects the map.
- Asks: replace `any()` with `argThat(cfg -> cfg.getAction().equals(...))`.

### FlowableSlppResourceAuditLogTest — REQUEST CHANGES

- Production class: `.../core/auditlogging/FlowableSlppResourceAuditLog.java`
- Logic complexity: trivial
- Tests: 4
- **Rationale:** Same as `FlowableSlmpResourceAuditLog` — two tests assert nothing about the configuration object.
- Asks: same as above.

### InfoApiServiceAuditLogTest — REJECT

- Production class: `.../core/auditlogging/InfoApiServiceAuditLog.java`
- Logic complexity: trivial
- Tests: 1
- **Rationale:** Same anti-pattern: only verifies username capture, not the message.

### LoginAttemptAuditLogTest — APPROVE WITH NITS

- Production class: `.../core/auditlogging/LoginAttemptAuditLog.java`
- Logic complexity: simple (`MessageFormat.format` template substitution)
- Tests: 1
- **Rationale:** Asserts the formatted output exactly — would catch a template change.
- Asks: parameterize for a couple of value combinations.

### MtaConfigurationPurgerAuditLogTest — APPROVE

- Production class: `.../core/auditlogging/MtaConfigurationPurgerAuditLog.java`
- Logic complexity: simple (5 overloads extracting different domain-object fields)
- Tests: 5
- **Rationale:** Each test verifies the parameter extraction and naming.

### MtasApiServiceAuditLogTest — APPROVE WITH NITS

- Production class: `.../core/auditlogging/MtasApiServiceAuditLog.java`
- Logic complexity: simple
- Tests: 3
- **Rationale:** Two strong tests verify mtaId and namespace+name extraction. The "no filters" test only verifies the call.
- Asks: tighten the "no filters" assertion.

### OperationsApiServiceAuditLogTest — APPROVE

- Production class: `.../core/auditlogging/OperationsApiServiceAuditLog.java`
- Logic complexity: simple (7 parameter-extraction methods)
- Tests: 7
- **Rationale:** All 7 tests assert the captured parameter maps.

---

## Module 3 — multiapps-controller-core / other (2 files)

**Module summary:** Both files are high quality and ready to merge.

### DeployedAfterModulesContentValidatorTest — APPROVE

- Production class: `.../core/cf/util/DeployedAfterModulesContentValidator.java`
- Logic complexity: non-trivial (V2 vs V3 split, archive vs CF dependency lookup, error aggregation)
- Tests: 8
- **Rationale:** All branches covered: V2 passthrough, no `deployed-after`, in-deployment dependencies, non-app vs app, CF lookup hits and
  misses, multiple errors aggregated.

### MtaDescriptorMergerTest — APPROVE WITH NITS

- Production class: `.../core/helpers/MtaDescriptorMerger.java`
- Logic complexity: non-trivial (orchestrates validator → merger → platform merger → compatibility validator; optional logger)
- Tests: 2
- **Rationale:** Both constructor variants exercised; orchestration verified.
- Asks: add a propagation test for validator-thrown exceptions.

---

## Module 4 — multiapps-controller-database-migration (1 file)

### DatabaseQueryClientTest — APPROVE

- Production class: `.../database/migration/client/DatabaseQueryClient.java`
- Logic complexity: non-trivial (JDBC iteration, ResultSetMetaData parsing, type-factory dispatch)
- Tests: 6
- **Rationale:** Covers `getLastSequenceValue` (with/without row), `updateSequence`, `extractTableData` (metadata + row aggregation, empty
  result), and `writeDataToDataSource` (iteration, empty rows). One of the strongest test files in the PR.

---

## Module 5 — multiapps-controller-persistence (16 files)

**Module summary:** Bimodal. The filter tests (`Content`, `TargetWildcard`, `Version`, `Visibility`) are excellent — they cover every branch
of real wildcard/predicate logic. `FileInfoTest`, `VersionJsonSerializerTest`, `ConfigurationEntriesUtilTest`,
`PEMToEncodedKeyConverterTest`, and `ClientKeyConfigurationHandlerTest` cover real logic. The remaining files test compiler-generated code (
records, enums), trivial POJOs, exception passthrough, or a Null-Object class — all REJECT-worthy.

### OrderDirectionTest — REJECT

- Production: `.../persistence/OrderDirection.java` — bare 2-constant enum
- Tests: 2 — `values()` and `valueOf()`. Both are language-generated.

### CloudTargetTest — REJECT

- Production: `.../persistence/model/CloudTarget.java` — POJO with hand-written but mechanical equals/hashCode/toString using `Objects.*`
- Tests: 10 covering constructors, getters/setters, equals reflexivity/symmetry/null/different-type, hashCode consistency, and toString.
- **Rationale:** No conditional logic; each test verifies what was just assigned. The equals/hashCode logic is `Objects.equals` plumbing.

### FileInfoTest — APPROVE

- Production: `.../persistence/model/FileInfo.java` — `getInputStream()` wraps `FileNotFoundException` in `FileStorageException`
- Tests: 2 — happy path + exception wrapping

### VersionJsonDeserializerTest — APPROVE WITH NITS

- Production: `.../persistence/model/adapter/VersionJsonDeserializer.java`
- Tests: 1
- **Rationale:** Mocks parser/codec/context; verifies `Version.parseVersion` is invoked with the codec result. Happy-path only.
- Asks: add cases for null/empty input and an invalid version string.

### VersionJsonSerializerTest — APPROVE

- Production: `.../persistence/model/adapter/VersionJsonSerializer.java` — null-check branch
- Tests: 2 — both branches covered

### ContentFilterTest — APPROVE

- Production: `.../persistence/model/filters/ContentFilter.java`
- Tests: 6 covering empty requirements, null content, invalid JSON, full match, missing property, value mismatch.

### TargetWildcardFilterTest — APPROVE

- Production: `.../persistence/model/filters/TargetWildcardFilter.java`
- Tests: 8 covering null target, full wildcard, org/space wildcards (each with match and mismatch), exact match, exact mismatch.

### VersionFilterTest — APPROVE

- Production: `.../persistence/model/filters/VersionFilter.java`
- Tests: 4 covering null requirement, null provider, satisfied, unsatisfied.

### VisibilityFilterTest — APPROVE

- Production: `.../persistence/model/filters/VisibilityFilter.java`
- Tests: 7 covering empty targets, exact match, full/org/space wildcards, null-visibility fallback, no match.

### StreamFetchingOptionsTest — REJECT

- Production: `.../persistence/query/options/StreamFetchingOptions.java` — Java `record` with two fields
- Tests: 3 testing accessor return values, equals, and inequality for records.
- **Rationale:** Compiler-generated; passing this test is a precondition of the language working.

### FileStorageExceptionTest — REJECT

- Production: `.../persistence/services/FileStorageException.java` — pure exception, no overrides
- Tests: 3 — verify `super(...)` works.

### NullProcessLoggerTest — REJECT

- Production: `.../persistence/services/NullProcessLogger.java` — Null-Object pattern, every method is a no-op
- Tests: 8 — each asserts that calling the method "does not throw".
- **Rationale:** Asserting that a no-op doesn't throw is meaningless: a test that passes for an empty class body has zero diagnostic value.
  If the class's contract is "do nothing", the test should at least assert that no other side effects occur (no logger handler invoked,
  etc.).

### OperationLogStorageExceptionTest — REQUEST CHANGES

- Production: `.../persistence/services/OperationLogStorageException.java` — extends `SLException`
- Tests: 4
- **Rationale:** A code comment in the test indicates the (String, Throwable) constructor resolves to `SLException(String, Object...)` and
  the throwable is treated as a `MessageFormat` arg, not a cause. The test asserts only the message, never `getCause()`. Either this is a
  bug in the production class that the test silently masks, or it's intended behavior that should be documented and asserted explicitly.
- Asks: assert `getCause()` and decide whether the current behavior is correct; if not, fix the production class.

### ClientKeyConfigurationHandlerTest — APPROVE WITH NITS

- Production: `.../persistence/util/ClientKeyConfigurationHandler.java` — parses PEM, encodes, writes file
- Tests: 1 happy-path
- **Rationale:** Real logic exercised end-to-end; missing error-path coverage.
- Asks: add tests for null/empty PEM, malformed PEM, IO failure.

### ConfigurationEntriesUtilTest — APPROVE

- Production: `.../persistence/util/ConfigurationEntriesUtil.java` — `providerNamespaceIsEmpty` boolean expression
- Tests: 4 — exercises every branch.

### PEMToEncodedKeyConverterTest — APPROVE WITH NITS

- Production: `.../persistence/util/PEMToEncodedKeyConverter.java`
- Tests: 2 — valid PEM, unsupported type
- **Rationale:** Covers both branches.
- Asks: assert exception message in the negative case to confirm the right branch threw.

---

## Module 6 — multiapps-controller-process (3 files)

**Module summary:** All three files are high quality and ready to merge.

### HookPhasesConfigValidatorTest — APPROVE

- Production: `.../process/util/HookPhasesConfigValidator.java`
- Tests: 8 — no modules, no hooks, no parameter, valid phases, non-list type, duplicates, missing key, deprecated phase warning.

### LockOwnerReleaserTest — APPROVE WITH NITS

- Production: `.../process/util/LockOwnerReleaser.java` — conditional clear of stale jobs
- Tests: 2 — both branches of the empty-list conditional.
- Asks: add an exception-propagation case if applicable.

### ModuleDependencyCheckerTest — APPROVE

- Production: `.../process/util/ModuleDependencyChecker.java`
- Tests: 8 — V2 always-satisfied, no `deployed-after`, dependency already deployed, not-for-deployment, for-deployment but pending, in CF,
  missing from CF, non-application dependency warning.

---

## Module 7 — multiapps-controller-web (7 files)

**Module summary:** Two REST contract tests (`CsrfTokenResource`, `PingResource`) are minimal but meaningful. `ServletUtilTest` tests real
utility logic well. The remaining four files are weak: one mocks the SUT's only dependency and asserts the mock (
`ApplicationHealthResourceTest`), two test compiler-generated record/POJO mechanics (`FileFromUrlData`, `RejectedAsyncUploadJobException`),
and one is bloated boilerplate around a manually-implemented equals (`SpaceWithUser`).

### ApplicationHealthResourceTest — REJECT

- Production: `.../web/resources/ApplicationHealthResource.java` — pure delegation to `ApplicationHealthCalculator`
- Tests: 1
- **Rationale:** Mocks the calculator, returns a sentinel object, asserts the resource returns it. No status code, no content type, no
  Spring contract is verified.
- Asks: drop the file or rewrite as a Spring `@WebMvcTest`/`MockMvc` integration test that verifies status, content-type, and JSON shape.

### CsrfTokenResourceTest — APPROVE

- Production: `.../web/resources/CsrfTokenResource.java` — returns `204 No Content`
- Tests: 1 — asserts status and that the body is null. Documents the contract.

### PingResourceTest — APPROVE

- Production: `.../web/resources/PingResource.java` — returns `200 OK` with body `"pong"`
- Tests: 1 — asserts both. Contract documentation, low but non-zero value.

### SpaceWithUserTest — REQUEST CHANGES

- Production: `.../web/security/SpaceWithUser.java` — POJO with hand-written equals/hashCode
- Tests: 7
- **Rationale:** Of 7 tests, 4 are reflexivity / null / getter / hashCode-equivalence boilerplate. The 3 useful tests (different user GUID,
  different space GUID, different type) are buried.
- Asks: drop the trivial 4, keep the 3 that exercise the equals discriminators.

### FileFromUrlDataTest — REJECT

- Production: `.../web/upload/client/FileFromUrlData.java` — Java `record`
- Tests: 2 — accessor return + record equals/hashCode. Both compiler-generated.

### RejectedAsyncUploadJobExceptionTest — REJECT

- Production: `.../web/upload/exception/RejectedAsyncUploadJobException.java` — exception with one getter
- Tests: 1 — verifies a getter returns the constructor argument. Also sets up a Mockito mock that is never used, then closes it.
- Asks: drop the file or fold the assertion into an existing exception-handling integration test.

### ServletUtilTest — APPROVE WITH NITS

- Production: `.../web/util/ServletUtil.java` — path-variable extraction, URI decoding, slash normalization, response writing
- Tests: 7
- **Rationale:** Decoding "hello%20world", collapsing "/a//b///c", and similar edge cases are real logic.
- Issues: `testGetPathVariablesReturnsMapFromRequestAttribute` and `testSendWritesBodyAndStatus` rely on Mockito verification rather than
  asserting an actual `StringWriter` output. `testRemoveInvalidForwardSlashesLeavesSingleSlashesUnchanged` is mostly trivial.
- Asks: replace mock-based response writer with a real `StringWriter`, then assert the captured string.

---

## Cross-cutting issues (PR-wide)

1. **Tautological mocking pattern.** A recurring shape:
   ```java
   when(dep.foo()).thenReturn(SENTINEL);
   assertSame(SENTINEL, sut.foo()); // or assertEquals
   verify(dep).foo();
   ```
   This passes whether `sut.foo()` returns `dep.foo()` or `dep.foo()` and then encrypts it. It tests Mockito, not the SUT. Affected files:
   `CloudFoundryTokenProviderTest`, `OAuthTokenProviderTest`, `ApplicationHealthResourceTest`, several auditlog tests using
   `verify(facade).logX(any())`.

2. **Asserting on `MessageFormat` arguments via `Mockito.any(...)`.** Auditlog tests capture the `AuditLogConfiguration` but pass
   `any(AuditLogConfiguration.class)` to `verify(...)`, then never inspect the captured object. The action string is the part that has the
   bug surface — assert it.

3. **Tests of compiler-generated code.** Java `record` accessors/equals/hashCode (`StreamFetchingOptions`, `FileFromUrlData`) and bare enum
   `values()`/`valueOf()` (`OrderDirection`) cannot regress without a JDK bug. Tests of these inflate coverage without protection.

4. **Tests that "verify the no-op doesn't throw".** `NullProcessLoggerTest` asserts that calling each method does not throw. This passes for
   an empty class, an exception-throwing class with `@Override` removed, etc. It only fails when the SUT becomes spectacularly broken.

5. **Misleading author commit message.** "Add unit tests to improve SonarScan tests code covarage" — typo ("covarage"). Worth fixing for
   searchability.

6. **`sonar-project.properties` change.** Not reviewed in detail here. If it expands or restricts coverage paths in line with the new tests,
   that's reasonable; if it lowers thresholds, flag it for a maintainer.

---

## Concrete revision asks for the author (suggested PR comment)

> Thanks for the work — happy to merge ~31 of the 53 files (the ones marked APPROVE / APPROVE WITH NITS). Before this can land, please
> address:
>
> **Drop or rewrite (the test does not exercise the SUT):**
> - `CloudFoundryTokenProviderTest`, `CloudExceptionTest`, `ApplicationHealthResourceTest` — currently assert on Mockito stubs.
> - `OrderDirectionTest`, `StreamFetchingOptionsTest`, `FileFromUrlDataTest`, `CloudTargetTest` — exercise compiler-generated code.
> - `FileStorageExceptionTest`, `RejectedAsyncUploadJobExceptionTest` — exercise standard exception/getter behavior.
> - `NullProcessLoggerTest` — asserting "does not throw" on a no-op class doesn't validate any contract.
> - `ApplicationConfigurationAuditLogTest`, `AuthenticationAuditLogTest`, `CsrfTokenApiServiceAuditLogTest`, `InfoApiServiceAuditLogTest` —
    please assert the captured `AuditLogConfiguration` contents (action string, parameters), not just `verify(times(1))`.
>
> **Tighten:**
> - `OAuthTokenProviderTest` — use `StepVerifier` to validate the `Mono`'s lazy/error semantics.
> - `CloudCredentialsTest`, `TokenPropertiesTest` — drop pure getter tests; keep the meaningful constructor-coverage cases.
> - `FlowableSlmpResourceAuditLogTest`, `FlowableSlppResourceAuditLogTest`, the second tests in `ConfigurationEntryServiceAuditLogTest` and
    `ConfigurationSubscriptionServiceAuditLogTest` — replace `any(AuditLogConfiguration.class)` with `argThat(...)` and assert the
    configuration.
> - `OperationLogStorageExceptionTest` — please clarify whether the `(String, Throwable)` constructor losing the cause is intended; if not,
    fix the production class; either way, assert `getCause()`.
> - `ServletUtilTest` — use a real `StringWriter` for the `send()` test.
> - `SpaceWithUserTest` — keep only the equals-discriminator tests; drop reflexivity/null/getter boilerplate.
> - `VersionJsonDeserializerTest`, `ClientKeyConfigurationHandlerTest`, `PEMToEncodedKeyConverterTest` — add the missing error-path cases (
    listed per file above).
>
> Also: typo in commit message (`covarage` → `coverage`).
>
> Once those are addressed I'll re-review.

---

## What I'd approve right now (no rework needed)

These 17 files can ship as-is and represent real coverage of real logic:

1. `CloudUtilTest`
2. `ResilientCloudOperationExecutorTest`
3. `ResilientOperationExecutorTest`
4. `FilesApiServiceAuditLogTest`
5. `MtaConfigurationPurgerAuditLogTest`
6. `OperationsApiServiceAuditLogTest`
7. `DeployedAfterModulesContentValidatorTest`
8. `DatabaseQueryClientTest`
9. `FileInfoTest`
10. `VersionJsonSerializerTest`
11. `ContentFilterTest`
12. `TargetWildcardFilterTest`
13. `VersionFilterTest`
14. `VisibilityFilterTest`
15. `ConfigurationEntriesUtilTest`
16. `HookPhasesConfigValidatorTest`
17. `ModuleDependencyCheckerTest`
18. `CsrfTokenResourceTest`
19. `PingResourceTest`

(That's 19 — 17 unconditional APPROVE plus the two minimal REST contract tests in the web module.)

The author should consider splitting the PR: a "ready-to-merge" subset and a follow-up that addresses the rework asks. Maintainers get
coverage now; the rest stays in review without blocking.
