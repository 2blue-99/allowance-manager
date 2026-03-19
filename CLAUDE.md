# Allowance Manager - Claude Context

시스템 알림을 감지해 자동으로 소비를 추적하고, 하루 사용 가능 금액을 실시간으로 알려주는 용돈 관리 안드로이드 앱.

## 핵심 특징

- `NotificationListenerService`로 카드사·은행 앱 결제 알림을 캐치해 자동 차감
- 상태바 고정 알림으로 잔여 금액 항상 표시 (토스 만보기 방식)
- 홈 화면 위젯 제공
- Firebase Remote Config로 알림 파싱 규칙 원격 관리

## 기술 스택

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material3
- **Architecture**: MVVM + UiState
- **DI**: Hilt
- **Navigation**: Navigation Compose (Type-safe, `@Serializable` routes)
- **Network**: Retrofit + OkHttp + Kotlinx Serialization
- **Local DB**: Room
- **설정 저장**: Jetpack DataStore (Preferences)
- **원격 설정**: Firebase Remote Config
- **위젯**: Glance (Jetpack)
- **Build**: Gradle Kotlin DSL, Version Catalog (`gradle/libs.versions.toml`)

## 모듈 구조

```
app/                        # Application, MainActivity, AppNavHost
feature/
  splash/                   # 스플래시
  home/                     # 메인 홈 (잔여 금액, 소비 내역)
  setting/                  # 예산 설정, 환경설정
core/
  domain/                   # UseCase, Repository 인터페이스, 도메인 모델
  data/                     # Repository 구현체
  remote/                   # Retrofit API
  local/                    # Room Database
  data-store/               # DataStore (사용자 설정)
  config/                   # Firebase Remote Config
  ui/                       # BaseViewModel, 공통 UI 컴포넌트
```

## 패키지명

`com.allowance.manager`

## SDK

- minSdk: 26 / targetSdk: 35 / compileSdk: 35
- Java: 17

## 주요 권한

- `BIND_NOTIFICATION_LISTENER_SERVICE` - 시스템 알림 수신
- `FOREGROUND_SERVICE` - 상태바 고정 알림 유지
- `RECEIVE_BOOT_COMPLETED` - 재부팅 후 서비스 자동 시작
