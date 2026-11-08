# APK 추출기 (APK Extractor)

휴대폰에 설치된 앱을 파일로 꺼내는 안드로이드 앱입니다.

- 단일 APK 앱 → `.apk`
- 분할 앱(base + split) → `.xapk` (ZIP 안에 각 APK, `manifest.json`, `icon.png`)
- 저장 위치: `다운로드/ApkExtractor/` · 공유 메뉴로 바로 보내기도 가능
- **광고·통계·서버 연결 없음. 인터넷 권한 자체가 없음**

## 권한
| 권한 | 이유 |
|---|---|
| `QUERY_ALL_PACKAGES` | 설치된 모든 앱 목록 (Play 출시 시 사용 목적 신고 필요) |
| `WRITE_EXTERNAL_STORAGE` (안드로이드 9 이하만) | 다운로드 폴더 저장. 10 이상은 MediaStore 사용으로 권한 불필요 |

## 빌드
GitHub Actions(`.github/workflows/build.yml`)가 푸시·PR 마다 디버그 APK 를 만들고
Actions 실행 화면의 Artifacts 에 `ApkExtractor-debug` 로 올립니다. 자동 릴리스는 하지 않습니다.

로컬: Android Studio 로 열거나 `./gradlew assembleDebug`

## 출시 전 할 일
- 정식 서명 키로 릴리스 빌드(AAB) 만들기 (키는 저장소에 넣지 말 것)
- Play Console 에서 `QUERY_ALL_PACKAGES` 사용 신고
- 유료 판매 방식 결정 (유료 앱 또는 인앱 결제)
