## <span style="color:hsl(80,68%,32%)">Summary</span>

<!-- What does this PR change and why? -->

## <span style="color:hsl(170,68%,36%)">Changes</span>

-

## <span style="color:hsl(260,68%,44%)">Testing</span>

- [ ] `./mvnw verify` passes locally (unit tests + JaCoCo report)
- [ ] Added/updated tests for the change
- [ ] Manually verified (describe how, if applicable)

## <span style="color:hsl(350,68%,44%)">Checklist</span>

- [ ] Validation is declarative; no business/validation logic added to controllers
- [ ] New config is bound via `@ConfigurationProperties` and documented in `application.yml`
- [ ] External calls degrade gracefully (timeouts/retries/guards)
- [ ] Docs updated (`README.md`) if behaviour/endpoints/config changed
- [ ] No secrets committed
