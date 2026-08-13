# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Compress Tool Output

When executing tools, compress outputs to save tokens:

- *Skip verbose headers* — remove unnecessary framework/tool information
- *Truncate long lists* — show only first 5-10 items with a count of remaining
- *Omit successful confirmations* — skip "success" messages unless critical
- *Abbreviate file paths* — use relative paths instead of full paths
- *Summarize repetitive output* — group similar lines with a count
- *Remove debug output* — strip timestamps, debug info, verbose logging
- *Only show actual results* — errors, key data, status only

Example: Instead of showing 100 lines of log output, show: "✓ Build succeeded (234 files processed, 3 warnings)"

## Compress LLM Output (Caveman Mode)

Keep responses concise and token-efficient:

- *Short sentences* — avoid elaboration, use direct language
- *Single sentences per idea* — break up complex thoughts
- *No fluff* — skip pleasantries, acknowledgments, transitions
- *Show don't tell* — present code/results instead of explaining
- *Lists over prose* — use bullet points instead of paragraphs
- *Direct answers* — respond to the exact question asked
- *Code first* — when writing code, minimize commentary
- *One summary line* — end with a single-line summary when done

Example: ✓ Done. (vs. "I've successfully completed the task as requested...")
