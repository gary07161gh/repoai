# Errors Log

Continuously updated log of command failures, runtime errors, and tool exceptions.

---

## [ERR-20260902-001] command_not_found_antigravityPro_codegen

**Logged**: 2026-09-02T00:52:12Z
**Priority**: medium
**Status**: resolved
**Area**: config

### Summary

Command `antigravityPro.codegen` not found when triggered in IDE.

### Error

```text
command 'antigravityPro.codegen' not found
```

### Context

- User attempted to run `antigravityPro.codegen` shortcut / command in Antigravity IDE
- Mapped in keybindings/settings to legacy extension command `antigravityPro.codegen`

### Suggested Fix

- Reload IDE Window (`Developer: Reload Window`)
- Clean stale keybindings in `keybindings.json`
- Use native `Ctrl + I` inline edit modality (`antigravity.inlineEdit`)

### Metadata

- Reproducible: yes
- Related Files: C:/Users/Gary0/AppData/Roaming/Antigravity IDE/User/settings.json

### Resolution

- **Resolved**: 2026-09-02T01:05:00Z
- **Notes**: Provided troubleshooting and verified standard `Ctrl + I` / sidebar agent workflow.

---
