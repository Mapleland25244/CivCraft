# CustomMobs 在 CivCraft 的作用與替代方案

> 更新：2026-10-09 ｜ 結論：**不再使用 CustomMobs 插件**（上游停止更新），改由 CivCraft 內建的魔物生成點取代。
> 相關：[ARCHITECTURE §11](ARCHITECTURE.md) ｜ [ROADMAP §6](ROADMAP.md)

## 1. CustomMobs 對 CivCraft 的作用

CivCraft 只用 CustomMobs 做一件事：**在固定座標建立或移除怪物生成點**。怪物的外觀、血量、技能、掉落都由 CustomMobs 自己的設定決定，CivCraft 不介入。

### 1.1 唯一的 API 接觸點

`integration/CustomMobsProvider.java` 實作 `MobSpawnProvider.setSpawnerActive(mobName, loc, active)`：

| 動作 | CustomMobs API |
|---|---|
| 取得編輯器 | `CustomMobsAPI.getSpawnerEditor()` |
| 查座標是否已有生成點 | `spawnerEditor.getSpawner(loc)` |
| 啟用 | `CustomMobsAPI.getCustomMob(mobName)` + `spawnerEditor.setSpawner(mob, loc, 60)` |
| 停用 | `spawnerEditor.resetSpawner(loc)` |

沒有監聽 CustomMobs 事件，也不生成、刪除或修改個別怪物。

### 1.2 CivCraft 自己在其上建立的功能

| 功能 | 位置 | 說明 |
|---|---|---|
| 生成點資料 | `object/MobSpawner`、資料表 `MOB_SPAWNERS` | 名稱、座標、啟用狀態、所屬建築 ID；啟動時由 `CivGlobal.loadMobSpawners()` 載入 |
| 類型定義 | `data/spawners.yml`、`ConfigMobSpawner` | 陸地 8 族（yobo、ruffian、behemoth、undead、kodiac、cube、arachne、direwolf）＋水域 kraken，各有 lesser／greater／elite／brutal 四級，稀有度 1.0／0.3／0.1／0.05 |
| 世界生成 | `MobSpawnerPopulator`、`MobSpawnerPreGenerate`、`MobSpawnerPick` | 依固定種子預先算位置（間隔 15–30 區塊、半徑 625 區塊），區塊生成時放置標記方塊與告示牌 |
| 建築連動 | `Buildable.disableMobSpawner/enableMobSpawner` | 在生成點上蓋建築會關閉該生成點並記錄所屬文明；拆除後重新啟用 |
| 管理指令 | `/dbg mobspawnergenerate`、`/dbg createmobspawner <id>` | 重新產生全部生成點（會 `TRUNCATE` 資料表）、手動建立單一生成點 |
| 總開關 | `spawners.yml` 的 `enable` | 為 false 時不載入、不啟用生成點 |

### 1.3 玩法意義

魔物巢穴散布在世界各處，玩家蓋城鎮或建築時會壓掉巢穴，成為「清除怪物」的一環。這是領地與建築系統的一部分，所以**取代時必須保留「建築開關生成點」的行為**，不能只是讓怪物出現。

### 1.4 為什麼要移除

- CustomMobs 4.17 只支援 v1_9–v1_11，在 1.12.2 起就自行停用，所以此功能在 1.12.2 基準與 1.13.2 測試中一直是 N/A。
- 上游已不再更新，不可能追上新版 Minecraft。
- 它曾是 `plugin.yml` 的 hard `depend`，裝了用不了、沒裝又無法載入。

## 2. 替代方案評估

| 方案 | 優點 | 缺點 | 結論 |
|---|---|---|---|
| **內建實作（採用）** | 無外部依賴；介面已存在（`MobSpawnProvider`）；行為完全可控 | 怪物外觀與技能要自己做，目前只有原版實體加屬性倍率 | ✅ 已實作 `NativeMobSpawnProvider` |
| **MythicMobs** | 事實標準，免費版涵蓋大部分需求，內建 spawner、技能、掉落；有 Maven 倉庫 `mvn.lumine.io` | 官方 API 文件只示範 `mob.spawn()`，**未記載以程式建立／移除 spawner**；要靠指令或自行以 `spawn()` 實作計時，版本支援需另查；非 CivCraft 專屬 | 日後想要豐富怪物技能時的升級路線 |
| **TheMob** | Paper／Spigot，YAML 設定，主打效能與多階段 Boss | 是怪物／Boss 系統，不是座標型 spawner 管理；API 與穩定度未查證 | 次要候選 |
| **CustomSpawners／Mob Creator 等** | 可建立自訂生成點或怪物 | 更新狀況、API 都未查證 | 不建議 |

