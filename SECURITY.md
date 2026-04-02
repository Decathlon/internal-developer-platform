# Security Policy

## Reporting a Vulnerability

The IDP-Core team takes security vulnerabilities seriously. We appreciate your efforts to responsibly disclose your findings and will make every effort to acknowledge your contributions.

### How to Report a Security Vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report them through one of the following channels:

#### Option 1: GitHub Security Advisories (Recommended)

Report vulnerabilities privately using GitHub Security Advisories:

1. Go to the [Security tab](https://github.com/Decathlon/internal-developer-platform/security/advisories/new) of this repository
2. Fill out the advisory form with details about the vulnerability
3. Submit the advisory

#### Option 2: email

Send an email to **<security@decathlon.com>** with the following information:

- **Subject**: Security Vulnerability in IDP-Core
- **Description**: A detailed description of the vulnerability
- **Impact**: The potential impact of the vulnerability
- **Reproduction**: Steps to reproduce the issue
- **Affected versions**: Which versions are affected
- **Suggested fix**: If you have one, please share

### What to Expect

When you report a vulnerability, you can expect:

1. **Acknowledgment**: We will acknowledge receipt of your report within **3 business days**
2. **Investigation**: We will investigate and validate the report within **7 days**
3. **Updates**: We will keep you informed about the progress toward a fix
4. **Credit**: With your permission, we will publicly acknowledge your responsible disclosure once the issue is resolved

### Response Timeline

- **Initial Response**: Within 3 business days
- **Status Update**: Within 7 days
- **Fix Timeline**: Depends on severity
  - **Critical**: Expedited release (target: 14-30 days)
  - **High**: Next security release (target: 14-90 days)
  - **Medium/Low**: Next regular release

### Security Best Practices

When deploying IDP-Core, we recommend:

- Keep your instance updated to the latest version
- Follow the [deployment guidelines](docs/src/deployment/index.md)
- Use strong authentication mechanisms (preferred OAuth 2.0/OIDC)
- Regularly review access logs

### Disclosure Policy

- We follow a **coordinated disclosure** approach
- Security vulnerabilities will be disclosed publicly after a fix is available
- CVE IDs will be requested for confirmed vulnerabilities
- Credit will be given to reporters (unless anonymity is requested)

---

Thank you for helping keep IDP-Core and our users safe.
