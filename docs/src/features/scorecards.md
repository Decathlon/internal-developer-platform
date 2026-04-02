---
title: Scorecards
description: Define and track quality metrics across your entities with levels, conditions, and DORA metrics
status: 🕐 Planned
---

> [!IMPORTANT]
> This document describes a feature that's not yet developed. The content is subject to change and may not reflect the final implementation.

Scorecards in the Internal Developer Platform allow you to define measurable standards and track compliance across your entities. Use scorecards to implement DORA metrics, security compliance, production readiness, and more.

## Overview

A scorecard consists of:

- **Levels** - Named achievement tiers, such as Bronze, Silver, Gold
- **Rules** - Conditions that entities must meet
- **Weights** - Relative importance of each rule
- **Score calculation** - Automatic tracking per entity

---

## Scorecard Definition

### Complete Example

```json
{
  "identifier": "production_readiness",
  "title": "Production Readiness",
  "description": "Measures how ready a service is for production",
  "levels": [
    {
      "color": "#CD7F32",
      "identifier": "bronze",
      "title": "Bronze",
      "min_percentage": 0
    },
    {
      "color": "#C0C0C0",
      "identifier": "silver",
      "title": "Silver",
      "min_percentage": 40
    },
    {
      "color": "#FFD700",
      "identifier": "gold",
      "title": "Gold",
      "min_percentage": 70
    },
    {
      "color": "#E5E4E2",
      "identifier": "platinum",
      "title": "Platinum",
      "min_percentage": 90
    }
  ],
  "rules": [
    {
      "identifier": "has_readme",
      "title": "Repository has README",
      "description": "A README.md file exists in the repository",
      "conditions": [
        {
          "property": "has_readme",
          "operator": "==",
          "value": true
        }
      ],
      "weight": 1
    },
    {
      "identifier": "has_cicd",
      "title": "Has CI/CD Pipeline",
      "conditions": [
        {
          "property": "has_ci",
          "operator": "==",
          "value": true
        }
      ],
      "weight": 2
    },
    {
      "identifier": "test_coverage",
      "title": "Test Coverage above 80%",
      "conditions": [
        {
          "property": "coverage_percentage",
          "operator": ">=",
          "value": 80
        }
      ],
      "weight": 3
    },
    {
      "identifier": "no_critical_vulnerabilities",
      "title": "No Critical Vulnerabilities",
      "conditions": [
        {
          "property": "critical_vuln_count",
          "operator": "==",
          "value": 0
        }
      ],
      "weight": 3
    }
  ],
  "target_templates": ["microservice", "api"]
}
```

### Scorecard Fields

| Field | Description |
| ------- | ------------- |
| `identifier` | Unique key for this scorecard |
| `title` | Human-readable name |
| `description` | Purpose of this scorecard |
| `levels` | Achievement tiers with color, min percentage |
| `rules` | Conditions to evaluate |
| `target_templates` | Entity Templates this scorecard applies to |

---

## Levels Configuration

Levels define achievement tiers:

```json
{
  "levels": [
    { "identifier": "bronze", "title": "Bronze", "color": "#CD7F32", "min_percentage": 0 },
    { "identifier": "silver", "title": "Silver", "color": "#C0C0C0", "min_percentage": 40 },
    { "identifier": "gold", "title": "Gold", "color": "#FFD700", "min_percentage": 70 }
  ]
}
```

| Field | Description |
| ------- | ------------- |
| `identifier` | Unique key for the level |
| `title` | Display name |
| `color` | Hex color for UI display |
| `min_percentage` | Minimum score to achieve this level |

---

## Rules & Conditions

### Rule Structure

```json
{
  "identifier": "rule_id",
  "title": "Rule Title",
  "description": "What this rule measures",
  "conditions": [...],
  "weight": 2
}
```

### Condition Operators

| Operator | Description | Example |
| ---------- | ------------- | --------- |
| `==` | Equals | `{"property": "status", "operator": "==", "value": "active"}` |
| `!=` | Not equals | `{"property": "tier", "operator": "!=", "value": "deprecated"}` |
| `>` | Greater than | `{"property": "uptime", "operator": ">", "value": 99}` |
| `>=` | Greater or equal | `{"property": "coverage", "operator": ">=", "value": 80}` |
| `<` | Less than | `{"property": "incidents", "operator": "<", "value": 5}` |
| `<=` | Less or equal | `{"property": "debt_days", "operator": "<=", "value": 10}` |
| `contains` | String contains | `{"property": "name", "operator": "contains", "value": "prod"}` |
| `in` | Value in list | `{"property": "env", "operator": "in", "value": ["prod", "staging"]}` |
| `not_in` | Value not in list | `{"property": "status", "operator": "not_in", "value": ["deprecated"]}` |
| `is_empty` | Is null or empty | `{"property": "owner", "operator": "is_empty"}` |
| `is_not_empty` | Has value | `{"property": "runbook", "operator": "is_not_empty"}` |

### Multiple Conditions (AND)

All conditions in a rule must be true:

