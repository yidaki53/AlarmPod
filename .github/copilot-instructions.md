Use [AGENTS.md](/home/robin/OneDrive/University%20and%20such/My%20Papers/Works%20in%20progress/antennapod/AGENTS.md) as the primary repository contract.

Repository-specific Copilot learnings are split into focused instruction files under [.github/instructions](/home/robin/OneDrive/University%20and%20such/My%20Papers/Works%20in%20progress/antennapod/.github/instructions).
Keep feature work narrowly scoped. For podcast alarm changes in particular, do not touch code outside the alarm functionality and its directly required integration points unless it is absolutely necessary.

Current instruction files:
- [podcast-alarm.instructions.md](/home/robin/OneDrive/University%20and%20such/My%20Papers/Works%20in%20progress/antennapod/.github/instructions/podcast-alarm.instructions.md): architecture and behavior for the podcast alarm feature.
- [android-local-toolchain.instructions.md](/home/robin/OneDrive/University%20and%20such/My%20Papers/Works%20in%20progress/antennapod/.github/instructions/android-local-toolchain.instructions.md): validated local build environment details for this machine.
- [release-automation.instructions.md](/home/robin/OneDrive/University%20and%20such/My%20Papers/Works%20in%20progress/antennapod/.github/instructions/release-automation.instructions.md): branch, workflow, and versioning conventions for this fork's GitHub Actions automation.