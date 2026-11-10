# ApkVault — Play 스토어 등록 문구 (v0.6.0)

Play Console → 앱 → 스토어 등록정보 → 기본 스토어 등록정보 에 붙여넣기.
(기본 언어 한국어 + "번역 관리 → 직접 번역 추가"로 영어(미국) en-US, 일본어 ja-JP, 중국어(간체) zh-CN)

> 가격은 설명문에 적지 않습니다. 나라마다 다르고 나중에 바뀔 수 있어서, Play 스토어가 버튼에 자동으로 보여 줍니다.
> 제목·아이콘에 "할인", "무료" 같은 가격·홍보 문구를 넣으면 정책 위반이라 넣지 않았습니다.

## 1. 앱 이름 (30자 이내)
- 한국어: ApkVault - 광고 없는 APK 추출·백업
- English: ApkVault - Ad-free APK Backup
- 日本語: ApkVault - 広告なしの APK 抽出・バックアップ
- 简体中文: ApkVault - 无广告 APK 提取与备份

## 2. 간단한 설명 (80자 이내)
- 한국어: 설치된 앱을 APK·XAPK로 저장·공유. 광고·추적 없음, 인터넷 권한 없음. 한 번 구매로 끝.
- English: Save and share installed apps as APK/XAPK. No ads, no tracking, no internet.
- 日本語: インストール済みアプリを APK・XAPK で保存・共有。広告・追跡なし、インターネット権限なし。買い切り。
- 简体中文: 将已安装应用保存并分享为 APK/XAPK。无广告、无跟踪、无网络权限。一次购买。

## 3. 자세한 설명 (4,000자 이내)

### 한국어
휴대폰에 설치된 앱을 파일로 꺼내 보관하고, 다른 기기로 옮기고, 필요한 곳에 보내세요.
ApkVault는 광고도, 추적도, 구독도 없는 APK 추출·백업 앱입니다. 한 번 구매하면 모든 기능을 계속 씁니다.

■ 이런 분께 필요해요
• 지금 버전의 앱을 백업해 두고 싶을 때 (업데이트 전 보관)
• 스토어에서 사라진 앱을 다른 기기로 옮기고 싶을 때
• 크기 제한이 있는 곳(AI 채팅, 메일, 메신저)에 큰 앱 파일을 올려야 할 때

■ 주요 기능
• 설치된 앱을 APK로 저장 – 다운로드/ApkExtractor 폴더
• 분할 앱(여러 파일로 나뉜 앱)은 XAPK 하나로 묶어 저장 – 각 APK, manifest.json, 아이콘 포함
• 여러 앱 한꺼번에 – 앱을 길게 눌러 선택하고 한 번에 저장·공유
• 공유 – 카카오톡, 메일, 드라이브 등으로 바로 보내기
• 검색, 시스템 앱 표시, 이름·최근 업데이트·크기 순 정렬

■ 큰 파일 30MB씩 나눠 저장
• Claude 같은 AI 채팅 앱은 파일 하나에 30MB까지만 올릴 수 있습니다. 메뉴에서 이 옵션을 켜면 30MB가 넘는 앱을 .001, .002 … 조각으로 나눠 저장·공유해, 큰 앱도 그대로 올릴 수 있습니다.
• 조각 합치기 – 조각이 있는 폴더의 첫 조각(.001)만 목록으로 보여 주고, 고르면 나머지를 찾아 원래 파일로 합칩니다. 합친 뒤 빠지거나 섞인 조각이 없는지 검사합니다.
• 컴퓨터에서는 7-Zip·반디집의 "파일 합치기"로도 합칠 수 있습니다.

■ 개인정보를 지키는 설계
• 인터넷 권한이 아예 없습니다. 앱이 외부 서버와 통신할 수 없는 구조입니다.
• 광고 SDK, 분석·통계 SDK가 없습니다.
• 어떤 데이터도 수집하지 않습니다. 파일은 직접 공유하지 않는 한 기기 밖으로 나가지 않습니다.

