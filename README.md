# POC Projects

This repository contains various Proof of Concept (POC) projects for Kotlin/Java SpringBoot development.

## Structure

```
poc/
├── README.md                    # This file
├── .gitignore                   # Common ignore rules
├── projects/                    # Individual POC projects
│   ├── spring-cache-demo/       # Gradle project example
│   ├── jpa-playground/          # Gradle multi-module project
│   ├── maven-example/           # Maven project
│   └── legacy-gradle/           # Legacy Gradle project
└── docs/                        # Common documentation
    ├── project-list.md
    └── conventions.md
```

## Design Principles

### 1. Project Independence
- Each project has its own build system (`build.gradle*`, `pom.xml`)
- Gradle versions, Maven settings are independent per project
- No dependencies between projects (explicit dependencies can be added if needed)

### 2. Git Workflow
```bash
cd poc/projects/spring-cache-demo
# Do your work...
cd ../../
git add . && git commit -m "feat: add caching example"
```

## Adding a New Project

1. Create a new directory under `projects/`:
   ```bash
   mkdir -p projects/your-new-project
   ```

2. Initialize with your preferred build system:
   - **Gradle (Kotlin DSL)**: `build.gradle.kts`, `settings.gradle.kts`
   - **Gradle (Groovy DSL)**: `build.gradle`, `settings.gradle`
   - **Maven**: `pom.xml`

3. Update `docs/project-list.md` with your project description

## Projects

| Project | Build Tool | Description | Status |
|---------|-----------|-------------|--------|
| *To be added* | - | - | - |

## Common Conventions

See [docs/conventions.md](docs/conventions.md) for:
- Code style guidelines
- Commit message conventions
- Project setup standards

## License

Each project may have its own license. Please refer to individual project documentation.
