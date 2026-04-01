---
name: Bug Report
description: Report a bug in the zz143 SDK
title: "[Bug]: "
labels: ["bug"]
body:
  - type: textarea
    id: description
    attributes:
      label: Description
      description: A clear description of the bug.
    validations:
      required: true
  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: Steps to reproduce the behavior.
      value: |
        1. Integrate zz143 SDK version '...'
        2. Call '...'
        3. See error
    validations:
      required: true
  - type: textarea
    id: expected
    attributes:
      label: Expected Behavior
      description: What you expected to happen.
    validations:
      required: true
  - type: textarea
    id: actual
    attributes:
      label: Actual Behavior
      description: What actually happened.
    validations:
      required: true
  - type: input
    id: sdk-version
    attributes:
      label: SDK Version
      placeholder: "0.1.0-alpha01"
    validations:
      required: true
  - type: input
    id: android-version
    attributes:
      label: Android Version
      placeholder: "Android 14 (API 34)"
  - type: input
    id: device
    attributes:
      label: Device
      placeholder: "Pixel 8, Samsung Galaxy S24, Emulator"
  - type: textarea
    id: logs
    attributes:
      label: Logs / Stack Trace
      description: Paste any relevant logs or stack traces.
      render: shell
---
