---
name: adb-ui-inspector
description: Dump and inspect Android UI hierarchy live using adb and uiautomator
---

# ADB UI Inspector

## Dump live UI hierarchy
Run the following PowerShell commands to capture the UI hierarchy:
```powershell
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml ./dump.xml
```

## Extract unique resource-ids
Extract and list all unique `resource-id` attributes from the dumped XML:
```powershell
Select-String -Path ./dump.xml -Pattern 'resource-id="([^"]+)"' -AllMatches |
  ForEach-Object { $_.Matches } |
  ForEach-Object { $_.Groups[1].Value } |
  Where-Object { $_ -ne "" } |
  Sort-Object -Unique
```
