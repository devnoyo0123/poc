# Claude + ArchUnit 자동 수정 워크플로우 PoC

**개발자 Cosmos - Claude AI 코딩 비용 절감 방법** 실제 검증

## 🎯 목표

Claude API와 ArchUnit을 연동하여 아키텍처 규칙 위반을 자동으로 수정하는 워크플로우 검증

## 📁 프로젝트 구조

```
claude-archunit-automation/
├── test-project/              # 아키텍처 위반이 포함된 샘플 프로젝트
│   └── src/main/java/com/example/
│       ├── controller/        # UserController (Repository 직접 접근 위반)
│       ├── service/           # UserService
│       └── repository/        # UserRepository
│
├── archunit-rules/           # ArchUnit 테스트 규칙
│   └── src/test/java/com/example/archunit/
│       └── ArchitectureTest.java
│
├── automation/                # 자동화 스크립트
│   └── claude-auto-fix.py   # 메인 워크플로우 스크립트
│
└── README.md                  # 이 문서
```

## 🔄 워크플로우

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│      A      │     │      B      │     │      C      │
│   Claude    │────▶│  생성된 코드  │────▶│  ArchUnit   │
│  (코드 생성)  │     │             │     │  테스트 실행  │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                    ┌──────────┴──────────┐
                                    │                     │
                                  통과                   실패
                                    │                     │
                                    ▼                     ▼
                           ┌─────────────┐       ┌─────────────┐
                           │      D      │       │      E      │
                           │  아키텍처    │       │    위반     │
                           │  규칙 준수 ✓ │       │   메시지 ✗  │
                           └─────────────┘       └──────┬──────┘
                                                        │
                                                        ▼
                                                ┌─────────────┐
                                                │      F      │
                                                │   Claude    │
                                                │  (자동 수정)  │
                                                └──────┬──────┘
                                                       │
                                                       └────────▶ (B로 돌아감)
```

## 🚀 실행 방법

### 1. 필수 조건

```bash
# Java 17+
java -version

# Gradle (자동으로 gradlew 생성됨)
```

### 2. ArchUnit 테스트 실행 (위반 확인)

```bash
cd archunit-rules
../test-project/gradlew test

# 예상 결과: ❌ 실패 (UserController → UserRepository 직접 접근)
```

### 3. 자동 수정 스크립트 실행

```bash
cd automation
chmod +x claude-auto-fix.py
python3 claude-auto-fix.py

# 워크플로우:
# 1. ArchUnit 테스트 실행
# 2. 위반 감지 시 Claude API로 전송
# 3. Claude가 수정 제안 생성
# 4. 수정 적용 후 재테스트
# 5. 통과할 때까지 반복 (최대 5회)
```

## 📋 검증할 항목

- [ ] ArchUnit 테스트가 실제로 아키텍처 위반을 감지하는가?
- [ ] Claude API가 위반 메시지를 정확히 파싱하는가?
- [ ] Claude가 제대로 수정 코드를 생성하는가?
- [ ] 자동화 스크립트가 전체 워크플로우를 완료하는가?
- [ ] 수정 후 ArchUnit 테스트가 통과하는가?

## 🔍 테스트 시나리오

### 시나리오 1: Controller → Repository 직접 접근

**위반 코드:**
```java
// UserController.java
@RestController
public class UserController {
    // ❌ Controller가 Repository를 직접 의존
    private final UserRepository userRepository;
}
```

**ArchUnit 규칙:**
```java
// controllers_should_not_access_repositories_directly()
noClasses()
    .that().resideInAPackage("..controller..")
    .should().dependOnClassesThat()
    .resideInAPackage("..repository..");
```

**예상 Claude 수정:**
```java
// 수정 후
@RestController
public class UserController {
    // ✅ Service를 통해서만 접근
    private final UserService userService;
}
```

## 💡 비용 절감 효과 분석 포인트

### 1. 토큰 사용량
- ArchUnit 에러 메시지 → Claude 입력 (짧음)
- 수정된 코드 → Claude 출력 (중간 크기)
- 반복 횟수에 따른 비용 측정

### 2. 캐싱 효과
- 반복적인 아키텍처 위반 패턴을 캐싱할 수 있는가?
- 유사한 위반 수정 제안을 재사용할 수 있는가?

### 3. 워크플로우 최적화
- 최대 반복 횟수 제한 (현재: 5회)
- 병렬 처리 가능성 (여러 파일 동시 수정)
- 점진적 수정 vs 일괄 수정

## 📊 예상 결과

### 성공 사례
```
📍 Iteration 1/5
🧪 Running tests in archunit-rules...
❌ Found 2 architecture violation(s)
   1. Controller 'UserController' depends on class 'UserRepository'
   2. Service 'UserService' directly accesses database layer
🤖 Sending violations to Claude for analysis...
🔧 Applying 2 fix(es)...
  • Modifying src/main/java/com/example/controller/UserController.java
      Remove UserRepository dependency, inject only UserService
  • Modifying src/main/java/com/example/service/UserService.java
      Add UserRepository dependency for data access
✅ Fixes applied, re-running tests...

📍 Iteration 2/5
🧪 Running tests in archunit-rules...
✅ All architecture tests passed!
   Architecture rules are satisfied.
```

### 실패 사례 (Claude 수정 오류)
```
🤖 Sending violations to Claude for analysis...
❌ Error calling Claude API: Invalid JSON response
⚠️ Claude response didn't contain valid JSON
```

## 🛠️ 트러블슈팅

### 문제: Gradle wrapper 없음
```bash
cd test-project
gradle wrapper --gradle-version 8.5
```

### 문제: Claude API 인증 오류
```bash
export ANTHROPIC_AUTH_TOKEN="your-key-here"
export ANTHROPIC_BASE_URL="https://api.anthropic.com"  # 또는 커스텀 URL
```

### 문제: ArchUnit 클래스패스 매칭
```java
// test-project의 클래스를 import해야 함
ImportOptions.Predefined.DO_NOT_INCLUDE_TESTS
.importPackages("com.example")
```

## 📝 다음 단계

1. **실행 및 검증**: 위 절차대로 실행하여 실제 동작 확인
2. **메트릭 수집**: Claude API 토큰 사용량, 응답 시간, 수정 정확도
3. **최적화**: 반복 횟수, 캐싱 전략, 병렬 처리 등
4. **확장**: 더 복잡한 아키텍처 규칙, 다층 구조 적용

## 🔗 관련 링크

- [ArchUnit 공식 문서](https://www.archunit.org/)
- [Anthropic API 레퍼런스](https://docs.anthropic.com/)
- [개발자 Cosmos 블로그: Claude 코딩 비용 절감 방법](https://techblog.musinsa.com/개발자-cosmos-ai-claude-코딩-비용-줄이는-법-활용-61c3d533fc40)
