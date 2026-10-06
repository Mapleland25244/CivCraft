# CivCraft R4（NMS 與 NBT 隔離）測試紀錄

> 更新：2026-10-06 ｜ 範圍：把 `net.minecraft.server`／`org.bukkit.craftbukkit` 的使用收進 `nms` 套件 ｜ 狀態：**程式完成、javac 通過；伺服器部分驗證（4.1、4.2、4.3、4.5 PASS；4.6 受既有 bug 阻擋；4.7–4.11 未觸發；4.4 未回報；4.1 PASS）**
> 對照：[基準線](00-baseline-1.12.2.md)。本檔只記錄改動後的結果，不覆蓋基準線。
> 慣例與索引：[README](README.md) ｜ 設計：[ROADMAP §3](../ROADMAP.md)

結果欄位：`PASS` / `FAIL` / `原本就壞` / `N/A` / `待測`。

## 1. 改動摘要

### 1.1 結構
- 新增 `com.avrgaming.civcraft.nms`：`NmsAdapter`（介面，只用 Bukkit 型別）、`ItemNbt`、`HorseAccess`、`AttributeData`、`Nms`（依伺服器 CraftBukkit 套件版本挑 adapter）。
- 新增 `com.avrgaming.civcraft.nms.v1_12_R1`：`NmsAdapter_v1_12_R1`、`ItemNbt_v1_12_R1`、`HorseAccess_v1_12_R1`。**全專案只有這三個檔案 import 伺服器內部類別。**
- `CivCraft.onEnable` 在最前面呼叫 `Nms.init()`；找不到對應版本的 adapter 就記錄錯誤並停用插件，不再等到某個功能才出 `NoClassDefFoundError`。

### 1.2 逐檔處理（原本 11 個檔案碰伺服器內部，ARCHITECTURE 記為 9 個，另有 `Reflection`、`FireworkEffectPlayer` 的註解）

| 原檔 | 處理 |
|---|---|
| `gpl/AttributeUtil` | 公開 API 不變（46 個檔案使用），內部改呼叫 `ItemNbt`；`Attribute` 改用 `AttributeData`；移除公開欄位 `nmsStack`（無外部使用）。NBT 鍵（`display`、`AttributeModifiers`、`civcraft`、`item_enhancements`、`civ_enhancements`、`HideFlags`、`SkullOwner`）**逐字保留**，既有玩家物品照常讀取 |
| `gpl/HorseModifier` | 公開 API 不變，內部改呼叫 `HorseAccess`；反射與混淆名稱（`b`、`a`、`f`）原樣搬到 `HorseAccess_v1_12_R1` |
| `gpl/ImprovedOfflinePlayer` | **刪除**：全專案沒有任何呼叫端。同時讓 `material-getid` 棘輪歸零 |
| `util/NBTStaticHelper` | 刪除（只被上面兩個檔案使用） |
| `util/Reflection` | 刪除；唯一呼叫端 `CivMessage.itemTooltip` 改用 `Nms.get().itemToJson` |
| `util/EntityProximity` | 改為轉呼叫 `NmsAdapter.getNearbyEntities`；篩選型別由 NMS 的 `EntityPlayer.class` 改為 Bukkit 的 `Player.class`（`CannonProjectile` 是唯一呼叫端） |
| `components/ProjectileComponent` | `canSee` 改用 `NmsAdapter.isLineClear` |
| `listener/BlockListener` | 發情計時（`InLove`）、Ruffian 攻擊力、Ruffian 範圍傷害改用 `NmsAdapter`；移除一段無效程式（見 §1.3） |
| `siege/CannonProjectile` | 篩選型別改 `Player.class` |
| `structure/Stable` | `HashTreeSet`（CraftBukkit 工具類別）改 `HashSet`，與 `Pasture`、`Battledome` 相同；該欄位只有 add／iterate／clear |
| `util/PlayerBlockChangeUtil` | 移除未使用的欄位與 import（用到它的程式早已被註解掉） |

