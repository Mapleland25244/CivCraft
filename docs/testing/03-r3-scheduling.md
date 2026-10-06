# CivCraft R3（排程與生命週期）測試紀錄

> 更新：2026-10-06 ｜ 範圍：`TaskMaster` 介面化、修 `cancelTimer`、`onDisable` 取消任務、async 世界存取審計（R3.1–R3.4） ｜ 狀態：已驗證（3.1、3.2 建造、3.6 PASS；3.3、3.4、3.5 未觸發、3.7 跳過；3.2 的箱子朝向與拆除破損擱置、未歸因）
> 對照：[基準線](00-baseline-1.12.2.md)。本檔只記錄改動後的結果，不覆蓋基準線。
> 慣例與索引：[README](README.md) ｜ 設計：[ROADMAP §3](../ROADMAP.md)

結果欄位：`PASS` / `FAIL` / `原本就壞` / `N/A` / `待測`。

## 目錄
- [1. 改動摘要](#1-改動摘要)
- [2. 審計：async 任務碰世界](#2-審計async-任務碰世界)
- [3. 伺服器驗證項目](#3-伺服器驗證項目)
- [4. 已知問題與留待事項](#4-已知問題與留待事項)

---

## 1. 改動摘要

| 步驟 | 內容 | 驗證 |
|---|---|---|
| R3.1 | 新增 `threading/TaskScheduler`（介面）與 `BukkitTaskScheduler`（實作）；`TaskMaster` 保留所有靜態簽章，改為委派。只有 `BukkitTaskScheduler` 與 `BukkitObjects` 碰 Bukkit scheduler；`BukkitObjects` 的 `schedule*` 移除，新增 `getPlugin()`；`Buildable`（2 處）、`Blacksmith` 的直接呼叫改走 `TaskMaster` | javac 通過 |
| R3.2 | 修正：`cancelTimer` 原本從 `tasks` 取值（取消不到計時器）；`syncTimer` 原本不登記名稱（無法取消）；名稱表原為 `HashMap` 卻被 async 執行緒寫入（競態），改 `ConcurrentHashMap`；同名計時器重複登記時先取消舊的；空名稱（`""`，約 20 處）不再登記（原本互相覆蓋且無人讀取） | javac；原本沒有任何呼叫端使用 `cancelTimer/cancelTask/stopAll` |
| R3.3 | `onDisable`：`isDisable = true` → `TaskMaster.stopAll()`（`cancelTasks(plugin)`，連未登記的任務一併取消）→ `SQLUpdate.save()` | javac；伺服器 stop 測試待測 |
| R3.4 | 審計見 §2；修 2 處明確違規（登入重生傳送、間諜任務失敗移除單位物品） | javac |
| 檢查 | 新增 `tools/check-scheduler.sh`（`civcraft/src` 內，threading 以外不得使用 Bukkit scheduler），接入 `tools/pre-commit-check.sh` | 腳本 OK |

行為差異（刻意）：
- 同名 sync/async 計時器重複登記，舊的會被取消（`/dbg` 的 `lagtimer` 重複執行時不再疊加）。
- 空名稱任務不再登記；`hasTask("")` 本來就不會被用到。
- `syncTask` 由 `scheduleSyncDelayedTask` 改為等價的 `runTaskLater`。

---

## 2. 審計：async 任務碰世界

方法：從 `TaskMaster.asyncTask/asyncTimer` 的呼叫端收集被排程的類別（約 50 個，另含以變數傳入者），在這些類別內搜尋世界／實體／背包 API，逐一對照該段是否在 `syncTask`／`Sync*` 請求內。結果以讀碼判斷，未實測。

### 2.1 架構結論
`CivAsyncTask` 已提供「async 發請求、主執行緒處理」的機制（`syncLoadChunk`、`getChestInventory`、`updateInventory`、`syncGrow`，由 `Sync*` 計時器每 tick 處理）。大多數重任務（`BuildAsyncTask`、`StructureValidator`、`MobGrinder/Trommel/Quarry/Fishery`、兩個 PostGen、`BiomeCache`、`CannonExplosionProjectile`）都遵守此模式。

### 2.2 已修正

| 位置 | 問題 | 修法 |
|---|---|---|
| `PlayerLoginAsyncTask`（待處理重生） | 在 async 執行緒呼叫 `Player#teleport` | 包進 `TaskMaster.syncTask` |
| `EspionageMissionTask`（任務失敗） | 在 async 執行緒呼叫 `Unit.removeUnit(player)`（修改玩家背包） | 包進 `TaskMaster.syncTask` |

### 2.3 判定為合規（world 存取皆在 sync 內）
`BuildAsyncTask`（`removeScaffolding` 在內層 `SyncTask`）、`StructureValidator`（`SyncLoadSnapshotsFromLayer`）、`MobSpawnerPostGenTask`／`TradeGoodPostGenTask`（`SyncMopSpawnerGenTask` 等）、`BiomeCache`、`CannonExplosionProjectile`（`SyncTask`）、`PlayerLoginAsyncTask` 的死亡處理（`SyncTask`）、`TradeLevelComponent`／`ConsumeLevelComponent`／`Resident` 內的 `AsyncTask`（只碰 SessionDB 或 perk 資料表）。

### 2.4 風險較低、未修（留待事項，見 §4）

| 位置 | 內容 | 風險 |
|---|---|---|
| `UpdateTagBetweenCivsTask`、`SLSManager` | async 呼叫 `Bukkit.getOnlinePlayers()` | 讀取集合；1.12 為非同步安全性不保證，新版 Paper 為複製清單 |
| `CivGlobal.getPlayer`（`PlayerChunkNotifyAsyncTask`、`NotificationTask`、`TownAddOutlawTask`、`EspionageMissionTask` 等） | async 查玩家 | 只讀 |
| `EspionageMissionTask`、`PlayerChunkNotifyAsyncTask`、`PlayerLoginAsyncTask` | async 讀 `Player#getLocation`、`CivMessage.send`（`sendMessage`） | 只讀／送訊息 |
| `MobGrinder/Trommel/Quarry/Fishery` 等 | 在 async 對 `getChestInventory` 取得的 `Inventory`／`MultiInventory` 讀取並計算，寫回走 `UpdateInventoryRequest` | 讀取為主；R4 前不動 |
| `civcraft_dynmap` | 直接使用 `getScheduler()` | 不同模組，未納入檢查腳本 |

---

## 3. 伺服器驗證項目

| # | 項目 | 預期 | 結果 |
|---|---|---|---|
| 3.1 | 啟動 | 無例外，約 35 個計時器照常運作（以日誌確認各計時器的輸出） | PASS（啟動、延遲 async 任務、登入 async 任務、整點事件的 async 計時器）；`syncTimer` 類待 3.2 補證，見實測紀錄 3.1 |
| 3.2 | 建造一棟建築至完成 | 與基準線相同（箱子、朝向、進度訊息） | 建造流程 PASS；**箱子朝向有 1 個反了、`/build demolish` 後建地破損且殘留建築方塊：待歸因**（是否 R3 造成未確認，見實測紀錄 3.2） |
| 3.3 | 礦場／採石場／漁場等結構的定時產出 | 產出照常 | 未測（沒有這些建築） |
| 3.4 | 登入時有待處理重生（SessionDB `global:respawnPlayer`） | 玩家被傳送、紀錄被刪除、無 async 例外 | 未觸發（使用者無法造出該條件）；僅確認登入任務無例外 |
| 3.5 | 間諜任務因曝光失敗 | 單位物品被移除、無例外 | 未觸發（使用者無法執行） |
| 3.6 | 以 `stop` 關閉伺服器 | 日誌無 `Plugin attempted to register task while disabled`、佇列中的存檔寫入完成、程序正常結束 | PASS（無錯誤關閉）：以 R3 jar 乾淨重跑（16:35:24 啟動、16:35:32 `stop`），日誌無例外、無 `attempted to register task while disabled`，插件依序停用，port 25565 已釋放；**「佇列中的存檔寫完」未驗證**（這輪沒有登入也沒有產生存檔，`stopAll` 無日誌輸出），見實測紀錄 3.6 |
| 3.7 | `/reload` 或停用再啟用（若可用） | 計時器不重複 | 部分通過：使用者在 16:38:00 於主控台執行 `/reload`，CivCraft 成功停用並重新啟用（`Reload complete.`），無 CivCraft 例外；但 Spigot 印出 **2 筆 `Nag author … 'CivCraft' … not properly shutting down its async tasks when it is being reloaded`**，代表有 2 個 async 工作在停用後 2.5 秒仍在執行。**未歸因**（沒有 R2 的 `/reload` 對照），見實測紀錄 3.7 |

### 實測紀錄

環境：Spigot 1.12.2（`git-Spigot-79a30d7-f4830a1`）、Java 1.8.0_504、本機 MySQL（`civ_game` / `civ_global`）；已安裝 dynmap 3.0-beta-4、dynmap-civcraft、CustomMobs 4.17、WorldBorder 1.8.5、Vault 1.7.3、Herochat 5.6.7；未安裝 TitleAPI、TagAPI、NoCheatPlus、VanishNoPacket。`plugins/CivCraft.jar` 由使用者以 Maven 自行建置（`Created-By: Maven JAR Plugin 3.2.2`），不是 Claude 暫存的 jar；以 `javap` 確認含 `TaskScheduler`、`BukkitTaskScheduler`、`onDisable` 內 `TaskMaster.stopAll` → `SQLUpdate.save`、`PlayerLoginAsyncTask$1`、`EspionageMissionTask$1`。

**3.1（2026-10-06 15:18 啟動，日誌 363 行）**
- CivCraft 區段：26 個設定檔、SQL（game／global 兩個連線池）、所有資料表 OK，載入 2 Civs / 2 Towns / 4 Residents / 40 TownChunks / 5 Structures / 13 Trade Goods，`Done (2.943s)`；無 `NoClassDefFoundError`、無 CivCraft 例外。
- 日誌中的 ERROR 只有兩項，皆非 R3 造成：`CustomMobs` 在 1.12.2 自行停用（已知，見 ROADMAP §6）；`Herochat` 的 `NoSuchMethodError: Server._INVALID_getOnlinePlayers()`（HeroChat 5.6.7 與此環境的已知問題，ROADMAP §6 已列）。
- 啟動時 `startTimers()` 本身沒有任何日誌輸出，所以「約 35 個計時器運作中」無法從啟動日誌直接證明；需等計時器自然觸發的輸出（例如城鎮文化產出、事件訊息）出現在日誌中。
- 後續日誌（至 15:21:32，363 → 371 行）：
  - 15:21:02 起每約 10 秒一筆 `Doing a structure validate... <建築>`（Barracks → Bank → Capitol → Capitol），執行緒為 `Craft Scheduler Thread - 7`。這是 `startTimers()` 內 `asyncTask(new StructureValidationChecker(), toTicks(120))` 的延遲任務（啟動後約 2 分 16 秒），證明**延遲 async 任務**經新的 `BukkitTaskScheduler.asyncTask` 正常執行。
  - 15:21:08 `Mapleland25244` 登入：`Scheduling on player login task` → `Running PlayerLoginAsyncTask for Mapleland25244`，之後無例外。這證明 `PlayerLoginAsyncTask`（含本次修改的類別）在登入路徑上可正常載入與執行；但**沒有**待處理重生紀錄，修改的 `teleport` 分支未被觸發（見 3.4）。
  - 至 15:21:32 無任何 CivCraft 例外或 `Plugin attempted to register task while disabled`。
- 16:00:04（使用者貼自主控台，非 `latest.log`）：`TimerEvent: Hourly` → `Hourly Finished`，隨後 `[Town:Rome]`／`[Town:Berlin]` 各有 `Converted … beakers into … culture` 與 `Generated … culture`。這條路徑是 `EventTimerTask`（`asyncTimer`，每 5 秒）→ `HourlyTickEvent` → `asyncTask("cultureProcess", …)`，證明**週期性 async 計時器**與**具名 async 任務**經新排程器正常運作。
- 仍未直接證明：`syncTimer` 類（`SyncBuildUpdateTask`、`SyncLoadChunk` 等）。3.2 建造會走 `SyncBuildUpdateTask`，由 3.2 補證。

**3.2（2026-10-06 16:11–16:23，`latest.log` 至 455 行）**
- 流程：`/build monument`（16:14:06 開工，`hammerrate 99999` 後 10%→100%，16:15:50 完成）；`/build demolish world,-320,71,304`（Barracks，16:17:10）與 `/build demolish world,-320,70,288`（Farm，16:22:06）；重新 `/build barracks`（16:22:30 開工、16:23:06 完成）。
- 建造進度由 async 執行緒回報、方塊由 `SyncBuildUpdateTask` 逐 tick 貼出，兩者都正常，所以 **`syncTimer` 類計時器有運作**（補證 3.1）。
- 日誌 16:11–16:23 無任何例外或 stack trace。
- 使用者回報（目視）：(a) 新建 Barracks 的箱子其中**一個朝向反了**；(b) `/build demolish` 後，原建地**破損**且**殘留建築方塊**。
- 讀碼與日誌的觀察：
  - 拆除走 `Structure.delete…` → `undoFromTemplate()` → `BuildUndoTask`（async 讀備份、`SyncBuildUpdateTask.queueSimpleBlock` 貼回）。完成後 `BuildUndoTask` 會刪除該 `templates/undo/<town>/<corner>` 備份，所以拆除後目錄裡找不到 `world,-320,71,304`、`world,-320,70,288` 是正常的；日誌也沒有 `FileNotFound`／`fancyDestroyStructureBlocks` 的例外路徑。
  - 拆除時出現 3 筆 `Couldn't get sync build update lock, skipping until next tick.`（16:17:12–14）。這是 `SyncBuildUpdateTask` 取不到鎖而延後一 tick，屬既有機制；是否原本就會出現尚未對照。
  - 箱子朝向來自 `PostBuildSyncTask` 的 `/chest` 分支：`CivData.convertSignDataToChestData` 只轉換 4 種看板方向（其餘值不轉換），再由 R2 加入的 `ItemManager.reapplyData` 寫回。**R3 的 7 個修改檔（`CivCraft`、`Blacksmith`、`Buildable`、`TaskMaster`、`EspionageMissionTask`、`PlayerLoginAsyncTask`、`BukkitObjects`）都不含這條路徑**；`Buildable` 與拆除相關的改動只是把 `BukkitObjects.scheduleAsyncDelayedTask(task, 0)` 換成等價的 `TaskMaster.asyncTask(task, 0)`（兩者都呼叫 `runTaskLaterAsynchronously`）。
  - R2 紀錄（R2-B）在同一種建築上箱子朝向為 PASS，但只記錄「箱子朝向正確」，沒有區分每個箱子；拆除還原在 R2 與基準線都沒有測過，所以**不能直接當成回歸，也不能直接當成原本就壞**。
- 使用者補充：反的是 **Roman Barracks「樓梯往下數第一個箱子」**。離線檢查 `civcraft_data/templates/themes/roman/structures/barracks/barracks_{north,east,south,west}.def`：
  - 每個朝向只有 **1 個** `/chest` 看板行（`id:0`，由 `PostBuildSyncTask` 轉成箱子）；其餘 20 個左右的箱子是藍圖裡寫死的普通箱子方塊（id 54），朝向來自藍圖 data，不經過 R2/R3 改過的程式。
  - 藍圖本身的箱子 data 並不一致：例如 `barracks_east`／`barracks_west` 內 `2:5:8` 與 `3:5:8` 為 data 2，上一層 `2:6:8`、`3:6:8` 卻是 data 3，`2:1:17`、`3:1:17`（data 2）與 `2:2:17`、`3:2:17`（data 3）同理；`barracks_north` 的 `14:1:2`／`14:2:2` 等為 data 5。因此「某個箱子朝向看起來反了」**有可能是藍圖資料本身如此**（或 1.12 雙箱合併時自動調整朝向），尚未證實。
  - 不知道使用者建的是哪個朝向的藍圖，所以還不能指出是哪一格的哪個 data。
- 歸因方法（待做）：把 `plugins/CivCraft.jar` 換成 R2 版（HEAD `dc9da469`，不含 R3），在相同位置與朝向重做「建 Barracks → 檢查箱子 → demolish」，結果相同即為原本就有、與 R3 無關。

**3.6 與 R2 對照的部分資料（`latest.log` 16:28:37–16:33:54，共 424 行）**
- `plugins/CivCraft.jar` 在 16:25:59 被換成 Claude 暫存的 **R2 jar**（與 `Temp\r2stage\CivCraft.jar` 逐位元組相同，內無 `BukkitTaskScheduler`）；使用者的 R3 jar 另存為 `plugins/CivCraftR3`（15:18:22，內有 R3 類別）。因此 16:28:37 起這一輪是 **R2**，不是 R3。
- R3 那一輪的關閉（`logs/2026-10-06-13.log.gz`，16:28:00）：`Stopping the server` → `Disabling CivCraft v1.8.0-Beta6` → 依序停用 Herochat、Vault、WorldBorder、dynmap → `Saving players` → `Saving worlds`；全程無例外、無 `attempted to register task while disabled`。
- R2 那一輪（16:28:37 起）：16:29:28 `/build demolish world,-336,91,288` 同樣印出 `Delete with Undo! Barracks`，隨後 **9 筆** `Couldn't get sync build update lock, skipping until next tick.`（R3 那次是 3 筆）；重建 Barracks 16:30:10–16:31:11 完成；16:33:52 `/stop`，關閉同樣乾淨。→ **`Couldn't get sync build update lock` 在 R2 也會出現，確認不是 R3 引入的**。
- 這一輪沒有看到使用者對 R2 下箱子朝向與拆除殘留是否重現的回報，所以 3.2 的兩個問題仍未歸因。

**3.6 乾淨重跑（`latest.log` 16:35:24–16:35:32，386 行）**
- `plugins/CivCraft.jar` 為使用者的 Maven R3 建置（15:18:22，1,806,725 位元組，含 `BukkitTaskScheduler`）；`plugins/CivCraftR3` 備份已不存在（已改回 `CivCraft.jar`）。
- 啟動 `Done (2.992s)`，無 CivCraft 例外；啟動 4 秒後主控台 `stop`：`Stopping server` → `Disabling CivCraft v1.8.0-Beta6` → Herochat、Vault、WorldBorder、dynmap → `Saving players` → `Saving worlds` → 三個世界 `Saving chunks`。`grep` 例外／`attempted to register`／`SEVERE` 皆無。
- 關閉後 port 25565 無監聽（`netstat` 只剩舊連線 `TIME_WAIT`）。
- 沒有驗證：關閉前有未寫入的存檔時是否寫完（需要 `/town deposit` 等操作後立即 `stop`，再重啟比對金額）。

**3.7（`latest.log` 16:37:39 啟動，16:38:00 `/reload`，746 行）**
- 事前：16:37:46 登入，16:37:55 `/town deposit 100`，16:37:57 登出，16:38:00 主控台 `/reload`。（沒有記錄 deposit 前的金額，所以「存檔是否寫完」仍無法比對。）
- 停用：`[CivCraft] Disabling CivCraft` → Herochat、Vault、WorldBorder、dynmap；16:38:01 dynmap 存檔完成。
- 16:38:04 兩筆 `Nag author … about the following: This plugin is not properly shutting down its async tasks when it is being reloaded. This may cause conflicts with the newly loaded version of the plugin`。這是 `CraftServer.reload` 在停用所有插件後最多等 2.5 秒（日誌 16:38:01 → 16:38:04 吻合）、仍有 `BukkitWorker` 存活時印出的。
- 重新啟用：`Enabling CivCraft` → 設定、SQL 連線、載入 2 Civs / 2 Towns / 4 Residents / 42 TownChunks / 5 Structures 等 → `dynmap-civcraft enabled` → `Reload complete.`。Herochat 的 `NoSuchMethodError` 與 CustomMobs 的自行停用與啟動時相同，非 R3 造成。
- 讀碼上的可能原因（**未證實**，日誌無法指出是哪兩個工作）：`Bukkit` 在 `onDisable` 返回後本來就會 `cancelTasks(plugin)`，但**取消不會中斷已在執行的 async 工作**。若 async 工作剛好卡在等待 sync 端回應，而 sync 計時器已被取消，就會一直等下去：`CivAsyncTask` 的 `while(!request.finished) await(5000)`（行 87、119、172、201）、`MultiInventory`（行 145）、`StructureValidator` 的無逾時 `this.wait()`（行 266）。`SQLUpdate.run` 會在 `isDisable` 後結束迴圈，`StructureValidationChecker` 要啟動後 120 秒才開始，所以兩者在 16:38:00（啟動後約 21 秒）不太可能是這兩個。
- 結論：**`/reload` 可用但不乾淨**；`stop` 不受影響（JVM 結束時一併結束）。

---

## 4. 已知問題與留待事項

- **更正（R3.3 的效果）**：先前紀錄寫「`onDisable` 原本不取消任務」不完全正確。Bukkit 在 `onDisable` 返回後會自動 `cancelTasks(plugin)`，所以 R3 的 `TaskMaster.stopAll()` 對「任務會不會被取消」沒有新增效果；它的實際作用只是**把取消提前到 `SQLUpdate.save()` 之前**（避免關閉流程中仍有計時器排入新存檔）並清空 `TaskMaster` 的名稱表。已執行的 async 工作不會被取消（見上方 3.7）。
- **待處理（候選 R3.5）**：讓卡在等待 sync 回應的 async 工作能在 `CivCraft.isDisable` 時結束：給 `CivAsyncTask` 的等待迴圈、`MultiInventory`、`StructureValidator.wait()` 加上 `isDisable` 檢查／逾時。屬核心執行緒行為變更，需要使用者同意；改完以 `/reload` 是否還印 `Nag author` 驗證。

- **擱置（2026-10-06，使用者決定）**：(a) Roman Barracks「樓梯往下數第一個箱子」朝向反了；(b) `/build demolish` 後建地破損、殘留建築方塊。尚未用 R2 jar 對照，**未確認是否為 R3 造成**；讀碼上 R3 的 7 個修改檔不在這兩條路徑（見 §3 的 3.2 實測紀錄）。要查時：以 `HEAD dc9da469`（R2）重建 jar，在同位置同面向重做「建 Barracks → 檢查箱子 → demolish」；並對照藍圖中箱子 data 的不一致（`roman/structures/barracks/*.def`）。

- `SQLUpdate.run` 迴圈在 `isDisable` 後直接 `break`，不排空佇列；殘餘由 `onDisable` 的 `SQLUpdate.save()` 寫入。與進行中的一筆 `saveNow` 可能重複寫入同一物件（冪等，無資料風險，但未驗證）。→ R6。
- `Bukkit.getOnlinePlayers()`、`getPlayer` 的 async 讀取在現代 Paper 較嚴格，列入 U1 前再審。
- 第 3 節全部待測；測完後把本檔狀態改為「已驗證」，並更新 ROADMAP。