```json
{
  "identifier": "production_ready",
  "title": "Production Ready",
  "conditions": [
    { "property": "has_monitoring", "operator": "==", "value": true },
    { "property": "has_alerting", "operator": "==", "value": true },
    { "property": "has_runbook", "operator": "==", "value": true }
  ],
  "weight": 5
}
```

---

## Weight Calculation

Calculate the score using this formula:

$$\text{Score} = \frac{\sum (\text{passed rules} \times \text{weight})}{\sum \text{all weights}} \times 100$$

### Example Calculation

| Rule | Weight | Passed |
| ------ | -------- | -------- |
| Has README | 1 | ✅ |
| Has CI or CD | 2 | ✅ |
| Coverage 80% | 3 | ❌ |
| No Critical Vulnerabilities | 3 | ✅ |

Total weight: $$1 + 2 + 3 + 3 = 9$$
Passed weight: $$1 + 2 + 3 = 6$$
Score: $$\frac{6}{9} \times 100 = 66.7\%$$
Displayed level → **Silver**

---

## DORA Metrics Example

Implement DORA (DevOps Research and Assessment) metrics as a scorecard:

```json
{
  "identifier": "dora_metrics",
  "title": "DORA Metrics",
  "description": "DevOps Research and Assessment metrics",
  "levels": [
    { "identifier": "low", "title": "Low Performer", "color": "#FF6B6B", "min_percentage": 0 },
    { "identifier": "medium", "title": "Medium Performer", "color": "#FFE66D", "min_percentage": 25 },
    { "identifier": "high", "title": "High Performer", "color": "#4ECDC4", "min_percentage": 50 },
    { "identifier": "elite", "title": "Elite Performer", "color": "#2ECC71", "min_percentage": 75 }
  ],
  "rules": [
    {
      "identifier": "deployment_frequency",
      "title": "Deployment Frequency",
      "description": "How often code is deployed to production",
      "conditions": [
        { "property": "deploys_per_week", "operator": ">=", "value": 1 }
      ],
      "weight": 25
    },
    {
      "identifier": "lead_time",
      "title": "Lead Time for Changes",
      "description": "Time from commit to production (in hours)",
      "conditions": [
        { "property": "lead_time_hours", "operator": "<=", "value": 24 }
      ],
      "weight": 25
    },
    {
      "identifier": "mttr",
      "title": "Mean Time to Recovery",
      "description": "Time to restore service (in hours)",
      "conditions": [
        { "property": "mttr_hours", "operator": "<=", "value": 1 }
      ],
      "weight": 25
    },
    {
      "identifier": "change_failure_rate",
      "title": "Change Failure Rate",
      "description": "Percentage of deployments causing failures",
      "conditions": [
        { "property": "change_failure_rate", "operator": "<=", "value": 15 }
      ],
      "weight": 25
    }
  ],
  "target_templates": ["microservice", "application"]
}
```

---

## Scorecard API

### Create Scorecard

```http
POST /api/v1/scorecards
Content-Type: application/json

{
  "identifier": "security_compliance",
  "title": "Security Compliance",
  ...
}
```

### Get Entity Scores

```http
GET /api/v1/entities/{entity_id}/scores
```

Response:

```json
{
  "entity_id": "service-a",
  "scorecards": [
    {
      "scorecard_id": "production_readiness",
      "score": 66.7,
      "level": {
        "identifier": "silver",
        "title": "Silver"
      },
      "rules": [
        { "identifier": "has_readme", "passed": true },
        { "identifier": "has_cicd", "passed": true },
        { "identifier": "test_coverage", "passed": false },
        { "identifier": "no_critical_vulnerabilities", "passed": true }
      ]
    }
  ]
}
```

### Get Scorecard Summary

```http
GET /api/v1/scorecards/{scorecard_id}/summary
```

Response:

```json
{
  "scorecard_id": "production_readiness",
  "entity_count": 45,
  "level_distribution": {
    "bronze": 10,
    "silver": 20,
    "gold": 12,
    "platinum": 3
  },
  "average_score": 58.4,
  "rule_compliance": {
    "has_readme": { "passed": 42, "total": 45 },
    "has_cicd": { "passed": 38, "total": 45 },
    "test_coverage": { "passed": 25, "total": 45 },
    "no_critical_vulnerabilities": { "passed": 30, "total": 45 }
  }
}
```

---

## Best Practices

### 1. Start Simple

Begin with a few important rules and expand:

```json
{
  "rules": [
    { "identifier": "has_owner", "title": "Has Owner", "weight": 1 },
    { "identifier": "has_docs", "title": "Has Documentation", "weight": 1 }
  ]
}
```

### 2. Balance Weights

Use weights to reflect business priority:

- Low weight: 1 for nice to have
- Medium weight: 2-3 for important
- High weight: 4-5 for critical

### 3. Clear Level Names

Use intuitive level names that teams understand:

| Style | Examples |
| ------- | ---------- |
| Medals | Bronze, Silver, Gold, Platinum |
| Maturity | Beginner, Intermediate, Advanced, Expert |
| Status | At Risk, Needs Improvement, Good, Excellent |
| Stars | ⭐, ⭐⭐, ⭐⭐⭐, ⭐⭐⭐⭐ |

---

## Next Steps

- **[Data Integration](data-integration.md)** - Feed data into properties
