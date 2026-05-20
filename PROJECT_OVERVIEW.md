# GreenLink 프로젝트 구조 및 서비스 플로우

이 문서는 현재 코드베이스를 기준으로 GreenLink 프로젝트의 구성, 각 파일/패키지의 역할, 주요 서비스 흐름을 정리한 분석 문서다.

## 1. 프로젝트 개요

GreenLink는 식물 키우기 앱 기능과 IoT 재배 장치 제어 기능을 함께 제공하는 Spring Boot 백엔드다. 사용자는 회원가입/로그인 후 씨앗 아이템으로 식물을 생성하고, 출석/수확/급수 같은 활동으로 퀘스트를 진행한다. IoT 장치는 재배공간, 식물, 펌프 채널과 연결되며 센서 데이터를 서버로 전송한다. 서버는 센서 데이터를 저장한 뒤 자동 급수/조명 조건을 평가하고, Raspberry Pi가 처리할 명령을 큐 형태로 생성한다.

기술 스택은 다음과 같다.

- Java 17
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL
- AWS S3 SDK
- JJWT
- Lombok
- Gradle

## 2. 최상위 파일 역할

| 파일/디렉터리 | 역할 |
| --- | --- |
| `build.gradle` | Gradle 빌드 설정, Spring Boot 플러그인, Java 17 toolchain, JPA/Security/Web/Thymeleaf/MySQL/S3/JWT 의존성 선언 |
| `settings.gradle` | 루트 프로젝트명 `greenlink` 설정 |
| `gradlew`, `gradlew.bat` | Gradle Wrapper 실행 파일 |
| `gradle/wrapper/*` | Gradle Wrapper jar/properties |
| `HELP.md` | Spring Initializr가 생성한 기본 참고 문서 |
| `.gitignore`, `.gitattributes` | Git 추적 제외 및 속성 설정 |
| `src/main/java` | 백엔드 애플리케이션 Java 소스 |
| `src/main/resources` | Spring 설정, Thymeleaf 템플릿, 정적 리소스 |
| `src/test/java` | 테스트 소스 |
| `build/` | Gradle 빌드 산출물 |
| `.gradle/` | Gradle 로컬 캐시 |
| `.idea/` | IntelliJ IDEA 프로젝트 설정 |

주의: `src/main/resources/yaml/application-keys.yaml`에는 DB, AWS, OAuth 키가 평문으로 들어 있다. 운영/공유 저장소에서는 즉시 키를 폐기하고 환경변수나 secret manager로 이전해야 한다.

## 3. 리소스 설정

| 파일 | 역할 |
| --- | --- |
| `src/main/resources/application.yaml` | Spring 설정의 중심 파일. `application-keys.yaml` import, JPA `ddl-auto: update`, SQL 로그, multipart 용량, S3 bucket/region, JWT secret/만료시간 설정 |
| `src/main/resources/yaml/application-keys.yaml` | MySQL datasource, AWS access/secret key, Kakao/Google OAuth client 설정 |
| `src/main/resources/static/greenlink/theme-portal.css` | 사용자 포털 화면(`home`, `plants`, `quests`)의 커스텀 CSS |
| `src/main/resources/static/sb-admin/**` | SB Admin 기반 관리자 UI 정적 템플릿, Bootstrap, jQuery, FontAwesome, Chart.js, DataTables 등 외부 자산 |

## 4. 패키지 구성

```text
com.greenlink.greenlink
├── common          공통 응답, 공통 엔티티, 예외 처리
├── config          Security, S3 설정
├── controller      REST API 및 관리자 웹 컨트롤러
├── domain          JPA 엔티티와 enum
├── dto             요청/응답 DTO
├── repository      Spring Data JPA Repository
├── security        JWT 인증 및 UserDetails 구현
└── service         비즈니스 로직
```

## 5. 공통 계층

| 파일 | 역할 |
| --- | --- |
| `GreenlinkApplication.java` | Spring Boot 애플리케이션 진입점 |
| `common/ApiResponse.java` | API 공통 응답 포맷. 성공/실패 응답 생성 |
| `common/BaseEntity.java` | `createdAt`, `modifiedAt`, `deleted`를 가진 공통 mapped superclass. soft delete/restore 제공 |
| `common/GlobalExceptionHandler.java` | 전역 예외 처리. validation, 인증, 인가, request parameter/header 오류, 일반 예외를 JSON 응답으로 변환 |

## 6. 설정/보안 계층

