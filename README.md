# OSH — 실시간 정보 대시보드

> 날씨, 교통, 재난, 뉴스 등 실시간 정보를 모아 보여주는 웹 대시보드. SSE(Server-Sent Events) 기반 스트리밍과 Google Gemini AI 요약을 지원합니다.

## 주요 기능

- **실시간 대시보드** — 날씨, 교통, 재난, 뉴스 정보를 SSE로 주기적 스트리밍
- **날씨 정보** — OpenWeatherMap API 연동 (다중 좌표)
- **뉴스 API** — 뉴스 데이터 SSE 스트림, 전체 목록, 회사별 조회
- **Gemini AI 요약** — 대시보드 데이터를 AI로 요약 및 프롬프트 생성
- **스케줄러** — 60초 간격으로 재난, 교통, 뉴스, 날씨 데이터 자동 갱신
- **Actuator 모니터링** — Spring Boot Actuator로 시스템 상태 노출

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.4.0-SNAPSHOT |
| 웹 | Spring Web, Spring WebFlux (SSE Flux) |
| 템플릿 | Thymeleaf |
| 데이터 | Spring Data JPA, JDBC, 다중 DataSource (메인 + 뉴스) |
| DB | MySQL / MariaDB (런타임), H2 (테스트) |
| 빌드 | Maven |
| 기타 | Jsoup, Gson, Lombok, Actuator |

## 프로젝트 구조

```
osh/
├── pom.xml
└── src/main/
    ├── java/com/project/osh/
    │   ├── OshApplication.java        # 진입점 (@EnableScheduling)
    │   ├── controller/                # Main, Dashboard, News, Gemini, Test
    │   ├── service/                   # Dashboard, News, Gemini, EventEmitter
    │   ├── config/                    # DB, Dashboard, Weather 프로퍼티
    │   ├── schedule/                  # 60초 간격 데이터 갱신
    │   ├── interfaces/                # Weather, Traffic, Emergency, News HTTP 연동
    │   ├── model/                     # JPA 엔티티, DTO
    │   └── repository/                # 뉴스 Repository
    └── resources/
        ├── application.properties     # DB, 서버, 로깅, Gemini
        ├── dashboard.properties       # 대시보드 설정
        ├── weather.properties         # 날씨 도시·코드 목록
        ├── templates/                 # Thymeleaf 뷰
        └── static/js/                 # 프론트엔드 JS
```

## 실행

### 사전 요구사항
- JDK 17
- Maven
- MySQL 또는 MariaDB

### 실행
```bash
mvn spring-boot:run
```
기본 포트: `8080`

## 설정

`src/main/resources/application.properties`에서 다음 항목을 환경에 맞게 수정:

| 항목 | 설명 |
|------|------|
| DB 연결 | `spring.datasource.url`, 사용자, 비밀번호 |
| 뉴스 DB | `spring.datasource.news.*` (별도 DataSource) |
| Gemini | `gemini.api.key`, `gemini.api.url` |
| 날씨 | `weather.properties` (도시, 코드) |
| 대시보드 | `dashboard.properties` (재난/교통 건수) |

> **보안 참고**: DB 자격증명과 API 키는 운영 시 환경 변수로 분리하는 것을 권장합니다.

## 라이선스

MIT