> 搜尋結果出處：[MythicMobs API](https://wiki.mythiccraft.io/mythicmobs/API)、[MythicMobs on Hangar](https://hangar.papermc.io/MythicCraft/MythicMobs)、[TheMob](https://modrinth.com/plugin/the-mob)。版本支援範圍與 API 細節只做了表面查閱，採用前需實測。

### 2.1 建議

1. **現在**：內建實作，零依賴，保住「建築開關生成點」的玩法。
2. **之後若要豐富怪物**：保留 `MobSpawnProvider` 介面，新增 `MythicMobsProvider`（以 `getMobManager().getMythicMob(id).spawn()` 搭配 CivCraft 自己的計時），內建實作當作後備。因為 MythicMobs 沒有文件化的 spawner API，**計時與上限控制要留在 CivCraft 這一側**，這正是內建實作已經做好的部分。

## 3. 內建實作設計（`NativeMobSpawnProvider`）

- **類型對應**：spawner id 為 `<family>_<tier>`。

  | family | 實體 | | tier | 倍率 |
  |---|---|---|---|---|
  | yobo | ZOMBIE | | lesser | ×1 |
  | ruffian | WITCH | | greater | ×2 |
  | behemoth | HUSK | | elite | ×4 |
  | undead | SKELETON | | brutal | ×8 |
  | kodiac | POLAR_BEAR | | | |
  | cube | SLIME（尺寸隨等級） | | | |
  | arachne | SPIDER | | | |
  | direwolf | WOLF（憤怒） | | | |
  | kraken | GUARDIAN（水中） | | | |

  實體對應是依名稱推測，**不是原 CustomMobs 設定的還原**（原設定不在 repo 內），需要調整時改 `FAMILIES` 表。
- **倍率**：套用到最大血量與攻擊傷害（實體沒有該屬性則略過）。
- **行為**：每 100 tick 檢查一次；40 格內有玩家、所屬怪物少於 4 隻、區塊已載入時，在生成點周圍 4 格內找安全位置生成 1 隻。陸地要求腳下為固體且身體空間無方塊與液體，水域要求兩格皆為水。
- **執行緒**：世界操作只在 sync 計時器內；`setSpawnerActive` 只改記憶體 map，可從任何執行緒呼叫。停用時待移除的怪物放入佇列，下一次計時器再處理。
- **關閉**：`Integrations.shutdown()`（`onDisable`）移除所有已生成的怪物。
- **總開關**：沿用 `spawners.yml` 的 `enable`。旗標 `hasCustomMobs` 已改為 `hasMobSpawners`。

### 3.1 自行生成（Ambient）與開關

依維基描述：自訂怪物除了生成點，也會在**文明文化範圍內的玩家周圍**生成，且不會生成在農田區塊或水上。

- 設定在插件的 `config.yml` 的 `ambient_mobs:` 區塊，`ambient_mobs.enable: false` 即可關閉；固定生成點由 `data/spawners.yml` 的頂層 `enable` 另外控制，兩者互不影響。舊伺服器的 `config.yml` 沒有此區塊時，預設為開啟。
- 參數：`interval_seconds`（檢查間隔）、`chance`（每次檢查嘗試的機率）、`min_distance`／`radius`（離玩家的距離）、`max_per_player`（玩家附近上限）。
- 規則：玩家須在文化範圍內、非創造／旁觀模式；候選位置須在文化範圍內、不在農田區塊、腳下為固體、身體空間無方塊與液體。
- 怪物等級依**生態域**決定（維基的生態域表，已換成 1.13 生態域名稱），目前只涵蓋 yobo、ruffian、behemoth 三族；其他生態域不生成。
- 關閉伺服器時一併移除自行生成的怪物。
- 通用掉落表（`MobSpawnDropListener`）與血量（yobo 20/25/30/40、ruffian 10/15/20/30、behemoth 75/125/150/175）取自維基。

### 3.2 已知限制

- 怪物沒有自訂技能、裝備、掉落；強度只靠倍率。
- 重啟後怪物追蹤清單不保留；關閉時已清除，崩潰時可能留下少量帶名稱的怪物，靠 `setRemoveWhenFarAway(true)` 自然消失。
- 每個生成點每次檢查最多生成 1 隻，沒有全域上限；生成點數量大時需實測 TPS。
- 此實作**尚未編譯或在伺服器上執行**。

## 4. 移除清單

| 項目 | 狀態 |
|---|---|
| `NativeMobSpawnProvider`、`Integrations` 接線、`onDisable` 的 `shutdown()` | 已完成（未編譯） |
| `plugin.yml`：CustomMobs 由 `depend` 改為 `softdepend` | 已完成 |
| 刪除 `CustomMobsProvider.java` | 待做 |
| `Integrations` 移除 CustomMobs 分支，`hasCustomMobs` → `hasMobSpawners` | 待做 |
| `plugin.yml` 移除 softdepend 中的 CustomMobs | 待做 |
| `pom.xml`、`.classpath`、`.gitignore`、`local-libs/README.md` 移除 CustomMobs jar | 待做 |
| ARCHITECTURE §11、ROADMAP §6 同步更新 | 待做 |

## 5. 驗收

1. 全專案 `grep -r "hellfirepvp\|CustomMobs"` 只剩本文件與歷史測試紀錄。
2. 不裝 CustomMobs 啟動，日誌沒有 CustomMobs 警告，`Loaded N Mob Spawners` 正常。
3. `/dbg createmobspawner yobo_lesser` 後靠近，能看到怪物生成且不超過 4 隻。
4. 在生成點上蓋建築 → 怪物停止出現；拆除 → 恢復。
5. 關閉伺服器後世界中沒有殘留的生成點怪物。
