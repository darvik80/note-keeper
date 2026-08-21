-- ============================================================
-- System user (owner of default templates, not a real account)
-- ============================================================
INSERT OR IGNORE INTO users (id, email, name, avatar_url, provider, google_id, is_active, created_at, updated_at)
VALUES ('system', 'system@notekeeper.local', 'System', NULL, 'local', NULL, 0, datetime('now'), datetime('now'));

-- ============================================================
-- System default templates (visible to all users, owner_id = 'system')
-- ============================================================

-- Work: Meeting Notes
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_meeting', 'Meeting Notes',
'# Meeting Notes

**Date:**
**Attendees:**

## Agenda
- [ ]

## Discussion


## Action Items
- [ ]

## Next Steps
- [ ]',
'["meeting","work"]', 'Work', 'system', datetime('now'));

-- Work: Project Plan
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_project', 'Project Plan',
'# Project Plan

## Overview


## Goals
1.

## Timeline
- **Phase 1:**
- **Phase 2:**
- **Phase 3:**

## Resources

## Risks & Mitigation
| Risk | Impact | Mitigation |
|------|--------|------------|
| | | |

## Success Criteria
- [ ]',
'["project","planning"]', 'Work', 'system', datetime('now'));

-- Work: Weekly Report
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_weekly_report', 'Weekly Report',
'# Weekly Report

**Week:**
**Author:**

## Completed This Week
- [x]

## In Progress
- [ ]

## Blocked
- [ ]

## Plan for Next Week
- [ ]

## Notes
',
'["report","weekly","work"]', 'Work', 'system', datetime('now'));

-- Work: Bug Report
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_bug_report', 'Bug Report',
'# Bug Report

**Severity:** Low / Medium / High / Critical
**Status:** Open / In Progress / Resolved
**Reporter:**
**Date:**

## Description


## Steps to Reproduce
1.
2.
3.

## Expected Behavior


## Actual Behavior


## Environment
- **OS:**
- **Browser/App version:**

## Screenshots / Logs


## Possible Fix
',
'["bug","work"]', 'Work', 'system', datetime('now'));

-- Work: Decision Log
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_decision', 'Decision Log',
'# Decision Log

**Date:**
**Decision:**
**Decided by:**

## Context


## Options Considered

### Option A
- **Pros:**
- **Cons:**

### Option B
- **Pros:**
- **Cons:**

## Decision


## Consequences
',
'["decision","work"]', 'Work', 'system', datetime('now'));

-- Work: Retrospective
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_retro', 'Retrospective',
'# Retrospective

**Sprint/Period:**
**Date:**
**Participants:**

## What Went Well
-

## What Didn''t Go Well
-

## Action Items
| Action | Owner | Due Date |
|--------|-------|----------|
| | | |

## Improvements for Next Time
',
'["retrospective","work"]', 'Work', 'system', datetime('now'));

-- Development: Code Review
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_code_review', 'Code Review',
'# Code Review

**PR:**
**Author:**
**Reviewer:**
**Date:**

## Summary


## Architecture & Design


## Code Quality
- [ ] Naming conventions
- [ ] DRY / No duplication
- [ ] Error handling
- [ ] Edge cases covered
- [ ] Tests added/updated

## Issues Found
| # | Severity | Description | Line |
|---|----------|-------------|------|
| 1 | | | |

## Verdict
- [ ] Approve
- [ ] Request Changes
- [ ] Needs Discussion',
'["code-review","development"]', 'Development', 'system', datetime('now'));

-- Development: Technical Design / RFC
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_rfc', 'Technical Design (RFC)',
'# Technical Design

**Author:**
**Status:** Draft / Under Review / Accepted / Rejected
**Date:**

## Problem Statement


## Proposed Solution


## Alternatives Considered


## Architecture


## API Changes


## Database Changes


## Migration Plan


## Rollback Plan


## Open Questions
- [ ]',
'["rfc","design","development"]', 'Development', 'system', datetime('now'));

-- Development: Incident Postmortem
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_postmortem', 'Incident Postmortem',
'# Incident Postmortem

**Incident:**
**Severity:** S1 / S2 / S3
**Date:**
**Duration:**
**Author:**

## Summary


## Timeline
| Time | Event |
|------|-------|
| | |

## Impact


## Root Cause


## Resolution


## Prevention
- [ ]

