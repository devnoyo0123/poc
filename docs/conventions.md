# Conventions

This document outlines the common conventions used across POC projects.

## Code Style

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 space indentation
- Prefer immutable data classes
- Use expression body for single-expression functions

### Java
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use 4 space indentation (NO tabs)
- Maximum line length: 100 characters

## Commit Messages

Follow conventional commits format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

### Examples
```
feat(cache): add Redis caching example
fix(jpa): resolve lazy loading issue
docs(readme): update project setup instructions
```

## Project Structure

### Standard Gradle Project
```
project-name/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/  (or java/)
│   │   └── resources/
│   └── test/
│       ├── kotlin/  (or java/)
│       └── resources/
└── README.md
```

### Multi-module Gradle Project
```
project-name/
├── build.gradle.kts
├── settings.gradle.kts
├── module-a/
├── module-b/
└── README.md
```

## Naming Conventions

### Project Names
- Use lowercase with hyphens: `spring-cache-demo`
- Be descriptive: `jpa-playground` not `jpa-test`

### Package Names
- Use lowercase: `com.example.demo`
- Follow reverse domain notation

## README Requirements

Each project should have a README.md with:
1. Project description
2. Prerequisites (Java version, etc.)
3. How to build and run
4. Key features demonstrated
5. Any relevant notes or caveats
