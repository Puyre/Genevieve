---
name: verify
description: Verify the project compiles by running `./gradlew -q compileKotlin`. Use before marking a code-change task as done, or when asked to check that recent edits still compile.
---

Run the compile check from the project root:

```bash
./gradlew -q compileKotlin
```

If compilation fails, report the errors exactly as Gradle emits them and do not mark the task done. If it succeeds with no output, state that compilation passed.

Notes:
- This is a compile-only check — it does not execute tests or run `Main.kt`.
- The `-q` flag suppresses Gradle's progress chatter; real errors still print.
- First invocation after a clean can be slow (Kotlin compiler warmup); subsequent runs are fast.
