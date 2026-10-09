# CivCraft 測試紀錄索引與慣例

> 更新：2026-10-06 ｜ 相關：[ROADMAP](../ROADMAP.md) ｜ [ARCHITECTURE](../ARCHITECTURE.md)

## 目錄
- [1. 紀錄索引](#1-紀錄索引)
- [2. 命名規則](#2-命名規則)
- [3. 檔案格式](#3-檔案格式)
- [4. 結果欄位](#4-結果欄位)
- [5. 新增一份紀錄](#5-新增一份紀錄)
- [6. 提交前檢查](#6-提交前檢查)

---

## 1. 紀錄索引

| 檔案 | 範圍 | 對應 ROADMAP | 狀態 |
|---|---|---|---|
| [00-baseline-1.12.2.md](00-baseline-1.12.2.md) | 改動前的 1.12.2 行為（基準線，不覆蓋） | R0 | 基準線，持續補測 |
| [01-phase1-integration.md](01-phase1-integration.md) | 外部插件整合層（`integration` 套件） | R1 | 已驗證 |
| [02-r2-legacy-api.md](02-r2-legacy-api.md) | 方塊／物品存取層（舊 API 收進 `ItemManager`） | R2 | 進行中 |
| [03-r3-scheduling.md](03-r3-scheduling.md) | 排程與生命週期（`TaskScheduler`、`onDisable`、async 審計） | R3 | 已驗證（有擱置項） |
| [04-r4-nms.md](04-r4-nms.md) | NMS 與 NBT 隔離（`nms` 套件、`AttributeUtil`／`HorseModifier` 外殼） | R4 | 程式完成，部分驗證（有既有 bug 與未觸發項） |
| [05-u0-paper-api.md](05-u0-paper-api.md) | 建置依賴改為 Paper API 1.12.2 | U0 | Maven 建置與 Paper 啟動 PASS，基準線回歸擱置 |
| [06-u1a-1.13.2.md](06-u1a-1.13.2.md) | 升級到 1.13.2（`LegacyBridge`、`v1_13_R2`、`api-version: 1.13`） | U1a | 程式完成，javac／Maven／離線檢查通過，伺服器待驗證 |
| [_template.md](_template.md) | 空白範本 | — | — |

---

## 2. 命名規則

`NN-<主題>.md`

- `NN`：兩位數流水號，依開始測試的先後；基準線固定為 `00`。
- `<主題>`：小寫英文與連字號。重構步驟用 `phase<N>-<名稱>` 或 `r<N>-<名稱>`；升級跳躍用 `upgrade-<版本>`（例如 `03-upgrade-1.13.md`）。
- 基準線檔名帶版本（`00-baseline-1.12.2.md`），其他紀錄不帶，版本寫在檔內的環境表。
- 範本以底線開頭（`_template.md`），不編號。
- 設計類文件（`ARCHITECTURE.md`、`ROADMAP.md`）放在上一層 `docs/`，檔名全大寫。

---

## 3. 檔案格式

每份紀錄依此順序：

1. `# 標題`（不放檔名）。
2. 一個引用區塊：`更新`、`範圍`、`狀態`，以及對照的基準線與本索引的連結。
3. 結果欄位一行、版面約定一行。
4. `## 目錄`。
5. `## 0. 環境`：日期、伺服器、Java、CivCraft commit、資料庫、已安裝／未安裝插件。
6. 改動摘要（基準線則為測試說明）。
7. 各項測試：表格只放簡短結論；日誌、數字、讀碼推論放在表格下方的「實測紀錄」，以條列呈現並標明時間。
8. `已知問題`：日期、現象、原因、狀態四欄；依性質分組。
9. `尚未完成`：編號清單。

原則：
- 不刪除原始證據（時間戳、數字、日誌行），只重新排列。
- 推測要標「推測」或「未驗證」，讀碼結論標「讀碼」。
- 跨檔連結用相對路徑。

---

## 4. 結果欄位

| 值 | 意思 |
|---|---|
| `PASS` | 符合預期；部分通過寫成 `PASS（部分）` 並在實測紀錄說明 |
| `FAIL` | 不符預期，且由本次改動造成或尚未釐清 |
| `原本就壞` | 1.12.2 基準線已壞，非遷移或重構造成 |
| `N/A` | 環境無法測（例如缺少相依插件） |
| `待測` | 尚未測 |

---

## 5. 新增一份紀錄

1. 複製 [_template.md](_template.md) 為 `NN-<主題>.md`。
2. 填環境表；從基準線挑出與本次改動相關的項目，保持相同編號，方便逐項對照。
3. 測前備份 `civ_game`，並記下 `/res info`、`/town info`、`/civ info` 的金額。
4. 一律 `stop` 後重啟，不使用 `/reload`。
5. 在上面的索引表加一列，並在 ROADMAP 對應步驟連結過去。

---

## 6. 提交前檢查

兩支檢查腳本由 `tools/pre-commit-check.sh` 統一呼叫，依暫存的檔案決定要跑哪些：

| 暫存的檔案 | 執行的檢查 | 耗時 |
|---|---|---|
| `civcraft/src/**/*.java`、`tools/check-legacy-api.sh`、`tools/legacy-api-baseline.txt` | `tools/check-legacy-api.sh`：舊（1.13 以前）API 棘輪，數字只能降、不能升 | 數秒 |
| `civcraft_data/templates/**`、`tools/ScanTemplates.java`、`tools/scan-templates.sh`、`tools/template-blocks-baseline.txt` | `tools/scan-templates.sh --check`：藍圖的 `id:data` 組合不得超出基準 | 約 1 分鐘 |

- 全部執行：`bash tools/pre-commit-check.sh --all`。
- 啟用 hook（每個 clone 一次）：`bash tools/install-hooks.sh`（設定 `core.hooksPath = .githooks`）。復原：`git config --unset core.hooksPath`。略過一次：`git commit --no-verify`。
- 失敗時：修掉問題；若改動是有意的，更新基準：`tools/check-legacy-api.sh --write-baseline` 或 `tools/scan-templates.sh --write`。
- 某類別數字下降時腳本會印出提示，請執行 `--write-baseline` 把進度鎖住。
- `.gitattributes` 把 `*.sh`、`.githooks/*`、`tools/*.txt`、`tools/*.java` 固定為 LF；Windows 預設的 `core.autocrlf=true` 會把它們轉成 CRLF，bash 就無法執行。
