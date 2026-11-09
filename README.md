# ApkVault — 광고 없는 APK 추출·백업

휴대폰에 설치된 앱을 파일로 꺼내는 안드로이드 앱입니다.

- 단일 APK 앱 → `.apk`
- 분할 앱(base + split) → `.xapk` (ZIP 안에 각 APK, `manifest.json`, `icon.png`)
- 저장 위치: `다운로드/ApkExtractor/` · 공유 메뉴로 바로 보내기도 가능
- 큰 파일 나눠 저장(메뉴 옵션): 30MB 넘는 파일을 `이름.xapk.001`, `.002` … 로 30MB 씩 나눠 저장·공유 — 7-Zip·반디집의 "파일 합치기" 또는 윈도우 `copy /b 이름.xapk.001+이름.xapk.002 이름.xapk` 로 원래 파일 복원
- 조각 합치기(메뉴): `.001`, `.002` … 조각을 모두 고르면 번호순으로 이어 원래 이름으로 저장. APK·XAPK 는 합친 뒤 ZIP 구조와 CRC 를 검사해 빠지거나 섞인 조각을 잡음
- 여러 앱 한꺼번에: 앱을 길게 눌러 선택 모드 → 모두 선택 · 저장 · 공유 (하나가 실패해도 나머지는 계속)
- **광고·통계·서버 연결 없음. 인터넷 권한 자체가 없음**

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
