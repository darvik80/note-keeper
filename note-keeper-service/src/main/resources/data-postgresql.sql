-- ============================================================
-- System user (owner of default templates, not a real account)
-- ============================================================
INSERT INTO users (id, email, name, avatar_url, provider, google_id, is_active, created_at, updated_at)
VALUES ('system', 'system@notekeeper.local', 'System', NULL, 'local', NULL, false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- System default templates (visible to all users, owner_id = 'system')
-- ============================================================

-- Work: Meeting Notes
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["meeting","work"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Work: Project Plan
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["project","planning"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Work: Weekly Report
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["report","weekly","work"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Work: Bug Report
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["bug","work"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Work: Decision Log
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["decision","work"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Work: Retrospective
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["retrospective","work"]', 'Work', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Development: Code Review
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["code-review","development"]', 'Development', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Development: Technical Design / RFC
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["rfc","design","development"]', 'Development', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Development: Incident Postmortem
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["postmortem","incident","development"]', 'Development', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Development: API Design
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["api","design","development"]', 'Development', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Personal: Daily Journal
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["journal","personal"]', 'Personal', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Personal: Goal Setting
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["goals","personal","planning"]', 'Personal', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Personal: Book Notes
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["book","notes","personal"]', 'Personal', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Personal: Travel Plan
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["travel","personal"]', 'Personal', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Study: Lecture Notes
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["study","lecture"]', 'Study', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Study: Research Notes
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["research","study"]', 'Study', 'system', NOW())
ON CONFLICT (id) DO NOTHING;

-- Finance: Budget
INSERT INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["budget","finance"]', 'Finance', 'system', NOW())
ON CONFLICT (id) DO NOTHING;
