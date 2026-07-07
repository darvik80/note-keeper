INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_meeting', 'Meeting Notes',
'# Meeting Notes

**Date:** 
**Attendees:** 

## Agenda
- 

## Discussion


## Action Items
- [ ] ',
'["meeting","work"]', 'Work', 'test-user-001', datetime('now'));

INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
VALUES ('tmpl_journal', 'Daily Journal',
'# Daily Journal

**Date:** 

## What I accomplished today


## What I learned


## Plans for tomorrow

',
'["journal","personal"]', 'Personal', 'test-user-001', datetime('now'));

INSERT OR IGNORE INTO note_template (id, name, content, tags, category, owner_id, created_at)
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
'["project","planning"]', 'Work', 'test-user-001', datetime('now'));

-- Test user: test@example.com / password123
INSERT OR IGNORE INTO users (id, email, name, avatar_url, provider, google_id, is_active, created_at, updated_at)
VALUES ('test-user-001', 'test@example.com', 'Test User', NULL, 'local', NULL, 1, datetime('now'), datetime('now'));

INSERT OR IGNORE INTO user_credentials (user_id, password_hash, salt, created_at, updated_at)
VALUES ('test-user-001', '$2a$12$..rD.MSXulO7JEAmYdA31.rWcS6smUrv.Z0TpQ.YpGdvC8p1qNhGK', '', datetime('now'), datetime('now'));
