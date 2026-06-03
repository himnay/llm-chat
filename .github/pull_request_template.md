## Summary

<!-- What does this PR change and why? -->

## Changes

-

## Testing

- [ ] `./mvnw verify` passes locally (unit tests + JaCoCo report)
- [ ] Added/updated tests for the change
- [ ] Manually verified (describe how, if applicable)

## Checklist

- [ ] Validation is declarative; no business/validation logic added to controllers
- [ ] New config is bound via `@ConfigurationProperties` and documented in `application.yml`
- [ ] External calls degrade gracefully (timeouts/retries/guards)
- [ ] Docs updated (`README.md`) if behaviour/endpoints/config changed
- [ ] No secrets committed
