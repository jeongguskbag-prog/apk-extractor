# ApkVault — 광고 없는 APK 추출·백업

휴대폰에 설치된 앱을 파일로 꺼내는 안드로이드 앱입니다.

- 단일 APK 앱 → `.apk`
- 분할 앱(base + split) → `.xapk` (ZIP 안에 각 APK, `manifest.json`, `icon.png`)
- 저장 위치: `다운로드/ApkExtractor/` · 공유 메뉴로 바로 보내기도 가능
- 큰 파일 나눠 저장(메뉴 옵션): 30MB 넘는 파일을 `이름.xapk.001`, `.002` … 로 30MB 씩 나눠 저장·공유 (Claude 등 AI 채팅의 파일 하나 30MB 업로드 제한에 맞춤) — 7-Zip·반디집의 "파일 합치기" 또는 윈도우 `copy /b 이름.xapk.001+이름.xapk.002 이름.xapk` 로 원래 파일 복원
- 조각 합치기(메뉴): 조각이 있는 폴더를 고르면 그 폴더의 첫 조각(`.001`)만 목록으로 보여 줌 — 고르면 나머지(.002 …)를 찾아 원래 이름으로 합쳐 저장. 폴더 선택(안드로이드 화면이라 모든 파일이 보임)은 처음 한 번만 — 다음부터는 메뉴를 누르면 바로 .001 목록이 뜸("다른 폴더"로 바꿀 수 있음). APK·XAPK 는 합친 뒤 ZIP 구조와 CRC 를 검사해 빠지거나 섞인 조각을 잡음
- 여러 앱 한꺼번에: 앱을 길게 눌러 선택 모드 → 모두 선택 · 저장 · 공유 (하나가 실패해도 나머지는 계속)
- **광고·통계·서버 연결 없음. 인터넷 권한 자체가 없음**

## 사용 설명서

### 앱 꺼내기
1. 목록에서 앱을 누르면 **저장** 또는 **공유**를 고를 수 있습니다.
2. 저장한 파일은 `다운로드/ApkExtractor/` 에 생깁니다. 분할 앱은 `.xapk`, 나머지는 `.apk` 입니다.
3. 여러 앱을 한꺼번에: 앱을 길게 눌러 선택 모드로 들어간 뒤 저장·공유합니다.

### 왜 30MB 씩 나눠 저장하나요?
Claude 같은 AI 채팅 앱은 **파일 하나에 30MB 까지만 올릴 수 있습니다.** 그보다 큰 APK·XAPK 를
올려 분석을 맡기려면 30MB 이하로 나눠야 합니다. 메일 첨부·메신저처럼 크기 제한이 있는 곳에 보낼 때도 같습니다.

1. 오른쪽 위 메뉴(⋮) → **큰 파일 30MB씩 나눠 저장** 을 켭니다. (꺼 두면 통째로 저장)
2. 30MB 가 넘는 앱은 `이름.apk.001`, `이름.apk.002` … 처럼 30MB 조각으로 저장·공유됩니다.
   30MB 이하인 앱은 나누지 않습니다.
3. 조각을 **모두** 올립니다. (Claude 에서는 조각 파일을 한 번에 여러 개 첨부)

### 조각 합치기 (원래 파일로 되돌리기)
1. 메뉴(⋮) → **조각 합치기**
2. 처음 한 번만: 안드로이드 폴더 선택 화면이 뜹니다. 이 화면은 이름으로 거를 수 없어 다른 파일도 보이지만,
   조각이 있는 폴더(예: `Download/ApkExtractor`)로 가서 **이 폴더 사용** 을 누르면 됩니다.
3. 그 폴더의 첫 조각(`.001`)만 목록으로 나옵니다. 합칠 파일을 누르면 나머지 조각을 찾아 합쳐
   `다운로드/ApkExtractor/` 에 원래 이름으로 저장합니다. APK·XAPK 는 합친 뒤 빠지거나 섞인 조각이 없는지 검사합니다.
4. 다음부터는 메뉴를 누르면 바로 `.001` 목록이 뜹니다. 다른 폴더의 조각은 목록 아래 **다른 폴더** 로 바꿉니다.
5. 컴퓨터에서는 7-Zip·반디집의 "파일 합치기" 또는 윈도우 `copy /b 이름.apk.001+이름.apk.002 이름.apk` 로 합칠 수 있습니다.

## 아이콘
- 앱: 적응형 아이콘(`drawable-anydpi-v26/ic_launcher.xml`) — 배경 `#1A56DB`, 금고 + 다이얼 속 아래 화살표, 안드로이드 13 테마(단색) 아이콘 포함
- Play 스토어용 512×512: `store/icon-512.png` · 모양별 미리보기: `store/icon-preview.png`
- Play 그래픽 이미지 1024×500 (24비트 PNG): `store/feature-graphic-ko.png`, `store/feature-graphic-en.png`

## 권한
| 권한 | 이유 |
|---|---|
| `QUERY_ALL_PACKAGES` | 설치된 모든 앱 목록 (Play 출시 시 사용 목적 신고 필요) |
| `WRITE_EXTERNAL_STORAGE` (안드로이드 9 이하만) | 다운로드 폴더 저장. 10 이상은 MediaStore 사용으로 권한 불필요 |

## 빌드
GitHub Actions(`.github/workflows/build.yml`)가 푸시·PR 마다 디버그 APK 를 만들고
Actions 실행 화면의 Artifacts 에 `ApkExtractor-debug` 로 올립니다. 자동 릴리스는 하지 않습니다.

로컬: Android Studio 로 열거나 `./gradlew assembleDebug`

## 출시용(서명) 빌드
- 패키지 이름: `com.jeongguskbag.apkvault` (Play 출시 후 변경 불가)
- 서명 정보는 저장소에 넣지 않고 GitHub Secrets 로만 받음:
  `APKVAULT_KEYSTORE_BASE64`, `APKVAULT_KEYSTORE_PASSWORD`, `APKVAULT_KEY_ALIAS`, `APKVAULT_KEY_PASSWORD`
- 만들기: Actions → **Release build** → Run workflow (또는 `v1.0.0` 같은 태그 푸시)
  → Artifacts 의 `ApkVault-release` 에 `app-release.aab`(Play 업로드용), `app-release.apk`, `mapping.txt`
- Play Console 에서 **Play 앱 서명**을 사용 — 이 키는 업로드 키라 잃어버려도 Play 고객센터에서 재설정 가능
- 로컬: 위 4개 값을 환경 변수 `APKVAULT_KEYSTORE_FILE`(키 파일 경로), `APKVAULT_KEYSTORE_PASSWORD`,
  `APKVAULT_KEY_ALIAS`, `APKVAULT_KEY_PASSWORD` 로 주고 `./gradlew bundleRelease`

## 개인정보처리방침
- `docs/privacy/index.html` (한국어·영어) — GitHub Pages: Settings → Pages → Deploy from a branch → `main` / `docs`
- 주소: https://jeongguskbag-prog.github.io/apk-extractor/privacy/

## 출시 전 할 일
- Play Console 에 `QUERY_ALL_PACKAGES` 사용 신고, 데이터 보안 양식(수집 없음)
- 유료 앱으로 생성(무료로 올린 뒤에는 유료로 바꿀 수 없음)
