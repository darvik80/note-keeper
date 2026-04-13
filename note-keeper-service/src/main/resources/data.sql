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
