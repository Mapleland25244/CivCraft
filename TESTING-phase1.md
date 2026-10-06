# CivCraft Phase 1（外部插件整合層）測試紀錄（TESTING-phase1.md）

對照基準線：`TESTING.md`（1.12.2，改動前）。本檔只記錄 Phase 1 改動後的結果，不覆蓋基準線。
結果欄位：`PASS` / `FAIL` / `原本就壞` / `待測` / `N/A`。

> 版面約定：表格的「結果」欄只放簡短結論；日誌、數字、推論都移到各表下方的「實測紀錄」。

## 目錄
- [0. 環境](#0-環境)
- [1. 改動摘要](#1-改動摘要)
- [2. 啟動煙霧測試](#2-啟動煙霧測試)
- [3. 功能測試](#3-功能測試)
- [4. 新增／更新的已知問題](#4-新增更新的已知問題)
- [5. 尚未完成](#5-尚未完成)

---

## 0. 環境

| 項目 | 值 |
|---|---|
| 測試日期 | 2026-10-06 |
| 伺服器 | Spigot 1.12.2（`git-Spigot-79a30d7-f4830a1`），Java 1.8.0_504 |
| CivCraft | 1.8.0-Beta6，ai-refactor 分支，Phase 1 改動**尚未 commit** |
| 資料庫 | 本機 MySQL，`civ_game` / `civ_global`，沿用主測試庫（`online-mode=true`） |
| 已安裝插件 | dynmap 3.0-beta-4-213、dynmap-civcraft 1.0、CustomMobs 4.17（1.12.2 自行停用）、WorldBorder 1.8.5、Vault 1.7.3（只有 SuperPermissions）、Herochat 5.6.7（啟用失敗，見 §4） |
| 未安裝 | TagAPI/iTag、ProtocolLib、TitleAPI、NoCheatPlus、VanishNoPacket |

---

## 1. 改動摘要

**新增套件 `com.avrgaming.civcraft.integration`**
- `Integrations` 為唯一入口；Vault、WorldBorder、iTag、TitleAPI、VanishNoPacket、CustomMobs 各有「介面＋實作」。
- HeroChatListener、TagAPIListener、NoCheatPlusSurvialFlyHandler 搬入此套件。

**呼叫端改動**
- `Town`、`SyncUpdateTags*`、`Buildable`、`CivMessage`、`MobSpawner`、`VaultEconObject`、`PlayerLocationCache` 改呼叫 `Integrations`。
- `CivGlobal` 移除 Vault 欄位與 `getEconomy()`；`CivSettings.hasITag` 移除。

**啟動與偵測**
- `CivCraft.hasPlugin()` 改為要求插件已啟用。
- `Integrations.init()` 在 `CivSettings.init` 之後、`SQL.initialize` 之前呼叫。
- `plugin.yml`：`softdepends`（錯字，Bukkit 不認）改為 `softdepend`，並列出所有可選插件。

**靜態驗證**
- javac 編譯通過（單元測試檔除外）。
- 第三方插件的 import 只剩 `integration/` 內有。

---

## 2. 啟動煙霧測試

### 2.1 首次啟動（11:09）

| # | 檢查項 | 結果 |
|---|---|---|
| 1.1 | 無 `NoClassDefFoundError` / `ClassNotFoundException` | PASS |
| 1.2 | 無 `InvalidConfiguration` | PASS |
| 1.3 | 無 SQL 例外，資料表齊全 | PASS |
| 1.4 | 26 個 yml 載入 | PASS |
| 1.5 | `isError()` 為 false | PASS |
| — | 載入的新 jar 確實是 Phase 1 | PASS |
| — | softdepend 啟用順序 | PASS |
| — | 資料載入筆數 | PASS |
| — | 登入 | PASS |
| — | 重啟後建造進度接續 | PASS |

**實測紀錄**
- 1.1：缺 VanishNoPacket、TitleAPI、TagAPI、NCP 仍正常啟動。
- 1.3：各表 `OK`；只有 JDBC SSL 警告（舊問題）。
- 1.5：`Done (2.911s)`。
- 新 jar 證據：`TagAPI/NoCheatPlus not found` 警告出現在 `Initializing SQL` **之前**（舊版在 `loadGlobals` 之後才印）。
- 啟用順序：Vault → dynmap → CustomMobs → WorldBorder → Herochat → CivCraft。
- 載入筆數：2 Civs、2 Towns、4 Residents、10 PermissionGroups、40 TownChunks、4 Structures、28 Protected Blocks。
- 登入：Mapleland25244 UUID `f41d1dcf-…`，無 `Duplicate entry` / `No resident found`。
- 建造進度：Farm 11:17 顯示 40%，重啟後有接續。

### 2.2 `stop` 與重啟（12:11–12:16）

| # | 檢查項 | 結果 |
|---|---|---|
| 1.8 | `onDisable` 正常 | PASS |
| 2.9.1 | 重啟後筆數與金額還原 | PASS |

**實測紀錄**
- 1.8：12:11:12 `stop`（Mapleland 仍在線）：`Disabling CivCraft v1.8.0-Beta6` 後無例外，世界與玩家正常存檔。
- 2.9.1 筆數：12:12 重啟載入 2 Civs、1 Relation、2 Towns、4 Residents、10 PermissionGroups、40 TownChunks、4 Structures，與重啟前一致。
- 2.9.1 差異：Trade Goods 7→11、Protected Blocks 28→52（推測：期間探索新區塊與 Farm 完成所致，未逐項核對）。
- 2.9.1 登入：Mapleland 以 `f41d1dcf-…` 登入，無 `Duplicate entry`。
- 2.9.1 金額（12:16 核對）：Mapleland 38078.0、Rome 金庫 16475.0，與重啟前（12:08）完全一致；Upkeep 1100、Growth 122、Hammers 261.375 亦同。

---

## 3. 功能測試

| # | 項目 | 結果 |
|---|---|---|
| CM | CustomMobs 偵測與崩潰 | PASS |
| 2.7.2a | WorldBorder 有邊界，建築跨界 | PASS |
| 2.7.2b | WorldBorder 有邊界，位置在界內（對照組） | PASS |
| 2.7.2c | 無 WorldBorder 插件 | PASS |
| 2.4.3 | `use_vault=false`，`/pay`、`/town deposit` | PASS |
| 2.4.2 | `use_vault=true` | 待測（無經濟插件） |
| 2.7.3 | TagAPI/iTag + ProtocolLib 名牌 | 待測（需安裝） |
| 2.7.5 | TitleAPI | 待測（需安裝） |
| 2.7.7 | VanishNoPacket | 待測（需安裝） |
| 2.7.9 | dynmap | PASS（啟用） |

**實測紀錄：CM（CustomMobs）**
- CustomMobs 自行停用後，日誌為 `CustomMobs not found or disabled`（舊版誤報 `hooks enabled`）。
- 使用者確認「已經不會崩潰」。
- 限制：這份紀錄沒有包含探索新區塊的伺服器日誌，建議下次附上。

**實測紀錄：2.7.2 WorldBorder**
- 2.7.2a：
  - 設定 `/wb world set 40 -304 288` 後，11:40:04 `/build` 收到 `Failed to build Cannot build here. Part of the structure would sit beyond the world border.`（`cannotBuild_outsideBorder`）。
  - OP 身分也被擋；讀碼：OP 只繞過離出生點距離檢查（`Buildable.java:858`）。
- 2.7.2b：
  - 11:36:25 收到 `Your town is currently building a Farm.. Can only build one structure at a time.`。
  - 讀碼：`runCheck`（含邊界檢查）在「一次一棟」檢查之前執行，故代表通過邊界檢查。
- 2.7.2c：
  - 12:12 重啟（移除 WorldBorder.jar）：出現 `WorldBorder not found, buildings will not be checked against the world border.`，無 `NoClassDefFoundError`，啟動 `Done (3.070s)`，Mapleland 登入正常。
  - 12:16 在此狀態下 `/build`：使用者回報「未阻擋」（沒有邊界訊息、無例外），與預期一致。
- 測試指令（供重現）：`/wb list`、`/wb world set 40 -304 288`、`/wb world radius 20`、`/wb bypasslist`、結束後 `/wb world clear`。**請確認已執行 `/wb world clear`，且 `/wb list` 回到 `There are no borders currently set.`**

**實測紀錄：2.4.3 經濟（`use_vault=false`）**
- 12:07–12:08：Mapleland 38228 → `/pay Toxicnnan 100`、`/town deposit 100`、`/econ add 50` → 38078（= 38228 − 100 − 100 + 50）。
- Rome 金庫 16375 → 16475（+100）。
- 訊息 `Paid 100.0 Coins`、`Deposited 100 Coins`、`Added 50 Coins` 皆正確。
- Toxicnnan 餘額未直接查看。
- 開關位置：`plugins\CivCraft\data\civ.yml` 的 `global.use_vault`（預設 `false`；程式在 `CivGlobal.java:208` 讀取）。

**實測紀錄：待測項目**
- 2.4.2：環境沒有經濟插件（Vault 只有 SuperPermissions）；Vault 只是橋接層，需另有經濟提供者（如 EssentialsX）。
- 2.7.3 / 2.7.5 / 2.7.7：需安裝對應插件後再測。

**實測紀錄：2.7.9 dynmap**
- `dynmap-civcraft v1.0` 啟用；邊界是否畫上網頁地圖仍待確認。

---

## 4. 新增／更新的已知問題

| 日期 | 現象 | 原因 | 狀態 |
|---|---|---|---|
| 2026-10-06 | `plugin.yml` 的 `softdepends: [TitleAPI]` 是錯字，TitleAPI 從未真正是 softdepend | 鍵名應為 `softdepend` | **已修**（Phase 1） |
| 2026-10-06 | `CivCraft.hasPlugin()` 只看插件有無載入，CustomMobs 自行停用時仍被當成可用，導致 `CustomMobsAPI` 崩潰 | 偵測條件不足 | **已修**（改為 `isEnabled()`；啟動端與使用者回報皆確認） |
| 2026-10-06 | 缺 WorldBorder 時 `Buildable` 路徑可能 `NoClassDefFoundError` | 核心類別直接 import WorldBorder | **已修**（改走 `Integrations.isInsideBorder`），無插件分支已驗證（2.7.2c） |
| 2026-10-06 | 非盟友文明玩家被 WorldBorder 推回時，CivCraft 回 `[Denied] You must be allies in order to Teleport into Civ HolyRomanEmpire.`，玩家留在邊界外並反覆刷訊息（實例：Toxicnnan，GermanReich） | WorldBorder 用傳送把人推回，目的地在別國領地內，被 CivCraft 傳送限制否決；`/wb bypasslist` 為空，排除豁免 | 既有互動，與 Phase 1 無關；正式環境邊界通常遠大於領地，不易遇到 |
| 2026-10-05 | Herochat 5.6.7 啟用失敗 `NoSuchMethodError: Server._INVALID_getOnlinePlayers()` | jar 是針對舊 Bukkit API 編譯 | 環境問題，2.7.4 記 N/A |
| 2026-10-06 | 客戶端進服時 `NullPointerException`（`brz.a` 的 `forEach`） | 已列於基準線第 4 節 | 本輪再次出現，屬客戶端錯誤 |
| 2026-10-05 | JDBC SSL 警告洗版 | JDBC URL 未設 `useSSL` | 待處理（基準線已列） |

---

## 5. 尚未完成

1. 把 `WorldBorder.jar` 放回 plugins 還原環境（目前伺服器處於無 WorldBorder 狀態）。
2. 確認 `/wb world clear` 已執行（有 WorldBorder 時 `/wb list` 應回到 `There are no borders currently set.`）。
3. 驗收標準 3：`INSTALL.txt` 與 `plugin.yml` 的相依宣告一致。
4. 2.4.2：`use_vault=true` 需先有經濟插件。
5. 2.7.3 / 2.7.5 / 2.7.7：安裝 TagAPI+ProtocolLib、TitleAPI、VanishNoPacket 後測試。
6. 測試結束後：確認 Farm 完成後 Growth 變化（基準線 C2）、commit Phase 1。
