# OSH — 상세 구현 계획

## 1. 프로젝트 비전

개인용 실시간 정보 대시보드. 날씨, 교통, 재난, 뉴스, Gemini 요약 등 특화된 기능을 더 편리하고 확장 가능하게 개선.

## 2. 현재 상태

- Spring Boot 3.4.0-SNAPSHOT (불안정)
- 날씨(OpenWeatherMap), 교통, 재난, 뉴스, 시스템 정보
- SSE 기반 실시간 스트리밍 (1초/60초 간격)
- 스케줄러로 60초마다 데이터 갱신
- Thymeleaf 웹 UI
- Gemini AI 대시보드 요약
- 이중 DB (메인 + 뉴스 MySQL)
- WeatherInterface에 API 키 하드코딩
- Actuator 전체 노출

## 3. 디자인 시스템

**Google Material Design 3 (M3) 준용**

### M3 적용 방침
현재 Thymeleaf 기반이므로, 정적 HTML/CSS에 M3 원칙을 점진적으로 적용:

### 컬러 시스템
| 토큰 | Light | Dark | 용도 |
|------|-------|------|------|
| Primary | `#1B6B52` | `#7EDCB5` | 주요 액션, 강조 |
| On Primary | `#FFFFFF` | `#003828` | Primary 위 텍스트 |
| Secondary | `#4A635B` | `#B2CCBF` | 보조 요소 |
| Tertiary | `#3F6374` | `#A3CDDF` | 날씨/정보 강조 |
| Surface | `#F5FBF6` | `#1A1C1A` | 카드/배경 |
| Error | `#BA1A1A` | `#FFB4AB` | 재난/경고 |

> 대시보드 특성 반영: Primary를 차분한 그린 계열로 설정 (데이터 시각화에 적합)

### 타이포그래피
- M3 Type Scale: `headlineSmall`, `titleMedium`, `bodyLarge`, `labelMedium`
- 한글: Pretendard 또는 Noto Sans KR
- 숫자/데이터: Roboto Mono (고정폭)

### 컴포넌트
- M3 카드: Filled / Outlined / Elevated 혼용
- M3 Navigation: Top App Bar + Navigation Rail (사이드)
- 다크/라이트 모드 전환 지원
- `border-radius: 12px` (M3 Medium)

---

## 4. 단계별 구현 계획

### Phase 1 — 안정화 + 보안 수정 (2주)

**1.1 Spring Boot 버전 수정**
- [ ] `3.4.0-SNAPSHOT` → `3.4.x` 정식 릴리즈로 변경
- [ ] 스냅샷 저장소 참조 제거
- [ ] 의존성 호환성 확인

**1.2 보안 수정**
- [ ] `WeatherInterface`의 하드코딩 API 키 → `weather.properties` 또는 환경 변수로 이동
- [ ] `application.properties`의 DB 자격증명 → 환경 변수 분리
- [ ] Actuator: `include=*` → `include=health,info,metrics` 제한
- [ ] Gemini API 키 환경 변수 통일

**1.3 에러 핸들링**
- [ ] 외부 API 호출 실패 시 기본값/캐시 폴백
- [ ] SSE 연결 에러 처리 표준화
- [ ] 글로벌 예외 핸들러 추가

### Phase 2 — UI 리뉴얼 (M3) (4주)

**2.1 레이아웃 재설계**
- [ ] M3 Navigation Rail (좌측) + Top App Bar
- [ ] 대시보드: 그리드 카드 레이아웃 (반응형)
- [ ] 카드 표시/숨기기 + 순서 커스터마이징
- [ ] 다크/라이트 모드 토글

**2.2 데이터 카드 개선**
- [ ] 날씨 카드: 온도, 습도, 풍속을 M3 카드에 아이콘과 함께 표시
- [ ] 교통/재난 카드: 경고 수준별 컬러 코딩 (Error 토큰)
- [ ] 뉴스 카드: 헤드라인 리스트, 클릭 시 상세
- [ ] 시스템 카드: CPU/메모리 게이지 (SVG 원형 차트)

**2.3 반응형 대응**
- [ ] 모바일: 1열 카드 스택
- [ ] 태블릿: 2열 그리드
- [ ] 데스크톱: 3~4열 그리드
- [ ] M3 브레이크포인트: compact(<600), medium(600~840), expanded(840+)

### Phase 3 — 기능 확장 (4주)

**3.1 뉴스 키워드 필터**
- [ ] 관심 키워드 등록 UI
- [ ] 키워드 매칭 뉴스만 SSE 스트림
- [ ] 키워드별 뉴스 카운트 배지

**3.2 대시보드 뷰 프리셋**
- [ ] 출근 전: 날씨 + 교통 중심
- [ ] 업무 중: 뉴스 + 시스템 중심
- [ ] 퇴근 후: 생활정보 중심
- [ ] 시간대별 자동 전환 스케줄

**3.3 Gemini 대시보드 어시스턴트**
- [ ] 현재: 단순 요약 API
- [ ] 개선: 대시보드 데이터에 대한 대화형 질의 ("오늘 날씨 어때?", "교통 상황은?")
- [ ] 채팅 패널 UI (M3 Bottom Sheet 또는 Side Panel)

**3.4 SSE 안정성**
- [ ] 클라이언트 자동 재연결 (지수 백오프)
- [ ] heartbeat 메시지 (30초)
- [ ] 연결 상태 표시 (M3 Snackbar)

### Phase 4 — myAPI 통합 검토 + 안정화 (3주)

**4.1 myAPI 통합 검토**
- [ ] 중복 기능 정리: 날씨, 뉴스, 시스템 모니터링
- [ ] osh 고유 기능(교통/재난 SSE) → myAPI로 마이그레이션 방안
- [ ] 또는 osh를 "실시간 모니터링 특화", myAPI를 "생산성 도구 특화"로 역할 분리

**4.2 테스트**
- [ ] 외부 API mock 기반 단위 테스트
- [ ] SSE 통합 테스트
- [ ] 장시간 운영 안정성 테스트 (메모리 누수 점검)

---

## 5. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| 언어 | Java 17 | — |
| 프레임워크 | Spring Boot 3.4.x (정식) | 스냅샷 → 정식 |
| 웹 | Spring Web + WebFlux (SSE) | — |
| 템플릿 | Thymeleaf | 기존 유지 |
| DB | MySQL/MariaDB + H2 | — |
| 실시간 | SSE (Flux) | — |
| AI | Gemini API | — |
| 디자인 | M3 원칙 + CSS 커스텀 | 신규 적용 |
