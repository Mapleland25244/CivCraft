# CivCraft U0（建置依賴改為 Paper API）測試紀錄

> 更新：2026-10-06 ｜ 範圍：`spigot-api` 換成 `paper-api` 1.12.2（只改建置依賴，程式碼不變）｜ 狀態：**javac、Maven 建置（U0.1）、Paper 1.12.2 啟動（U0.2）皆 PASS；基準線回歸（U0.3）擱置**
> 對照：[基準線](00-baseline-1.12.2.md)。本檔只記錄改動後的結果，不覆蓋基準線。
> 慣例與索引：[README](README.md) ｜ 設計：[ROADMAP §3](../ROADMAP.md)

結果欄位：`PASS` / `FAIL` / `原本就壞` / `N/A` / `待測`。

## 1. 改動摘要

| 檔案 | 改動 |
|---|---|
| `pom.xml` | 新增屬性 `paper.version`（`1.12.2-R0.1-SNAPSHOT`）與 PaperMC 倉庫 `https://repo.papermc.io/repository/maven-public/`；`spigot.version` 保留，註解改為「只給 `nms` 套件的伺服器內部類別」 |
| `civcraft/pom.xml` | `spigot-api` 換成 `com.destroystokyo.paper:paper-api`，**排在 `spigot` 之前**（`spigot` 也含 Bukkit API 類別，順序決定編譯用哪一份） |
| `itag-stub/pom.xml`、`civcraft_dynmap/pom.xml` | `spigot-api` 換成 `paper-api` |

- `org.spigotmc:spigot`（v1_12_R1 的 NMS）**必須保留**：Paper 沒有把伺服器本體發佈到 Maven，而 `nms/v1_12_R1` 需要它；R4 已讓只有該套件用到。
- 運行環境不變：伺服器仍是 1.12.2；Paper API 是 Spigot API 的超集，所以不需要改任何 Java 檔。
- 取得的版本：`paper-api-1.12.2-R0.1-20190714.184133-413`（PaperMC 倉庫的最後一個 1.12.2 快照）。

## 2. 靜態驗證

| 項目 | 結果 |
|---|---|
| javac（JDK 8，`-source 8`，classpath 依序為 paper-api、spigot、`civcraft/lib/*`；含 `itag-stub`，不含 `unittests`） | 通過，無錯誤；`-verbose` 確認 `org.bukkit.Bukkit` 等 API 類別從 paper-api 載入 |
| `civcraft_dynmap`（paper-api、dynmap-api、已編譯的 civcraft） | 通過，無錯誤 |
| `tools/pre-commit-check.sh --all`（legacy-api、scheduler、nms、藍圖盤點） | 全部通過 |
| Maven（`mvn package`） | PASS（使用者回報，2026-10-06）：能從 PaperMC 倉庫解析 `paper-api` 並建置成功（本機沒有 `mvn`，由使用者建置） |

## 3. 伺服器驗證項目

| # | 項目 | 預期 | 結果 |
|---|---|---|---|
| U0.1 | 用 Maven 建置 `CivCraft.jar` | 可解析 `paper-api`；jar 內容與改動前相同（只有依賴不同，沒有被打包） | PASS（使用者回報成功） |
| U0.2 | 新 jar 在 1.12.2 伺服器啟動（Spigot 或 Paper 皆可） | 與 [R4 的 4.1](04-r4-nms.md) 相同：無例外、無 `no NMS adapter` | PASS（Paper git-Paper-1620，見下方實測紀錄） |
| U0.3 | 基準線回歸 | 與基準線逐項相同 | 擱置（2026-10-06 決定先打 tag；U1 開始前或之後以同一份基準線補測） |

### 實測紀錄（2026-10-06，`1.12.2_paper/logs/latest.log`，使用者提供）

- 伺服器：`git-Paper-1620 (MC: 1.12.2)`，Java 1.8.0_504；17:57:57 Enabling CivCraft → 17:57:59 `Done (2.867s)`。
- 全部設定檔載入、`civ_game`／`civ_global` 連線成功、資料表檢查 OK；載入 2 Civs、2 Towns、4 Residents、6 Structures、13 Trade Goods、60 Protected Blocks；`dynmap-civcraft` 啟用。
- 日誌無 `no NMS adapter`、無 `NoClassDefFoundError`；CivCraft 沒有例外。
- 新出現的 Paper 專屬警告：`LoreCraftableMaterial.buildRecipes`（第 223、274 行）用已棄用的 `ShapedRecipe`／`ShapelessRecipe` 建構子，共 200 則堆疊（Paper 要求配方有 `NamespacedKey`）。只是警告，不影響啟動。
- 與基準相同的既有項目：CustomMobs 自行停用、Herochat `NoSuchMethodError: _INVALID_getOnlinePlayers`、MySQL SSL 警告、VanishNoPacket／TitleAPI／TagAPI／NoCheatPlus 未安裝。
- 日誌只到啟動後數秒，沒有玩家操作，U0.3 仍待測。

## 4. 已知問題

| 日期 | 現象 | 原因 | 狀態 |
|---|---|---|---|
| 2026-10-06 | Paper 啟動時印出 200 則「Deprecated recipe」警告 | `LoreCraftableMaterial` 用無 `NamespacedKey` 的配方建構子（Paper 才警告） | 未處理；之後加 key 或於 U1 處理（行為需驗證） |
| 2026-10-06 | PaperMC 倉庫的 1.12.2 是舊快照（2019），之後若倉庫清理可能無法解析 | 倉庫保留策略（推測） | 若出現，改為把 jar 裝進 `local-libs` 或 `~/.m2` |

## 5. 尚未完成

1. ~~Maven 建置（U0.1）、啟動（U0.2）~~：已完成。
2. U0.3（基準線回歸）擱置；已先打 tag `u0-paper-api` 作為 U1 的回滾點。此步只改建置依賴、程式碼不變，風險低；補測若發現問題再回報。
3. 之後 U1（1.13）開始，`paper-api` 版本號才會改變；本步驟不要順便升版本。
