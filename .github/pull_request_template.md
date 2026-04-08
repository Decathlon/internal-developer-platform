# PR Description

<!--
Expected Format of the title of the PR:

- type: feat, fix, docs, style, refactor, perf, test, chore, build, ci, revert
- scopes: core, api, service, ui, database, config, security, auth, logging, metrics, deps, deps-dev
- short summary: written in present tense ("change" not "changed" or "changes"), not capitalized, no period at the end
-->

## What this PR Provides

- Describe in short sentences the goal of the PR. Use lists.
- If the PR is linked to an ADR, please provide the link.

## Fixes

<!--
Use this section to reference the related issue (GitHub issue number).

Examples:
- fixes #123
- fixes #123, fixes #456
-->

## Review

<!--
These check boxes should not be checked by the PR author but by the reviewer
has to check them.
Please delete the useless checkboxes :)
-->

The reviewer **must** double-check these points:

- [ ] The reviewer has tested the feature
- [ ] The reviewer has reviewed the implementation of the feature
- [ ] The documentation has been updated
- [ ] The feature implementation respects the Technical Doc / ADR previously produced
- [ ] The Pull Request title has a `!` after the type/scope to identify the breaking
    change in the release note and ensure we will release a major version.

## How to test

<!--
Copy/paste the "test" section of the Jira or Github Issue here
-->

Please **refer** (copy/paste) the test section **from the User Story**. This should include

- The initial state: what should be the status of the system before testing
   (for example, ensure the data xxx exists in idp-back to be able to test the feature)
- What and how to test: steps to perform to test the feature
   (for example, go to page xxx, fill the xxx field and click the 'send' button)
- Expected results: what should be observed for success or failure
   (for example, there is a link in the database between component X and component Y.
   You can retrieve the information with a `GET` request to the API)

## Breaking changes (if any)

<!--
Explain here if you have any breaking change in the UX or in the API contract.
If you don't introduce any breaking change, feel free to remove this section.
-->

- Data loss / modification
- API JSON schema modification (existing resource / behavior)
- Behavior modification of a component
- Others
- N/A

### Context of the Breaking Change

<!--
Explain here more about the breaking change you introduced.
-->

For example: we redefined the component types list in the DPAC referential

### Result of the Breaking Change

<!--
Explain more about the impact of the breaking change
-->

For example: your component of type xxx will migrate to the type yyy