| 파일 | 역할 |
| --- | --- |
| `config/S3Config.java` | AWS S3 client Bean 생성 |
| `config/SecurityConfig.java` | SecurityFilterChain 설정. JWT stateless, 공개 API, 관리자 권한, IoT device API 허용, admin redirect 처리 |
| `security/JwtTokenProvider.java` | JWT access token 생성/검증. token claim에 `userId`, `role`, email subject 저장 |
| `security/JwtAuthenticationFilter.java` | `Authorization: Bearer` 토큰 파싱 후 인증 객체를 SecurityContext에 저장 |
| `security/CustomUserDetails.java` | Spring Security `UserDetails` 구현. 사용자 id, email, password, role 보관 |
| `security/CustomUserDetailsService.java` | email 기반 사용자 조회 후 `CustomUserDetails` 생성 |

보안 정책 요약:

- `/api/auth/signup`, `/api/auth/login`, OAuth 로그인은 공개
- `/api/plants/**`, `/api/items/**`, `/api/quests/**`는 공개 조회
- `/api/iot/raspberry/**`, `/api/iot/esp/**`, `/api/iot/commands/**`, `/api/iot/plant-images`는 장치 API로 공개하되 서비스 계층에서 `X-DEVICE-KEY` 검증
- `/api/admin/**`, `/admin/**`는 ADMIN 권한 필요
- 그 외 API는 JWT 인증 필요
- `/api/ai/**`는 현재 공개 허용 상태

## 7. 도메인 모델

### 7.1 User

| 파일 | 역할 |
| --- | --- |
| `domain/user/User.java` | 사용자 엔티티. email, password, nickname, role, provider, providerId, profileImageUrl 관리 |
| `domain/user/UserRole.java` | `USER`, `ADMIN` |
| `domain/user/LoginProvider.java` | `LOCAL`, `KAKAO`, `GOOGLE` |

### 7.2 Plant

| 파일 | 역할 |
| --- | --- |
| `domain/plant/Plant.java` | 마스터 식물. 이름, 종, 설명, 이미지, 성장일수 |
| `domain/plant/UserPlant.java` | 사용자가 키우는 식물. 사용자/마스터 식물 연결, 닉네임, 성장 상태, 심은 날짜, 수확 날짜, 이미지 |
| `domain/plant/UserPlantStatus.java` | `GROWING`, `HARVESTABLE`, `HARVESTED` |

### 7.3 Item

| 파일 | 역할 |
| --- | --- |
| `domain/item/Item.java` | 마스터 아이템. 씨앗/화분/영양제. 씨앗은 `linkedPlant`로 식물과 연결 가능 |
| `domain/item/UserItem.java` | 사용자가 보유한 아이템. 보유/장착/사용 상태 및 연결 식물 관리 |
| `domain/item/ItemType.java` | `SEED`, `POT`, `NUTRIENT` |
| `domain/item/UserItemStatus.java` | `OWNED`, `EQUIPPED`, `USED` |

### 7.4 Quest

| 파일 | 역할 |
| --- | --- |
| `domain/quest/Quest.java` | 마스터 퀘스트. 타입, 목표 타입, 목표값, 보상 아이템, 보상 수량, reset cycle, active 관리 |
| `domain/quest/UserQuest.java` | 사용자별 퀘스트 진행 상태. 진행값, 상태, 시작/완료/보상 시각 |
| `domain/quest/QuestType.java` | `DAILY`, `WEEKLY`, `MONTHLY`, `ACHIEVEMENT` |
| `domain/quest/ResetCycle.java` | `DAILY`, `WEEKLY`, `MONTHLY`, `NONE` |
| `domain/quest/TargetType.java` | `ATTEND`, `WATERING`, `GROW_PLANT`, `HARVEST` |
| `domain/quest/UserQuestStatus.java` | `IN_PROGRESS`, `ACHIEVABLE`, `COMPLETED`, `EXPIRED` |

### 7.5 Attend

| 파일 | 역할 |
| --- | --- |
| `domain/attend/Attend.java` | 사용자 출석 기록. 사용자, 날짜, 생성시각, 연속 출석 수. 사용자+날짜 unique |

### 7.6 IoT

