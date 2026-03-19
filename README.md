# Allowance Manager

> 시스템 알림을 감지해 자동으로 소비를 추적하고, 하루 사용 가능 금액을 실시간으로 알려주는 용돈 관리 앱

---

## 소개

Allowance Manager는 사용자가 직접 지출을 입력하지 않아도 **카드 결제, 계좌 이체 등의 시스템 알림을 자동으로 감지**해 용돈을 차감하는 안드로이드 앱입니다.

하루에 얼마를 쓸 수 있는지 **상태바 고정 알림** 또는 **홈 화면 위젯**으로 항상 확인할 수 있어, 별도로 앱을 열지 않아도 소비 현황을 인식할 수 있습니다.

---

## 핵심 기능

### 알림 기반 자동 소비 감지
- `NotificationListenerService`를 통해 카드사·은행 앱의 결제 알림을 실시간으로 캐치
- 알림 텍스트를 파싱해 금액을 추출, 용돈에서 자동 차감
- 사용자가 직접 금액을 입력할 필요 없음

### 상태바 고정 알림 (토스 만보기 방식)
- 오늘 사용 가능한 잔여 금액을 상태바에 항상 표시
- 결제 알림이 감지될 때마다 즉시 업데이트
- 앱을 실행하지 않아도 소비 현황 파악 가능

### 홈 화면 위젯
- 오늘의 예산 / 사용 금액 / 잔여 금액을 위젯으로 제공
- 한눈에 소비 현황 확인

### 용돈 예산 설정
- 월/주/일 단위 예산 설정
- 설정한 주기에 맞춰 용돈 자동 초기화

---

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| Architecture | MVVM + UiState |
| DI | Hilt |
| Navigation | Navigation Compose (Type-safe) |
| Network | Retrofit, OkHttp, Kotlinx Serialization |
| Local DB | Room |
| 설정 저장 | Jetpack DataStore (Preferences) |
| 원격 설정 | Firebase Remote Config |
| 알림 감지 | NotificationListenerService |
| 위젯 | Glance (Jetpack) |
| Build | Gradle Kotlin DSL, Version Catalog |

---

## 모듈 구조

```
allowance-manager/
├── app/                        # Application, MainActivity, 네비게이션 진입점
│
├── feature/
│   ├── splash/                 # 스플래시 화면, 초기 데이터 로딩
│   ├── home/                   # 메인 홈 화면 (잔여 금액, 오늘 소비 내역)
│   └── setting/                # 예산 설정, 알림 파싱 규칙 관리, 앱 환경설정
│
└── core/
    ├── domain/                 # 비즈니스 로직 (UseCase, Repository 인터페이스, 도메인 모델)
    ├── data/                   # Repository 구현체, 데이터 소스 조합
    ├── remote/                 # Retrofit API, 네트워크 모듈
    ├── local/                  # Room Database, DAO
    ├── data-store/             # DataStore (사용자 설정, 예산 정보 저장)
    ├── config/                 # Firebase Remote Config (알림 파싱 규칙 원격 관리)
    └── ui/                     # BaseViewModel, 공통 UI 컴포넌트
```

### 모듈 의존성 방향

```
feature/* ──→ core:domain ←── core:data
                                  │
              ┌───────────────────┼───────────────────┐
              ↓                   ↓                   ↓
          core:remote         core:local        core:data-store
                                                core:config

feature/* ──→ core:ui            # BaseViewModel 상속
```

---

## 패키지 구조

```
com.allowance.manager
├── app
│   ├── AllowanceManagerApp.kt
│   ├── MainActivity.kt
│   └── navigation/
│       └── AppNavHost.kt
├── feature
│   ├── splash/
│   ├── home/
│   └── setting/
└── core
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   └── usecase/
    ├── data/
    ├── remote/
    ├── local/
    │   └── database/
    ├── datastore/
    └── config/
```

---

## 시작하기

### 요구사항
- Android Studio Hedgehog 이상
- JDK 17
- minSdk 26 (Android 8.0)

### 설정
1. Firebase 콘솔에서 프로젝트를 생성하고 `google-services.json`을 `app/` 폴더에 추가
2. `core/remote/NetworkModule.kt`의 `baseUrl`을 실제 API 주소로 교체
3. 앱 최초 실행 시 **알림 접근 권한** 허용 필요 (`설정 > 알림 > 알림 접근`)

---

## 주요 권한

| 권한 | 용도 |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | 시스템 알림 수신 |
| `FOREGROUND_SERVICE` | 상태바 고정 알림 유지 |
| `INTERNET` | 네트워크 통신 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 서비스 자동 시작 |
