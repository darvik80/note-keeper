INSERT OR IGNORE INTO note_template (id, name, content, tags, category, created_at)
VALUES ('tmpl_meeting', 'Meeting Notes',
'# Meeting Notes

**Date:** 
**Attendees:** 

## Agenda
- 

## Discussion


## Action Items
- [ ] ',
'["meeting","work"]', 'Work', datetime('now'));

INSERT OR IGNORE INTO note_template (id, name, content, tags, category, created_at)
VALUES ('tmpl_journal', 'Daily Journal',
'# Daily Journal

**Date:** 

## What I accomplished today


## What I learned


## Plans for tomorrow

',
'["journal","personal"]', 'Personal', datetime('now'));

INSERT OR IGNORE INTO note_template (id, name, content, tags, category, created_at)
VALUES ('tmpl_project', 'Project Plan',
'# Project Plan

## Overview

## Goals
1. 

## Timeline
- **Phase 1:** 
- **Phase 2:** 

## Resources

## Risks & Mitigation

',
'["project","planning"]', 'Work', datetime('now'));

-- Test user: test@example.com / password123
-- Password hash generated with SHA-256: password123 + salt
INSERT OR IGNORE INTO users (id, email, name, avatar_url, provider, google_id, is_active, created_at, updated_at)
VALUES ('test-user-001', 'test@example.com', 'Test User', NULL, 'local', NULL, 1, datetime('now'), datetime('now'));

INSERT OR IGNORE INTO user_credentials (user_id, password_hash, salt, created_at, updated_at)
VALUES ('test-user-001', 'qLkYvKxJzPz8xN9qR2mF4hL6pT8sA1cD3eG5iK7wXyZ=', 'randomSalt12345', datetime('now'), datetime('now'));
