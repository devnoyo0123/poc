# 저작권료 엑셀 파싱 POC

4가지 협회 타입의 엑셀 파일을 업로드하고, 헤더 매칭을 통해 JSONB로 저장하는 시스템

## 아키텍처

```
엑셀 업로드 → 협회 타입 선택 → DB 매핑 조회 → EasyExcel SAX 파싱
→ 헤더 매칭 → PostgreSQL JSONB 저장 + S3 원본 저장
```

## 기술 스택

- Kotlin + Spring Boot 3.2
- PostgreSQL (JSONB)
- EasyExcel 4.0.3 (SAX 스트리밍)
- AWS SDK v2 (S3)
- LocalStack (로컬 S3)
- Testcontainers (통합 테스트)

## 4가지 협회 타입

| 타입 | 협회명 | 헤더 시작 행 | 특징 |
|------|--------|-------------|------|
| A | 음반협회 | 1 | 단순 포맷 |
| B | 방송협회 | 3 | 타이틀/기간/날짜 행이 존재 |
| C | 영화협회 | 2 | 중간에 요약 행 |
| D | 공연협회 | 1 | 컬럼 순서 다름 |

## 로컬 실행

### 1. 인프라 시작

```bash
docker-compose up -d
```

- PostgreSQL: localhost:5434
- LocalStack: localhost:4566

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. API 테스트

#### 협회 타입 조회

```bash
curl http://localhost:8080/api/excel/types
```

응답:
```json
{
  "A": "음반협회",
  "B": "방송협회",
  "C": "영화협회",
  "D": "공연협회"
}
```

#### 엑셀 업로드

```bash
curl -X POST http://localhost:8080/api/excel/upload \
  -F "type=A" \
  -F "file=@sample.xlsx"
```

응답:
```json
{
  "uploadId": "uuid",
  "type": "A",
  "typeName": "음반협회",
  "originalFilename": "sample.xlsx",
  "status": "COMPLETED",
  "totalRows": 100,
  "processedRows": 100,
  "headerMapping": {
    "저작권료": "royalty_fee",
    "저작자": "copyright_holder",
    "작품명": "work_title",
    "사용일": "usage_date",
    "판매량": "sales_count"
  }
}
```

#### 업로드 상세 조회

```bash
curl http://localhost:8080/api/excel/uploads/{uploadId}
```

## DB 스키마

### excel_type_mappings

협회별 헤더 매핑 정보

```sql
type: 'A', 'B', 'C', 'D'
header_row_start: 1, 2, 3...
column_mappings: {"엑셀헤더명": "db컬럼명", ...}
```

### excel_uploads

업로드 이력

```sql
id: UUID
type: 협회 타입
original_filename: 원본 파일명
s3_key: S3 경로
status: PROCESSING, COMPLETED, FAILED
total_rows, processed_rows
```

### royalty_records

파싱된 데이터 (행별 JSONB)

```sql
upload_id: FK to excel_uploads
row_number: 행 번호
original_data: 원본 엑셀 데이터 {"컬럼명": "값", ...}
matched_data: 매핑된 데이터 {"db컬럼명": "값", ...}
header_mapping: 사용된 헤더 매핑
```

## 핵심 포인트

1. **EasyExcel SAX 스트리밍**: 대용량 엑셀도 메모리 부하 없이 처리
2. **동적 헤더 매칭**: DB에 저장된 매핑으로 협회별 포맷 자동 처리
3. **JSONB 저장**: 유연한 스키마, 별도 테이블 불필요
4. **S3 원본 저장**: Audit trail 및 재파싱 지원
5. **로컬/AWS 전환**: 설정만으로 로컬(LocalStack) ↔ 프로덕션 전환

## 엑셀 샘플 파일

`samples/` 디렉토리에 4가지 협회 타입별 예제 파일이 있습니다:

```bash
samples/
├── type_a_music.xlsx       # 음반협회 (헤더 1행부터)
├── type_b_broadcast.xlsx   # 방송협회 (헤더 3행부터, 타이틀/기간 행 존재)
├── type_c_movie.xlsx       # 영화협회 (헤더 2행부터)
└── type_d_performance.xlsx # 공연협회 (헤더 1행부터, 컬럼 순서 다름)
```

샘플 생성:
```bash
python3 generate_samples.py
```

### 음반협회 (타입 A) - 헤더 1행부터

| 저작권료 | 저작자 | 작품명 | 사용일 | 판매량 |
|---------|--------|--------|--------|--------|
| 100000 | 홍길동 | Song1 | 2024-01-01 | 1000 |
| 150000 | 김철수 | Song2 | 2024-01-02 | 1500 |

### 방송협회 (타입 B) - 헤더 3행부터

```
[1행] 방송협회 저작권료 정산 보고서
[2행] 정산 기간: 2024년 1월 ~ 3월
[3행] 지급액 | 권리자 | 프로그램명 | 방송일 | 방송시간
[4행~] 50000 | KBS | 뉴스9 | 2024-01-01 | 60
```

### 영화협회 (타입 C) - 헤더 2행부터

| 상영료 | 제작사 | 영화제목 | 개봉일 | 관객수 |
|--------|--------|----------|--------|--------|
| 5000000 | CJ엔터테인먼트 | 범죄도시4 | 2024-01-01 | 1000000 |

### 공연협회 (타입 D) - 헤더 1행부터, **컬럼 순서 다름**

| 공연자 | 티켓판매 | 공연일 | 공연료 | 공연명 |
|--------|----------|--------|--------|--------|
| BTS | 5000000 | 2024-01-01 | 100000000 | BTS WORLD TOUR |

## 프로젝트 구조

```
src/main/kotlin/com/example/royalty/
├── entity/           # JPA 엔티티
├── repository/       # Spring Data JPA
├── service/          # 비즈니스 로직
│   └── ExcelParsingService  # EasyExcel 파싱
├── controller/       # REST API
├── dto/             # Request/Response
└── excel/
    └── listener/    # EasyExcel Listener (SAX)

db/init/             # DB 초기화 스크립트
docker-compose.yml   # PostgreSQL + LocalStack
```