| 파일 | 역할 |
| --- | --- |
| `domain/iot/GrowSpace.java` | 재배공간. 이름, 설명, active/deleted, 생성/수정 시각 |
| `domain/iot/GrowSpacePlant.java` | 재배공간과 사용자 식물 연결 |
| `domain/iot/IotDevice.java` | IoT 기기. Raspberry Pi 또는 ESP32. deviceKey, 재배공간, 사용자 식물, active, lastConnectedAt |
| `domain/iot/PumpChannel.java` | 특정 식물의 펌프 채널. 재배공간, 식물, Raspberry Pi, GPIO pin, relay channel |
| `domain/iot/RaspberrySensorData.java` | Raspberry Pi 환경 센서 데이터. 온도, 습도, 조도, 측정시각 |
| `domain/iot/EspSensorData.java` | ESP32 토양수분 데이터. raw/percent, 측정시각 |
| `domain/iot/PlantImage.java` | Raspberry Pi가 업로드한 식물 이미지. 원본 파일명, 이미지 URL, 촬영시각 |
| `domain/iot/DeviceCommand.java` | Raspberry Pi가 처리할 명령 큐. WATER/LIGHT_ON/LIGHT_OFF 및 상태 전이 |
| `domain/iot/DeviceType.java` | `RASPBERRY_PI`, `ESP32` |
| `domain/iot/CommandType.java` | `WATER`, `LIGHT_ON`, `LIGHT_OFF` |
| `domain/iot/CommandStatus.java` | `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `CANCELLED` |

### 7.7 Automation

| 파일 | 역할 |
| --- | --- |
| `domain/automation/AutomationSetting.java` | 사용자 식물별 자동화 설정. 자동 급수/조명 on/off, 기준값, 쿨다운, 시간대, 학습 최적화 설정 |
| `domain/automation/AutomationLog.java` | 자동화 실행/스킵 로그. 트리거 타입, 값, 기준값, 연결 명령, 메시지 |
| `domain/automation/AutomationModel.java` | 자동화 학습 모델. 추천 급수/조명 기준, 데이터 수, 신뢰도, 학습 기간 |
| `domain/automation/AutomationDecisionMode.java` | `RULE_BASED`, `LEARNING_BASED`, `HYBRID` |
| `domain/automation/AutomationType.java` | `AUTO_WATER`, `AUTO_LIGHT_ON`, `AUTO_LIGHT_OFF`, `SKIP_WATER`, `SKIP_LIGHT` |
| `domain/automation/AutomationModelStatus.java` | `INSUFFICIENT_DATA`, `READY`, `FAILED` |
| `domain/automation/TriggerSensorType.java` | 토양수분, 조도, 시간, 쿨다운, 중복 명령, 비활성, 장치 미준비 등 자동화 판단 사유 |

### 7.8 AI/Notification

| 파일 | 역할 |
| --- | --- |
| `domain/ai/AiPlantImage.java` | 원본 식물 이미지에 연결된 AI 결과 이미지 |
| `domain/ai/AiImageStatus.java` | `SUCCESS`, `FAILED` |
| `domain/notification/UserFcmToken.java` | 사용자 FCM 토큰. 사용자, 토큰, 플랫폼, deleted, 생성/수정 시각 |

## 8. Repository 계층

모든 Repository는 Spring Data JPA 기반이며, 대부분 `deleted=false`, `active=true`, 최신순 조회, 사용자 소유 검증용 finder를 제공한다.

| 파일 | 역할 |
| --- | --- |
| `UserRepository.java` | email, provider/providerId 기반 사용자 조회, email 중복 확인 |
| `PlantRepository.java` | 식물 마스터 조회, 이름 중복 확인 |
| `UserPlantRepository.java` | 사용자 식물 목록/상세/상태 조회, 사용자 소유 검증 |
| `ItemRepository.java` | 아이템 마스터 조회, 이름 중복 확인 |
| `UserItemRepository.java` | 사용자 아이템 목록, 타입/상태 필터, 보유/사용/장착 카운트, 장착 화분 조회 |
| `QuestRepository.java` | 활성 퀘스트, 타입/목표 타입별 조회 |
| `UserQuestRepository.java` | 사용자 퀘스트 조회, 현재 기간 퀘스트 존재 확인, 업적 퀘스트 조회 |
| `AttendRepository.java` | 날짜별 출석 조회, 월별 출석 목록, 최근 출석 조회 |
| `GrowSpaceRepository.java` | 재배공간 조회, 이름 중복 확인 |
| `GrowSpacePlantRepository.java` | 재배공간-식물 연결 조회, 연결 여부 확인 |
| `IotDeviceRepository.java` | deviceKey 기반 장치 조회, Raspberry/ESP 조회, active/deleted 필터 |
| `PumpChannelRepository.java` | 식물/재배공간별 펌프 채널 조회, 중복 연결 확인 |
| `RaspberrySensorDataRepository.java` | 재배공간별 최신/기간별 환경 센서 데이터 조회 |
| `EspSensorDataRepository.java` | 식물별 최신/기간별 토양수분 데이터 조회, 급수 전후 학습 데이터 조회 |
| `PlantImageRepository.java` | 식물/재배공간별 최신 이미지 및 이미지 목록 조회 |
| `DeviceCommandRepository.java` | pending 명령 조회, 중복 명령 확인, 최근 명령 조회, 기간별 명령 조회 |
| `AutomationSettingRepository.java` | 식물별 자동화 설정 조회/존재 확인 |
| `AutomationLogRepository.java` | 자동화 로그 최신순 조회 |
| `AutomationModelRepository.java` | 최신 학습 모델, READY 모델 조회 |
| `AiPlantImageRepository.java` | 원본 이미지별 최신 AI 이미지 조회 |
| `UserFcmTokenRepository.java` | 사용자+FCM 토큰 조회 |

## 9. DTO 계층

| 파일 | 역할 |
| --- | --- |
| `AuthDto.java` | 회원가입, 로그인, OAuth 로그인 요청/응답 |
| `UserDto.java` | 내 정보 조회, 닉네임 수정 요청/응답 |
| `PlantDto.java` | 식물 마스터 목록/상세 응답 |
| `UserPlantDto.java` | 내 식물 생성/목록/상세/닉네임 변경/수확 응답 |
| `ItemDto.java` | 아이템 마스터 목록/상세 응답 |
| `UserItemDto.java` | 사용자 아이템 목록, 화분 장착/해제, 영양제 사용 DTO |
| `QuestDto.java` | 마스터 퀘스트 및 사용자 퀘스트 목록/상세/보상 응답 |
| `AttendDto.java` | 출석 등록, 월별 출석 응답 |
| `HomeDto.java` | 홈 화면 데이터 응답 |
| `CollectionDto.java` | 컬렉션 목록/상세 응답 |
| `AutomationDto.java` | 자동화 설정, 로그, 모델 학습 요청/응답 |
| `AdminDto.java` | 관리자 생성 API/화면용 식물, 아이템, 퀘스트, IoT 기기 DTO |
| `FcmTokenDto.java` | FCM 토큰 저장 요청/응답 |
| `dto/iot/IotSetupDto.java` | 재배공간, 식물 연결, 기기, 펌프 채널 설정 DTO |
| `dto/iot/IotDeviceDto.java` | IoT 장치 데이터 업로드, pending command, command 처리 DTO |
| `dto/iot/IotAppDto.java` | 앱용 최신 IoT 상태, 이미지, 물/조명 명령 응답 |
| `dto/ai/AiPlantImageDto.java` | AI 이미지 결과 저장 요청/응답 |

## 10. Service 계층

| 파일 | 역할 |
| --- | --- |
| `AuthService.java` | 회원가입/로그인/OAuth 사용자 생성, JWT 발급, 기본 씨앗/화분 지급, 업적 퀘스트 생성 |
| `UserService.java` | 내 정보 조회, 닉네임 수정 |
| `PlantService.java` | 식물 마스터 목록/상세 조회 |
| `UserPlantService.java` | 씨앗 아이템 사용으로 내 식물 생성, 목록/상세, 닉네임 수정, 수확, 수확 퀘스트 진행 |
| `ItemService.java` | 아이템 마스터 목록/상세 조회 |
| `UserItemService.java` | 내 아이템 목록, 화분 장착/해제, 영양제 사용 |
| `QuestService.java` | 마스터 퀘스트 목록/상세 조회 |
| `UserQuestService.java` | 사용자 퀘스트 생성/조회/만료/보상 지급 |
| `QuestProgressService.java` | 출석/급수/성장/수확 이벤트에 따른 퀘스트 진행도 증가 |
| `AttendService.java` | 오늘 출석, 월별 출석 조회, streak 계산, 출석 퀘스트 진행 |
| `HomeService.java` | 홈 화면용 대표 식물/요약 데이터 구성 |
| `CollectionService.java` | 사용자의 식물 컬렉션 목록/상세 구성 |
| `IotSetupService.java` | 재배공간 생성, 식물 연결, IoT 기기 등록, 펌프 채널 생성 |
| `IotDeviceDataService.java` | 장치 인증 후 Raspberry/ESP 센서 데이터 저장, 이미지 S3 업로드, 자동화 평가 호출 |
| `IotAppService.java` | 앱에서 내 식물 최신 IoT 상태/이미지 조회, 수동 급수/조명 명령 생성 |
| `IotCommandService.java` | Raspberry Pi pending 명령 조회, processing/success/failed 상태 변경 |
| `AutomationService.java` | 자동화 설정/로그 조회, 센서 데이터 기반 자동 급수/조명 판단 및 명령 생성 |
| `AutomationLearningService.java` | 최근 14일 센서/명령 데이터를 기반으로 자동화 모델 학습, 추천 기준값 산출 |
| `AiPlantImageService.java` | 원본 PlantImage에 AI 결과 이미지 저장 |
| `S3UploadService.java` | 이미지 파일 검증, S3 업로드, URL 생성 |
| `FcmTokenService.java` | 사용자 FCM 토큰 저장/재활성화 |
| `AdminService.java` | 관리자용 식물/아이템/퀘스트/IoT 기기 생성, 사용자/마스터 데이터 목록 조회, 권한 토글, 삭제 |
| `oauth/OAuthLoginService.java` | Kakao/Google OAuth 로그인 통합 처리 |
| `oauth/KakaoOAuthClient.java` | Kakao code로 access token/user info 조회 |
| `oauth/GoogleOAuthClient.java` | Google code로 access token/user info 조회 |
| `oauth/OAuthUserInfo.java` | OAuth 공급자별 사용자 정보 공통 모델 |

## 11. Controller 계층

### 11.1 인증/사용자

| 파일 | 매핑 | 역할 |
| --- | --- | --- |
| `AuthController.java` | `/api/auth` | 회원가입, 로그인, Kakao/Google OAuth 로그인 |
| `UserController.java` | `/api/users` | 내 정보 조회, 닉네임 수정 |
| `FcmTokenController.java` | `/api/users/me/fcm-token` | 내 FCM 토큰 저장 |

### 11.2 식물/아이템/퀘스트

| 파일 | 매핑 | 역할 |
| --- | --- | --- |
| `PlantController.java` | `/api/plants` | 식물 마스터 목록/상세 공개 조회 |
| `UserPlantController.java` | `/api/user-plants` | 내 식물 생성/목록/상세/수정/수확 |
| `ItemController.java` | `/api/items` | 아이템 마스터 목록/상세 공개 조회 |
| `UserItemController.java` | `/api/user-items` | 내 아이템 조회, 화분 장착/해제, 영양제 사용 |
| `QuestController.java` | `/api/quests` | 마스터 퀘스트 목록/상세 공개 조회 |
| `UserQuestController.java` | `/api/user-quests` | 내 퀘스트 목록/상세/보상 수령 |
| `AttendController.java` | `/api/attends` | 오늘 출석, 내 월별 출석 조회 |
| `CollectionController.java` | `/api/collections` | 내 식물 컬렉션 목록/상세 |
| `HomeController.java` | `/api/home` | 홈 화면 데이터 |

### 11.3 IoT/자동화/AI

| 파일 | 매핑 | 역할 |
| --- | --- | --- |
| `IotSetupController.java` | `/api/iot` | 재배공간, 식물 연결, 기기, 펌프 채널 설정 |
| `IotDeviceController.java` | `/api/iot` | Raspberry/ESP 데이터 수신, 이미지 업로드, command polling/처리 |
| `IotAppController.java` | `/api/user-plants/{userPlantId}/iot` | 앱용 최신 IoT 상태, 식물 이미지, 수동 물/조명 명령 |
| `AutomationController.java` | `/api/user-plants/{userPlantId}/automation` | 자동화 설정/로그/학습/모델 조회 |
| `AiPlantImageController.java` | `/api/ai/plant-images` | AI 결과 이미지 저장 |

### 11.4 관리자

| 파일 | 매핑 | 역할 |
| --- | --- | --- |
| `AdminController.java` | `/api/admin` | 관리자 REST API. 식물/아이템/퀘스트 생성 |
| `AdminWebController.java` | `/admin` | Thymeleaf 관리자 웹. 로그인 화면, 대시보드, 사용자/식물/아이템/퀘스트/IoT 목록 및 생성 |

## 12. 템플릿 파일 역할

### 12.1 사용자 포털

| 파일 | 역할 |
| --- | --- |
| `templates/home.html` | 사용자용 GreenLink 홈/랜딩 성격의 포털 화면 |
| `templates/plants.html` | 사용자용 식물 화면 |
| `templates/quests.html` | 사용자용 퀘스트 화면 |
| `templates/blank.html` | SB Admin 기반 빈 템플릿 |

### 12.2 관리자 공통 조각

| 파일 | 역할 |
| --- | --- |
| `templates/includes/admin/head.html` | 관리자 HTML head 공통 조각 |
| `templates/includes/admin/sidebar.html` | 관리자 사이드바 |
| `templates/includes/admin/topper.html` | 관리자 상단바 |
| `templates/includes/admin/footer.html` | 관리자 footer |
| `templates/includes/admin/script.html` | 관리자 공통 JS include |
| `templates/includes/admin/logout.html` | 로그아웃 모달 |

### 12.3 관리자 화면

| 파일 | 역할 |
| --- | --- |
| `templates/admin/login.html` | 관리자 로그인 화면 |
| `templates/admin/index.html` | 관리자 대시보드 |
| `templates/admin/user/list.html` | 사용자 목록 |
| `templates/admin/user/detail.html` | 사용자 상세 |
| `templates/admin/plant/list.html` | 식물 마스터 목록 |
| `templates/admin/plant/create.html` | 식물 마스터 생성 |
| `templates/admin/item/list.html` | 아이템 마스터 목록 |
| `templates/admin/item/create.html` | 아이템 마스터 생성 |
| `templates/admin/quest/list.html` | 퀘스트 마스터 목록 |
| `templates/admin/quest/create.html` | 퀘스트 마스터 생성 |
| `templates/admin/iot/list.html` | IoT 기기 목록 |
| `templates/admin/iot/create.html` | IoT 기기 생성 |
| `templates/admin/notice/*` | 공지 화면 템플릿. 현재 컨트롤러 직접 매핑은 확인되지 않음 |
| `templates/admin/faq/*` | FAQ 화면 템플릿. 현재 컨트롤러 직접 매핑은 확인되지 않음 |
| `templates/admin/role/*` | 권한/역할 화면 템플릿. 현재 컨트롤러 직접 매핑은 확인되지 않음 |

## 13. 핵심 서비스 플로우

### 13.1 회원가입 및 초기 지급

```text
POST /api/auth/signup
→ AuthController.signup
→ AuthService.signup
→ User 생성 및 저장
→ "바질 씨앗", "기본 화분" UserItem 지급
→ 활성 ACHIEVEMENT Quest에 대한 UserQuest 생성
→ SignupResDto 반환
```

로그인은 다음과 같다.

```text
POST /api/auth/login
→ email로 User 조회
→ BCrypt password 검증
→ JwtTokenProvider.createAccessToken
→ LoginResDto 반환
```

OAuth 로그인은 `OAuthLoginService`가 공급자별 client를 호출하고, `AuthService.oauthLogin`이 기존 사용자 조회 또는 신규 사용자 생성을 수행한다.

### 13.2 식물 생성

```text
POST /api/user-plants
→ JWT 인증 사용자 확인
→ UserPlantService.createUserPlant
→ 요청의 userItemId로 내 UserItem 조회
→ UserItem이 OWNED 상태의 SEED인지 검증
→ seed item의 linkedPlant 조회
→ UserPlant 생성
→ seed UserItem.useSeed()
→ CreateResDto 반환
```

식물 목록/상세 조회 시 현재 날짜 기준으로 `refreshHarvestableStatus`가 호출되어 성장일수가 충족되면 수확 가능 상태가 된다.

### 13.3 수확 및 퀘스트 진행

```text
POST /api/user-plants/{userPlantId}/harvest
→ 내 식물 조회
→ 수확 가능 여부 확인
→ UserPlant.harvest
→ QuestProgressService.increaseProgress(user, HARVEST, 1)
→ 관련 퀘스트 진행도 증가
```

출석도 같은 구조로 `TargetType.ATTEND` 진행도를 올린다.

### 13.4 사용자 퀘스트 조회/보상

```text
GET /api/user-quests
→ UserQuestService.getUserQuests
→ 현재 날짜 기준 active quest에 대한 UserQuest가 없으면 생성
→ 과거 기간형 퀘스트는 EXPIRED 처리 가능
→ 현재 기간 퀘스트와 업적 퀘스트만 목록 응답
```

```text
POST /api/user-quests/{userQuestId}/reward
→ ACHIEVABLE 상태인지 확인
→ 현재 기간 퀘스트인지 확인
→ rewardItem을 rewardQuantity만큼 UserItem으로 생성
→ UserQuest.completeReward()
```

### 13.5 IoT 세팅

```text
POST /api/iot/grow-spaces
→ GrowSpace 생성

POST /api/iot/grow-spaces/{growSpaceId}/plants
→ 내 UserPlant를 GrowSpace에 연결

POST /api/iot/devices
→ RASPBERRY_PI: growSpaceId 필수, userPlantId 불가
→ ESP32: userPlantId 필수, growSpaceId가 있으면 해당 식물이 공간에 연결되어야 함

POST /api/iot/pump-channels
→ GrowSpace + UserPlant + RaspberryDevice 검증
→ 식물당 하나의 PumpChannel 생성
```

### 13.6 IoT 데이터 수집

Raspberry Pi 환경 데이터:

```text
POST /api/iot/raspberry/environment
Header: X-DEVICE-KEY
→ deviceKey로 활성 Raspberry Pi 조회
→ GrowSpace 확인
→ RaspberrySensorData 저장
→ device.lastConnectedAt 갱신
→ AutomationService.evaluateAutoLight 호출
```

ESP32 토양수분 데이터:

```text
POST /api/iot/esp/soil-moisture
Header: X-DEVICE-KEY
→ deviceKey로 활성 ESP32 조회
→ 연결된 UserPlant 확인
→ EspSensorData 저장
→ device.lastConnectedAt 갱신
→ AutomationService.evaluateAutoWater 호출
```

Raspberry Pi 이미지 업로드:

```text
POST /api/iot/plant-images
Header: X-DEVICE-KEY
multipart file
→ 활성 Raspberry Pi 검증
→ S3UploadService.uploadUserPlantImage
→ PlantImage 저장
```

### 13.7 앱에서 IoT 상태 조회 및 수동 명령

```text
GET /api/user-plants/{userPlantId}/iot/latest
→ 내 식물 확인
→ 연결 GrowSpace 조회
→ 최신 RaspberrySensorData 조회
→ 최신 EspSensorData 조회
→ 최신 PlantImage 조회
→ 해당 원본 이미지의 최신 AiPlantImage 조회
→ IotLatestResDto 반환
```

수동 물주기:

```text
POST /api/user-plants/{userPlantId}/iot/water
→ 내 식물 및 GrowSpace 조회
→ PumpChannel 확인
→ pending/processing WATER 명령 중복 확인
→ DeviceCommand(WATER, PENDING) 생성
```

수동 조명 ON/OFF:

```text
POST /api/user-plants/{userPlantId}/iot/light/on
POST /api/user-plants/{userPlantId}/iot/light/off
→ GrowSpace의 Raspberry Pi 조회
→ pending/processing 조명 명령 중복 확인
→ DeviceCommand(LIGHT_ON 또는 LIGHT_OFF, PENDING) 생성
```

### 13.8 Raspberry Pi 명령 처리

```text
GET /api/iot/commands/pending
Header: X-DEVICE-KEY
→ 활성 Raspberry Pi 검증
→ 해당 기기의 PENDING 명령 목록 반환

PATCH /api/iot/commands/{commandId}/processing
→ PENDING → PROCESSING

PATCH /api/iot/commands/{commandId}/complete
→ success=true  : SUCCESS
→ success=false : FAILED
```

### 13.9 자동 급수 판단

ESP32 토양수분 저장 직후 실행된다.

```text
EspSensorData 저장
→ AutomationService.evaluateAutoWater
→ UserPlant/User/GrowSpace 확인
→ AutomationSetting 없으면 기본 생성
→ decisionMode에 따라 기준값 결정
   - RULE_BASED: 설정 기준값
   - HYBRID/LEARNING_BASED: 사용 가능한 READY 모델이 있으면 추천값, 없으면 설정 기준값
→ autoWaterEnabled 확인
→ soilMoisturePercent <= threshold인지 확인
→ 실행 중 명령/쿨다운 확인
→ Raspberry Pi/PumpChannel 확인
→ DeviceCommand(WATER) 생성
→ AutomationLog(AUTO_WATER) 저장
```

조건을 만족하지 못하면 `AutomationLog(SKIP_WATER)`가 저장된다.

### 13.10 자동 조명 판단

Raspberry 환경 데이터 저장 직후 실행된다.

```text
RaspberrySensorData 저장
→ AutomationService.evaluateAutoLight
→ GrowSpace의 활성 Raspberry Pi 조회
→ GrowSpace에 연결된 모든 UserPlant 조회
→ 각 UserPlant별 AutomationSetting 확인
→ autoLightEnabled 확인
→ 현재 시간이 lightStartTime~lightEndTime 범위 밖이면 LIGHT_OFF 후보
→ 조도 <= lightOnThreshold이면 LIGHT_ON 후보
→ 조도 >= lightOffThreshold이면 LIGHT_OFF 후보
→ 중복 명령/쿨다운 확인
→ DeviceCommand 생성
→ AutomationLog 저장
```

### 13.11 자동화 학습

```text
POST /api/user-plants/{userPlantId}/automation/train
→ AutomationLearningService.trainUserPlantModel
→ 최근 14일 EspSensorData 조회
→ 최근 14일 WATER DeviceCommand 조회
→ 연결 GrowSpace의 RaspberrySensorData 조회
→ soilDataCount가 minLearningDataCount보다 작으면 INSUFFICIENT_DATA 모델 저장
→ 충분하면:
   - 평균 토양 건조 속도 계산
   - 물주기 직전/후 회복량 계산
   - 추천 급수 기준값 계산
   - 조도 p25/p75 기반 추천 ON/OFF 기준값 계산
   - confidenceScore 계산
   - READY 모델 저장
→ autoOptimizeEnabled=true이고 confidence가 충분하면 AutomationSetting 기준값에 반영
```

## 14. 관리자 웹 흐름

관리자 웹은 Thymeleaf + SB Admin 기반이다.

```text
GET /admin/login
→ login template

GET /admin
→ dashboard template

GET /admin/users
→ AdminService.getAllUsers
→ user/list template

POST /admin/users/{id}/toggle-role
→ User.toggleRole

POST /admin/users/{id}/delete
→ User soft delete

GET/POST /admin/plants
→ Plant master 목록/생성

GET/POST /admin/items
→ Item master 목록/생성

GET/POST /admin/quests
→ Quest master 목록/생성

GET/POST /admin/iot
→ IoT device 목록/생성
```

현재 SecurityConfig는 `/admin/**`에 ADMIN 권한을 요구하지만 세션 기반 form login은 비활성화되어 있다. 따라서 실제 관리자 웹 접근은 JWT 인증을 어떻게 브라우저에서 전달할지 별도 확인이 필요하다.

## 15. 테스트 구성

| 파일 | 역할 |
| --- | --- |
| `src/test/java/com/greenlink/greenlink/GreenlinkApplicationTests.java` | Spring context load 테스트 1개 |

현재 핵심 비즈니스 흐름에 대한 단위/통합 테스트는 부족하다. 우선순위가 높은 테스트 대상은 다음과 같다.

- 회원가입 시 기본 아이템/업적 퀘스트 지급
- 씨앗 사용으로 식물 생성 및 씨앗 상태 변경
- 출석/수확 이벤트의 퀘스트 진행도 증가
- IoT deviceKey 검증과 센서 저장
- 자동 급수/조명 명령 생성 조건
- 중복 명령 및 쿨다운 방지
- 퀘스트 보상 지급

## 16. 현재 구조상 주요 리스크

1. Secret 평문 저장
   - DB password, AWS key, OAuth secret이 `application-keys.yaml`에 존재한다.
   - Git에 올라간 적이 있다면 키 폐기/재발급이 필요하다.

2. AI 결과 저장 API 공개
   - `/api/ai/**`가 `permitAll`이다.
   - 외부 AI 서버 전용 secret header, 서명, 내부망 제한 등 보호 장치가 필요하다.

3. IoT API 공개 범위
   - 장치 API는 service 계층에서 `X-DEVICE-KEY`로 검증하지만 endpoint 자체는 공개다.
   - deviceKey 유출 시 명령 조회/상태 변경이 가능하므로 rotation, scope, rate limit이 필요하다.

4. 관리자 웹 인증 흐름
   - `/admin/login` 화면은 있으나 Spring form login은 꺼져 있다.
   - 브라우저 기반 관리자 인증이 실제로 어떻게 동작하는지 확인해야 한다.

5. 테스트 부족
   - 현재 테스트는 context load 하나다.
   - 자동화/IoT/퀘스트처럼 상태 전이가 많은 로직은 회귀 위험이 높다.

6. soft delete 일관성
   - 많은 조회가 `deleted=false`를 사용하지만, 일부 서비스는 `findById` 후 filter 또는 직접 id 검증을 섞어 쓴다.
   - 중요 도메인은 repository 조회 정책을 통일하는 편이 안전하다.

## 17. 전체 서비스 진행 흐름 요약

```text
관리자
→ 식물/아이템/퀘스트/IoT 기기 마스터 데이터 준비

사용자
→ 회원가입/로그인
→ 기본 씨앗/화분 수령
→ 씨앗으로 내 식물 생성
→ 출석/아이템 사용/식물 성장/수확
→ 퀘스트 진행 및 보상 수령
→ 컬렉션/홈/식물 상태 조회

IoT 세팅
→ 재배공간 생성
→ 내 식물을 재배공간에 연결
→ Raspberry Pi를 재배공간에 연결
→ ESP32를 식물에 연결
→ 펌프 채널을 식물+Raspberry Pi에 연결

IoT 운영
→ Raspberry Pi가 환경 데이터/이미지 전송
→ ESP32가 토양수분 전송
→ 서버가 최신 상태 저장
→ 자동화 조건 평가
→ 필요한 경우 DeviceCommand 생성
→ Raspberry Pi가 pending command polling
→ 실제 펌프/조명 제어
→ command 결과 서버에 보고

자동화 고도화
→ 최근 센서/명령 데이터 기반 모델 학습
→ 추천 기준값과 confidence 저장
→ 설정에 따라 자동화 기준값에 학습 결과 반영
```

