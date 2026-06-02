# POC Projects

This repository contains various Proof of Concept (POC) projects for Kotlin/Java SpringBoot development.

## Structure

```
poc/
├── README.md                    # This file
├── .gitignore                   # Common ignore rules
├── projects/                    # Individual POC projects
│   ├── sse-webflux-servlet-projects/  # SSE WebFlux vs Servlet comparison
│   ├── webflux-sse-lab/              # WebFlux + R2DBC + jOOQ + SSE
│   ├── webflux-user-api/             # WebFlux user API
│   ├── claude-archunit-automation/     # ArchUnit + Claude auto-fix automation
│   ├── spring-cache-demo/            # Gradle project example
│   ├── jpa-playground/               # Gradle multi-module project
│   ├── maven-example/                # Maven project
│   └── legacy-gradle/              # Legacy Gradle project
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
| [sse-webflux-servlet-projects](projects/sse-webflux-servlet-projects/) | Gradle (Kotlin DSL) | SSE comparison: WebFlux vs Servlet | ✅ Complete |
| [sse-performance-comparison](projects/sse-performance-comparison/) | - | SSE performance comparison | - |
| [webflux-sse-lab](projects/webflux-sse-lab/) | Gradle (Kotlin DSL) | WebFlux + R2DBC + jOOQ + SSE integration | ✅ Complete |
| [webflux-user-api](projects/webflux-user-api/) | Gradle (Kotlin DSL) | Simple WebFlux user API | ✅ Complete |
| [claude-archunit-automation](projects/claude-archunit-automation/) | Gradle (Kotlin DSL) | ArchUnit rules + Claude auto-fix automation | ✅ Complete |
| [api-call-retry-save-poc](projects/api-call-retry-save-poc/) | - | API call retry with save POC | - |
| [avro-poc](projects/avro-poc/) | Gradle (Kotlin DSL) | Apache Avro serialization POC | ✅ Complete |
| [jobrunr-poc](projects/jobrunr-poc/) | Gradle (Groovy DSL) | JobRunr background job processing | ✅ Complete |
| [jpa-capa-scheduler-poc](projects/jpa-capa-scheduler-poc/) | - | JPA capacity scheduler POC | - |
| [jpa-concurrency-poc](projects/jpa-concurrency-poc/) | - | JPA concurrency POC | - |
| [payment-sdk-kotlin](projects/payment-sdk-kotlin/) | - | Payment SDK in Kotlin | - |
| [rate-limited-api-poc](projects/rate-limited-api-poc/) | - | Rate-limited API POC | - |
| [resilience4j-retry-poc](projects/resilience4j-retry-poc/) | - | Resilience4j retry POC | - |
| [resttemplate-async-comparison](projects/resttemplate-async-comparison/) | - | RestTemplate async comparison | - |
| [royalty-excel-poc](projects/royalty-excel-poc/) | - | Royalty Excel processing POC | - |
| [s3-sqs-localstack-poc](projects/s3-sqs-localstack-poc/) | - | S3 + SQS LocalStack POC | - |
| [spring-batch-study](projects/spring-batch-study/) | - | Spring Batch study | - |

## Common Conventions

See [docs/conventions.md](docs/conventions.md) for:
- Code style guidelines
- Commit message conventions
- Project setup standards

## License

Each project may have its own license. Please refer to individual project documentation.
