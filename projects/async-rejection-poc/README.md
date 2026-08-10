# async-rejection-poc

Spring `@Async` rejection propagation POC using Kotest + SpringBootTest.

Executor config:

- `corePoolSize = 1`
- `maxPoolSize = 1`
- `queueCapacity = 0`
- `RejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy`

Expected result:

1. First request submits async task and returns `202 Accepted`.
2. Async worker thread `excel-export-*` sleeps and keeps the only worker busy.
3. Second request enters the controller on `http-nio-*`.
4. `@Async` proxy calls `executor.submit(...)`.
5. Executor rejects before async handoff.
6. `ThreadPoolTaskExecutor` wraps rejection as `TaskRejectedException`.
7. `@ExceptionHandler(TaskRejectedException.class)` handles it on the request thread.
8. User receives `429 Too Many Requests`.

Run:

```bash
gradle test
```

Test stack:

- Kotest `StringSpec`
- `SpringExtension`
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `TestRestTemplate`
- Testcontainers `ComposeContainer`
- PostgreSQL from `docker-compose.yml`

Manual:

```bash
docker compose up -d postgres
gradle bootRun
curl -i -X POST 'http://localhost:8080/api/v1/half-yearly-distributions/excel?sleepMillis=10000'
curl -i -X POST 'http://localhost:8080/api/v1/half-yearly-distributions/excel?sleepMillis=100'
```

`gradle test` starts PostgreSQL through Testcontainers by reading `docker-compose.yml`.
Manual `docker compose up` is only for local inspection.
