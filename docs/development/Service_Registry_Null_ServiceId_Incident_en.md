# Service Registry `serviceId` Incident

## Status

This bug fix is under review. The source-level prevention for newly generated production service records is implemented, and the Java 25 test suite passes. Validation of existing registry data and a production-like startup remain open.

## User-visible failure

CAS accepted login requests but failed while looking up the requested application. Candidate service sorting threw a `NullPointerException` from `BaseRegisteredService.compareTo()` because at least one registered service had a null `serviceId`.

The failure occurred in the registered-service data path before normal login processing. Repeated login errors were consequences of the same invalid cached service state.

## Cause

The production generators previously created CAS, OAuth, and OIDC registry JSON without top-level `name` and `serviceId` fields. The records contained `ServiceName` and `Fqdn` custom properties and expected the CAS [`BaseService` template](../../resources/etc/cas/services/templates/BaseService.json) to supply both identity fields later.

That design made every generated record temporarily incomplete:

```text
generated JSON without name/serviceId
  -> custom registry deserializes an incomplete service
  -> CAS template manager is expected to add both fields
  -> service enters the cache
  -> candidate services are sorted for matching
```

Normal template expansion can complete these records. If expansion fails, is bypassed, or a raw registry object reaches the cache directly, CAS sorts a service with a null `serviceId`. The CAS comparator uses natural ordering for that field and is not null-safe.

The historical logs show template application starting without a corresponding successful completion, but the original internal exception was hidden by a load guard that converted `NullPointerException` failures into empty collections. The available evidence establishes the incomplete input and final comparator failure, but not the precise reason template application failed in that deployment.

## Changes already implemented

### Self-contained production records

Both production generators now write top-level `name` and `serviceId` fields:

- [`cas-service-template.json.tpl`](../../resources/etc/cas/config/services/cas-service-template.json.tpl)
- [`oauth-service-template.json.tpl`](../../resources/etc/cas/config/services/oauth-service-template.json.tpl)

The fields use the same source values and matching expression as `BaseService.json`:

```json
"name": "{{SERVICE}}",
"serviceId": "^https://((?i){{FQDN}})(:443)?/{{SERVICE}}(/.*)?"
```

Newly generated records are therefore valid immediately after deserialization. CAS can still apply its internal templates during normal manager loading, but matching correctness no longer depends on that later step for these required fields.

### Failure visibility

The load aspect now normalizes only null return values. Exceptions from `ServicesManager.load()` and `ServiceRegistry.load()` propagate with their original stack instead of being converted into an empty service collection.

Template discovery logs the template directory and discovered JSON files. A template application failure logs the affected service, requested template, discovered template files, and original exception before rethrowing that exception.

### Related null handling

`CesLegacyCompatibleTemplatesManager` now treats a null service-property map as empty. This prevents a secondary dereference failure in its fallback logic. It is defensive hardening and is not the root fix for the comparator incident.

## Test coverage

`RegisteredServiceTemplateIntegrationTests` renders CAS, OAuth, and OIDC records from the production generators and uses the real JSON serializer, custom registry, template manager, and CAS services manager.

The integration coverage verifies that:

- normal services-manager loading returns records with nonblank `name` and `serviceId`;
- direct custom-registry loading also returns generated records with those fields;
- putting direct registry results into the CAS cache does not break public service matching.

The focused comparator regression tests deliberately construct incomplete services and expect the historical `NullPointerException`. Those tests pass when the failure is reproduced; they document why incomplete objects must not enter matching and should not be interpreted as proof that the application path is safe.

Tests also cover template discovery logging, preservation of serializer failures, null property maps, and propagation of load failures. The complete Gradle test suite passes with JDK 25 after the generator change.

## Remaining review work

The generator change affects records created after the updated templates are deployed. Existing production JSON created by an older generator may still lack top-level `name` and `serviceId`. The upgrade path must establish whether those records are regenerated automatically. If they are retained, they need a migration or an explicit regeneration step.

The custom registry does not currently reject arbitrary incomplete JSON. Hand-written, stale, or malformed records can still deserialize with a missing identity field. A follow-up hardening change should validate `name` and `serviceId` before adding a service to the registry map and fail startup with a message containing the source file, service ID, class, and template name.

Before closing the bug, review should confirm:

1. Existing installations receive valid registry records after upgrade.
2. A production-like CAS startup loads all generated services successfully.
3. CAS, OAuth, and OIDC login matching works with the resulting registry.
4. The first template or registry failure remains visible with its original stack.

## Related documentation

- [Service Registry](Service_Registry_en.md)