### 1.3 刻意的行為差異
- `BlockListener` 的 `CreatureSpawnEvent` 內，雞的分支有 `new NBTTagCompound().getBoolean("IsChickenJockey")`：對**新建的空 compound** 讀取，永遠是 `false`，所以那個 `if` 從來不會成立。已移除，不改變行為。
- `AttributeUtil.values()` 原本回傳活的視圖，現在回傳快照；`AttributeUtil.clear()` 在物品沒有 NBT 時不再 NPE。兩者都沒有外部呼叫端（唯一外部使用是 `add(...)` 與 `removeAll()`）。
- `AttributeUtil.addEnhancement` 對保留字 `name` 的檢查提前到寫入前；仍丟 `IllegalArgumentException`，條件不變。

### 1.4 檢查
- 新增 `tools/check-nms.sh`：`civcraft/src` 與 `civcraft_dynmap` 內，`nms/` 以外不得出現 `net.minecraft.server`、`org.bukkit.craftbukkit`、`v1_x_Rx`（註解行除外），已接入 `tools/pre-commit-check.sh`。
- 棘輪基準 `material-getid` 由 1 降為 0（已鎖定）。

## 2. 靜態驗證

| 項目 | 結果 |
|---|---|
| javac（JDK 8，`-source 8`，`civcraft/lib/*.jar`＋`itag-stub`，不含 `unittests`） | 通過，無錯誤 |
| `tools/check-nms.sh` | OK |
| `tools/check-scheduler.sh` | OK |
| `tools/check-legacy-api.sh` | 全部不超過基準 |

沒有跑 Maven（本機沒有 `mvn`）；jar 需由使用者建置。

## 3. 伺服器驗證項目

| # | 項目 | 預期 | 結果 |
|---|---|---|---|
| 4.1 | 啟動 | 無例外；日誌沒有 `no NMS adapter` | PASS：`plugins/CivCraft.jar`（17:00）內含 `nms/` 類別、不含 `ImprovedOfflinePlayer`／`NBTStaticHelper`／`Reflection`；17:01:18 Enabling → 17:01:20 `Done (3.001s)`，無 `no NMS adapter`、無 `NoClassDefFoundError`、CivCraft 無例外（只有已知的 CustomMobs 自行停用與 Herochat 啟動失敗）；17:01:25 整點計時器、17:03 起 structure validate 正常 |
| 4.2 | 取得自訂物品（`/ad item` 或 SpawnItem GUI）並檢查名稱、lore、屬性（攻擊／血量／速度） | 與基準線相同；`AttributeModifiers` 顯示正常 | PASS（使用者回報成功） |
| 4.3 | **既有物品相容**：用 R3 之前建立的自訂物品（含附魔／靈魂綁定／`civcraft` 屬性）在新 jar 上檢查 | 仍被識別、附魔效果存在 | PASS（使用者回報成功；最重要的一項） |
| 4.4 | 皮革染色物品（`LeatherColor`）、頭顱（`setSkullOwner`）、`HideFlags` | 與基準線相同 | 待測 |
| 4.5 | 教學書／`/tutorial`（`CivTutorial` 呼叫 `removeAll`） | 屬性不顯示 | PASS（使用者回報成功） |
| 4.6 | Stable：購買馬、騾；`setHorseSpeed`；騎乘 | 速度正確、`isCivCraftHorse` 判斷正常、`openInventory` 正常 | **無法驗證（既有 bug，見 §4）**：買騾成功（扣款 2500），右鍵後出現 `Invalid horse!`；未買馬 |
| 4.7 | 野外動物繁殖限制（Pasture 外對動物右鍵餵食） | 不在牧場：顯示錯誤；牧場內發情中的動物不會重複餵食 | 未觸發（測試環境沒有牧場） |
| 4.8 | 砲台／箭塔 `canSee`（`ProjectileComponent`）：玩家被牆擋住與沒被擋住 | 擋住時不開火 | 未觸發（需要敵對玩家與塔，條件難以造出） |
| 4.9 | 砲彈爆炸對附近玩家的傷害（`CannonProjectile`，`Player.class` 篩選） | 範圍內玩家受傷；非玩家實體不受影響 | 未觸發（條件難以造出） |
| 4.10 | Ruffian 女巫投擲藥水的範圍傷害（`/dbg` 或 CustomMobs；CustomMobs 在 1.12.2 已自行停用，可能無法觸發） | 範圍內玩家受傷，會觸發 `EntityDamageByEntityEvent` | 未觸發（CustomMobs 在 1.12.2 自行停用） |
| 4.11 | 聊天中的物品懸浮提示（`CivMessage.send(sender, line, item)`） | 滑鼠移上去顯示物品 | 未觸發（未找到容易觸發的流程） |

