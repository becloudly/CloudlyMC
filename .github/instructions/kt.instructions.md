# GitHub Copilot Instructions

These instructions define how GitHub Copilot should assist with this project. The goal is to ensure consistent, high-quality code generation aligned with our conventions, stack, and best practices.

## 🧠 Context

- **Project Type**: Minecraft server plugin
- **Language**: Kotlin
- **Framework / Libraries**: Paper / Folia, MySQL, Sqlite, JSON, http

## 🔧 General Guidelines

- Use Kotlin-idiomatic syntax and features (e.g., data classes, extension functions).
- Prefer immutable data (`val`) over mutable (`var`).
- Use null safety, smart casting, and Elvis operators effectively.
- Favor expression-style syntax and scoped functions (`let`, `apply`, `run`, `with`).
- Keep files and functions concise and focused.
- Use `ktlint` or `detekt` for formatting and code style.

## 📁 File Structure

Use this structure as a guide when creating or updating files:

```text
app/
  src/
    main/
      kotlin/
        de/
          cloudly/
            config/
            radar/
            utils/
            CloudlyPaper.kt
      resources/
        lang/
        config.yml
        plugin.yml
      templates/

```

## 🧶 Patterns

### ✅ Patterns to Follow

- Write clear, concise, and professional Kotlin code with performance, security and stability in mind.
- Handle possible errors properly with catches and write it null- and type-safe.
- Prefer function and stability over performance.
- Don’t ignore nullability warnings — handle them explicitly.
- Use `when` expressions for control flow instead of `if-else` chains.
- Write code that is simple, easy to read and understand, and avoiding overly complex logic which could break.

## 🔁 Iteration & Review

- Always review Copilot output for idiomatic Kotlin usage and safety.
- Guide Copilot with inline comments when generating complex logic.
- Refactor verbose Java-style patterns into concise Kotlin equivalents.

## 📚 References

- [Kotlin Language Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [KDoc Reference](https://kotlinlang.org/docs/kotlin-doc.html)