■ 한 번 구매로 끝
• 광고, 구독, 인앱 결제, "프리미엄 잠금 해제"가 없습니다.
• 구매하면 모든 기능을 그대로 사용하고, 업데이트도 추가 비용 없이 받습니다.

■ 권한 안내
• 설치된 앱 목록 보기: 추출할 앱을 보여 주기 위해 필요합니다.
• 저장소 (안드로이드 9 이하만): 다운로드 폴더에 저장하기 위해 필요합니다. 안드로이드 10 이상은 권한 없이 저장합니다.
• 폴더 접근 (조각 합치기를 쓸 때만): 사용자가 고른 폴더 하나만 읽습니다.

■ 알아 두세요
• 유료 앱·게임을 꺼내 다른 사람에게 배포하는 것은 저작권 침해가 될 수 있습니다. 본인 기기 백업과 합법적인 용도로만 사용해 주세요.
• 일부 앱은 다른 기기에서 실행할 때 원래 스토어 설치를 요구할 수 있습니다.

### English
Back up the apps installed on your phone, move them to another device, or send them wherever you need.
ApkVault is an APK extractor and backup tool with no ads, no tracking and no subscription. Buy once and keep every feature.

■ When you need it
• Keep a copy of the current version before an update
• Move an app that is no longer in the store to another device
• Upload a large app file somewhere with a size limit (AI chat, email, messengers)

■ Features
• Save installed apps as APK files to Downloads/ApkExtractor
• Split apps (apps delivered as several files) are bundled into a single XAPK with every APK, manifest.json and the icon
• Extract many apps at once: long-press to select, then save or share them together
• Share straight to messengers, email or cloud drives
• Search, show system apps, sort by name, last update or size

■ Split large files into 30 MB parts
• AI chat apps such as Claude accept files of up to 30 MB. Turn this option on and apps over 30 MB are saved and shared as .001, .002 … parts, so you can upload them anyway.
• Join parts: ApkVault lists only the first parts (.001) in the folder you choose, then finds the rest and restores the original file, checking that no part is missing or mixed up.
• On a computer, 7-Zip and similar tools can join the parts too.

■ Private by design
• No INTERNET permission at all. The app cannot talk to any server.
• No ad SDKs, no analytics SDKs.
• No data is collected. Files never leave your device unless you share them.

■ Pay once
• No ads, no subscription, no in-app purchases, no "unlock premium".
• Your purchase includes every feature and every future update.

■ Permissions
• See installed apps: needed to list the apps you can extract.
• Storage (Android 9 and below only): needed to save into Downloads. Android 10+ saves without any permission.
• Folder access (only when joining parts): reads just the one folder you pick.

■ Please note
• Distributing paid apps or games to others may infringe copyright. Use ApkVault to back up your own device and for lawful purposes only.
• Some apps may require an installation from their original store to run on another device.


### 日本語
スマホにインストールしたアプリをファイルとして取り出し、保管したり、別の端末に移したり、必要な場所へ送ったりできます。
ApkVault は広告も追跡もサブスクリプションもない APK 抽出・バックアップアプリです。一度購入すれば、すべての機能をずっと使えます。

■ こんなときに
• 今のバージョンのアプリをバックアップしておきたいとき（アップデート前の保管）
• ストアから消えたアプリを別の端末に移したいとき
• サイズ制限のある場所（AI チャット、メール、メッセンジャー）に大きなアプリファイルを送りたいとき

■ 主な機能
• インストール済みアプリを APK として保存 – ダウンロード/ApkExtractor フォルダ
• 分割アプリ（複数のファイルに分かれたアプリ）は 1 つの XAPK にまとめて保存 – 各 APK、manifest.json、アイコンを含む
• 複数アプリをまとめて – アプリを長押しして選び、一度に保存・共有
• 共有 – メッセンジャー、メール、クラウドドライブなどへすぐに送信
• 検索、システムアプリの表示、名前・最終更新・サイズ順の並べ替え

