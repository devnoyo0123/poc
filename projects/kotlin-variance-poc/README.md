# Kotlin Variance POC

실제 업무 도메인 대신 `동물 급식 명령`으로 공변성, 반공변성, 무변성을 먼저 연습한다.

```bash
./gradlew test
```

학습 순서는 한 개념씩 진행한다.

1. 현재: `CommandBox<T>`로 무변성 이해
2. 다음: `out` 공변성
3. 다음: `in` 반공변성
4. 마지막: 실제 코드와 같은 TypedPipeline + Registry 패턴

현재 테스트는 의도적인 Red 상태다. `VarianceExamples.kt`에 값을 읽고 쓸 수 있는 `CommandBox<T>`를 직접 구현해 Green으로 만든다.

연습 순서:

1. 테스트를 읽고 `CommandBox<T>`의 최소 구현을 작성한다.
2. Green을 확인한다.
3. 주석 처리된 대입문을 해제해 무변성 컴파일 오류를 확인한다.

공변성과 반공변성은 무변성 drill을 끝낸 뒤 추가한다.

## Variance Drill

각 단계는 이전 단계가 Green이 된 뒤 하나씩 연다.

1. `[완료]` `<out T>`: 제한 없는 선언 지점 공변성
2. `[완료]` `<in T>`: 제한 없는 선언 지점 반공변성
3. `[진행 중]` `<out T : Base>`: 상한이 있는 선언 지점 공변성
4. `[대기]` `<in T : Base>`: 상한이 있는 선언 지점 반공변성
5. `[대기]` `Generic<out Base>`: 사용 지점 공변성
6. `[대기]` `Generic<in Base>`: 사용 지점 반공변성

현재 Red: `CommandSource`를 직접 구현해 세 번째 단계를 Green으로 만든다.