## Lessons Learned
',
'["postmortem","incident","development"]', 'Development', 'system', datetime('now'));

-- Development: API Design
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_api_design', 'API Design',
'# API Design

**Endpoint:**
**Method:** GET / POST / PUT / DELETE
**Auth:**

## Request

### Headers
| Header | Required | Description |
|--------|----------|-------------|
| | | |

### Body
```json
{
}
```

## Response

### Success (200)
```json
{
}
```

### Error
| Code | Description |
|------|-------------|
| 400 | |
| 401 | |
| 404 | |

## Notes
',
'["api","design","development"]', 'Development', 'system', datetime('now'));

-- Personal: Daily Journal
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_journal', 'Daily Journal',
'# Daily Journal

**Date:**

## What I accomplished today
-

## What I learned
-

## Grateful for
-

## Plans for tomorrow
- [ ]',
'["journal","personal"]', 'Personal', 'system', datetime('now'));

-- Personal: Goal Setting
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_goalsals', 'Goal Setting',
'# Goal Setting

**Period:** Q1 / Q2 / Q3 / Q4 / Year

## Goals

### Goal 1
- **Description:**
- **Why it matters:**
- **Key Results:**
  - [ ]
  - [ ]
- **Deadline:**

### Goal 2
- **Description:**
- **Why it matters:**
- **Key Results:**
  - [ ]
  - [ ]
- **Deadline:**

## Review Notes
',
'["goals","personal","planning"]', 'Personal', 'system', datetime('now'));

-- Personal: Book Notes
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_book_notes', 'Book Notes',
'# Book Notes

**Title:**
**Author:**
**Date finished:**
**Rating:** /5

## Summary


## Key Takeaways
1.
2.
3.

## Favorite Quotes
>

## How I can apply this
',
'["book","notes","personal"]', 'Personal', 'system', datetime('now'));

-- Personal: Travel Plan
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_travel', 'Travel Plan',
'# Travel Plan

**Destination:**
**Dates:**

## Flights
| Date | From | To | Flight | Confirmation |
|------|------|----|--------|-------------|
| | | | | |

## Accommodation
| Dates | Hotel | Confirmation |
|-------|-------|-------------|
| | | |

## Itinerary
### Day 1
- [ ]

### Day 2
- [ ]

## Packing List
- [ ] Passport / ID
- [ ] Tickets
- [ ] Charger

## Budget
| Item | Cost |
|------|------|
| | |
| **Total** | **0** |',
'["travel","personal"]', 'Personal', 'system', datetime('now'));

-- Study: Lecture Notes
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_lecture', 'Lecture Notes',
'# Lecture Notes

**Course:**
**Topic:**
**Date:**

## Key Concepts


## Definitions


## Examples


## Questions
- [ ]

## Further Reading
',
'["study","lecture"]', 'Study', 'system', datetime('now'));

-- Study: Research Notes
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_research', 'Research Notes',
'# Research Notes

**Topic:**
**Date:**

## Research Question


## Sources
1.

## Findings


## Analysis


## Conclusions


## Next Steps
- [ ]',
'["research","study"]', 'Study', 'system', datetime('now'));

-- Finance: Budget
INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_sys_budget', 'Monthly Budget',
'# Monthly Budget

**Month:**

## Income
| Source | Amount |
|--------|--------|
| | |
| **Total** | **0** |

## Expenses
| Category | Planned | Actual |
|----------|---------|--------|
| Housing | | |
| Food | | |
| Transport | | |
| Entertainment | | |
| Other | | |
| **Total** | **0** | **0** |

## Savings Goal

## Notes
',
'["budget","finance"]', 'Finance', 'system', datetime('now'));

-- ============================================================
-- Test user (for development only)
-- ============================================================
INSERT OR IGNORE INTO users (id, email, name, avatar_url, provider, google_id, is_active, created_at, updated_at)
VALUES ('test-user-001', 'test@example.com', 'Test User', NULL, 'local', NULL, 1, datetime('now'), datetime('now'));

INSERT OR IGNORE INTO user_credentials (user_id, password_hash, salt, created_at, updated_at)
VALUES ('test-user-001', '$2a$12$..rD.MSXulO7JEAmYdA31.rWcS6smUrv.Z0TpQ.YpGdvC8p1qNhGK', '', datetime('now'), datetime('now'));