■ 大きなファイルを 30MB ずつ分割保存
• Claude などの AI チャットアプリは 1 ファイル 30MB までしかアップロードできません。メニューでこのオプションをオンにすると、30MB を超えるアプリを .001、.002 … のパーツに分けて保存・共有するので、大きなアプリもそのまま送れます。
• 分割ファイルを結合 – パーツがあるフォルダの最初のパーツ（.001）だけを一覧表示し、選ぶと残りを見つけて元のファイルに結合します。結合後、パーツの欠けや混入がないか検査します。
• パソコンでは 7-Zip などの「ファイル結合」機能でも結合できます。

■ プライバシーを守る設計
• インターネット権限がまったくありません。アプリが外部サーバーと通信できない構造です。
• 広告 SDK、分析・統計 SDK がありません。
• データは一切収集しません。自分で共有しない限り、ファイルが端末の外に出ることはありません。

■ 買い切り
• 広告、サブスクリプション、アプリ内課金、「プレミアム解除」はありません。
• 購入するとすべての機能を使え、アップデートも追加料金なしで受け取れます。

■ 権限について
• インストール済みアプリの一覧: 抽出するアプリを表示するために必要です。
• ストレージ（Android 9 以下のみ）: ダウンロードフォルダに保存するために必要です。Android 10 以上では権限なしで保存します。
• フォルダへのアクセス（分割ファイルを結合するときのみ）: 選んだフォルダ 1 つだけを読み取ります。

■ ご注意
• 有料アプリやゲームを取り出して他人に配布すると、著作権侵害になる場合があります。ご自身の端末のバックアップなど、合法的な用途にのみお使いください。
• 一部のアプリは、別の端末で実行するときに元のストアからのインストールを求める場合があります。

### 简体中文
将手机上已安装的应用提取为文件，用于保存、迁移到其他设备，或发送到需要的地方。
ApkVault 是一款没有广告、没有跟踪、没有订阅的 APK 提取与备份应用。一次购买，所有功能永久可用。

■ 适用场景
• 想备份当前版本的应用（更新前保留）
• 想把商店里已下架的应用迁移到其他设备
• 需要把大应用文件上传到有大小限制的地方（AI 聊天、邮件、即时通讯）

■ 主要功能
• 将已安装应用保存为 APK – 下载/ApkExtractor 文件夹
• 拆分应用（由多个文件组成的应用）打包为一个 XAPK 保存 – 含各 APK、manifest.json 和图标
• 批量处理 – 长按应用进行多选，一次保存或分享
• 分享 – 直接发送到即时通讯、邮件、云盘等
• 搜索、显示系统应用、按名称·最近更新·大小排序

■ 大文件按 30MB 拆分保存
• Claude 等 AI 聊天应用每个文件最多只能上传 30MB。在菜单中打开此选项后，超过 30MB 的应用会被拆成 .001、.002 … 分卷保存和分享，大应用也能照常上传。
• 合并分卷 – 只列出所选文件夹中的第一个分卷（.001），选择后自动找到其余分卷并还原为原始文件。合并后会检查是否缺少或混入了分卷。
• 在电脑上也可以用 7-Zip 等工具的“合并文件”功能合并。

■ 保护隐私的设计
• 完全没有网络权限，应用在结构上无法与外部服务器通信。
• 不含广告 SDK，也不含分析·统计 SDK。
• 不收集任何数据。除非您主动分享，文件不会离开设备。

■ 一次购买
• 没有广告、订阅、应用内购买或“高级版解锁”。
• 购买后即可使用全部功能，后续更新也无需额外付费。

■ 权限说明
• 查看已安装应用：用于显示可提取的应用列表。
• 存储（仅 Android 9 及以下）：用于保存到下载文件夹。Android 10 及以上无需权限即可保存。
• 文件夹访问（仅在合并分卷时）：只读取您选择的那一个文件夹。

■ 注意
• 提取付费应用或游戏并分发给他人可能构成侵权。请仅用于备份自己的设备等合法用途。
• 部分应用在其他设备上运行时，可能要求从原商店安装。

---

## 4. 가격 설정

Play Console → 수익 창출 → 제품 → 앱 가격