### 實測紀錄（2026-10-06，使用者回報）

- 4.1：見表。4.2、4.3、4.5 成功。4.4 未回報，維持待測。
- 4.6 補充（`latest.log` 17:21:49、17:22:26）：兩次 `[DEBUG] Player tried using Horse without meta: civcrafthorse`，正是 `isCivCraftHorse` 缺少 metadata 的分支，印證下方「既有 bug」的成因；也排除 `isHorse` 判斷失敗的可能。
- 4.6（17:19–17:22 日誌）：建造 Stable（`/build`，位置驗證 0% 但因 OP 放行）、`hammerrate` 後完成；買騾 3 次（每次 `Paid 2500.0 Coins`）；右鍵騾兩次出現 `Invalid horse! You can only get horses from stable structures.`。沒有買馬（5000）。
- 4.7：測試環境沒有牧場。4.8–4.11：條件難以造出。以上皆未觸發，**不代表通過**；這些路徑的改動是機械式搬移，只以 javac 與 `check-nms.sh` 驗證沒有漏改。

## 4. 已知問題與留待事項

- **既有 bug（非 R4 造成，未修，日後獨立修復）：Stable 買的馬／騾右鍵會被判定為 Invalid horse 並被移除。**
  - `BlockListener.OnPlayerInteractEntityEvent` 對 `EntityType.HORSE` 呼叫 `HorseModifier.isCivCraftHorse`；該函式要求實體帶 `civcrafthorse` metadata，只有 `HorseModifier.setCivCraftHorse` 會設。
  - `git grep` 顯示 R3（`HEAD`）與 `1.8-dev` 上 `setCivCraftHorse` 都沒有任何呼叫端（`Stable` 買馬後未呼叫）；該函式由 `59d81a64 Fix Horse NBT and add metadata` 加入。所以 Stable 的馬必定被擋，騾也一樣。
  - 騾的另一個問題（**推測，未在伺服器確認**）：`Stable` 先生成 `EntityHorse` 再把 NBT `Type` 改成 MULE，1.11 以後騾是獨立實體類別，所以得到的大概仍是馬，才會落入上述檢查；騾分支也沒有 `setTamed`／`setSaddled`。
  - 未做的對照：買一匹馬（5000）確認同樣被擋；以 R3 jar 重做買騾。最小修法是 `Stable` 買馬後呼叫 `setCivCraftHorse`，騾需另外處理。

- 本步驟**沒有**引入 `PersistentDataContainer`，也沒有改任何 NBT 鍵：「舊 NBT 雙讀」是 U2 的工作，R4 只保證現有格式不變。
- `HorseAccess_v1_12_R1` 保留了原作者程式裡的可疑處（`setArmorItem`／`getArmorItem` 對 `invoke(this, …)` 傳錯物件、會失敗並印出 stack trace）；目前沒有呼叫端用到這兩個方法，不在 R4 修。
- `civcraft_dynmap` 沒有 NMS 使用，已納入檢查腳本。
- 升級到 1.13+（U1）時，只需新增 `nms.v1_13_R*` 的 adapter 並在 `Nms.create` 加一個 case；`HorseAccess` 的反射實作預期要重寫（`EntityHorse` 在 1.13 後拆成多個類別）。