| 항목 | 값 |
|---|---|
| 앱 종류 | **유료** (앱 만들 때 선택. 무료로 한 번 올리면 유료로 바꿀 수 없음) |
| 한국 가격 | **2,900원** 권장 (부가세 포함 가격. 실제 수령 약 2,240원 = 2,900 ÷ 1.1 × 0.85) |
| 다른 나라 | "환율에 따라 다른 국가 가격 업데이트" 사용 → 미국 약 1.99달러 수준 |
| 출시 기념 | 첫 2주 할인 등은 출시 후 가격 변경으로 가능 |

준비물: Play Console → 설정 → **결제 프로필** (은행 계좌, 세금 정보). 유료 앱을 파는 개발자는 스토어에 주소가 공개됩니다.

## 5. 권한 신고: QUERY_ALL_PACKAGES (설치된 모든 앱 보기)

Play Console → 앱 콘텐츠 → 민감한 권한 및 API → "모든 패키지 쿼리"(QUERY_ALL_PACKAGES) 신고

**핵심 기능 (선택지)**: 화면에 나오는 목록 중 "기기 검색", "백업 및 복원", "파일 관리" 등 가장 가까운 항목.

**사용 목적 설명 (한국어)**
ApkVault의 유일한 핵심 기능은 사용자가 휴대폰에 설치한 앱을 골라 APK/XAPK 파일로 백업(추출)하는 것입니다.
사용자가 어떤 앱이든 백업할 수 있어야 하므로, 런처 아이콘이 없는 앱과 시스템 앱을 포함해 설치된 모든 앱의 목록과
설치 파일 경로(sourceDir, splitSourceDirs)를 보여 주고 읽어야 합니다. 특정 앱만 지정하는 <queries> 선언으로는
사용자가 고를 앱을 미리 알 수 없어 이 기능을 제공할 수 없습니다.
앱 목록과 패키지 정보는 화면 표시와 사용자가 요청한 파일 생성에만 쓰이며, 저장·전송·분석하지 않습니다.
앱에는 인터넷 권한이 없어 이 정보가 기기 밖으로 나갈 수 없습니다.

**Purpose (English)**
ApkVault's core and only function is letting users back up (extract) apps installed on their device as APK/XAPK files.
To let users back up any app, including apps without a launcher icon and system apps, ApkVault must list all installed
packages and read their installer paths (sourceDir, splitSourceDirs). A <queries> declaration naming specific packages
cannot work because the user chooses the app at run time.
The package list is used only for on-screen display and for creating the files the user requests; it is never stored,
transmitted or analysed. The app has no INTERNET permission, so this information cannot leave the device.

**데모 영상(요청될 경우)**: 앱 실행 → 목록에서 앱 선택 → "다운로드 폴더에 저장" → 파일 관리자에서 저장된 파일 확인,
30초 정도 화면 녹화를 YouTube(일부 공개)에 올려 링크 제출.

## 6. 데이터 보안 양식 · 앱 콘텐츠
- 사용자 데이터를 수집하거나 공유하나요? **아니요** (인터넷 권한 없음, 광고·분석 SDK 없음)
- 광고 포함: **아니요**
- 앱 액세스 권한: 모든 기능을 로그인 없이 사용 가능
- 콘텐츠 등급 설문: 유틸리티, 폭력·성적 콘텐츠·도박 없음 → 전체이용가 예상
- 타겟층: 18세 이상 (아동 대상 아님)
- 개인정보처리방침 URL: https://jeongguskbag-prog.github.io/apk-extractor/privacy/
- 카테고리: 도구 (Tools)

## 7. 그래픽 자료
- 앱 아이콘 512×512: `store/icon-512.png`
- 그래픽 이미지 1024×500: `store/feature-graphic-ko.png`, `-en.png`, `-ja.png`, `-zh.png` (언어별로 하나씩)
- 휴대폰 스크린샷 2~8장: 실제 기기에서 캡처 (앱 목록, 선택 모드, 저장 완료, 나눠 저장 옵션, .001 조각 목록)
