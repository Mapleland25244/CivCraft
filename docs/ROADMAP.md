# CivCraft 重構與升級路線圖

> 更新：2026-10-06 ｜ 狀態：R2、R3 完成，R4 程式完成、部分驗證
> 依據：[ARCHITECTURE](ARCHITECTURE.md)（現況分析）、[基準線](testing/00-baseline-1.12.2.md)
> 本檔記錄決策與流程；執行結果記在 [測試紀錄](testing/README.md)。

## 目錄
- [1. 目標與決策](#1-目標與決策)
- [2. 版本無關設計原則](#2-版本無關設計原則為-26x-預留空間)
- [3. 兩條軌道](#3-兩條軌道)
- [4. R2 方塊/物品存取層](#4-r2-方塊物品存取層)
- [5. 風險與待查](#5-風險與待查)
- [6. 相依插件去留](#6-相依插件去留待決定)

---

## 1. 目標與決策

| 決策 | 內容 | 理由 |
|---|---|---|
| 升級終點 | **Paper 1.21.x** | 目前生態最新；Java 21 |
| 後續空間 | 保留升級到 Mojang 年份式新版本（**26.x**）的空間 | 見 §2 的「版本無關設計」 |
| 舊資料策略 | 藍圖（`.def`）、`materials.yml`、資料庫維持**數字 ID（舊 ID:data）**，載入時轉換 | 約 1275 個藍圖＋約 675 行物品定義，不改資料可回溯、可雙向驗證 |
| 順序 | 先在 1.12.2 上打接縫（Track R），再逐版升級（Track U） | 不同時改行為與版本 |
| 未決 | CustomMobs、TagAPI/iTag、HeroChat、Anti-Cheat 的去留 | 見 §6 |

「階段 A」定義：**把執行環境與 API 從 Spigot 1.12.2 升到 Paper 現代版本**，拆成多次小跳躍，每一跳用同一份基準線回歸。

---

## 2. 版本無關設計原則（為 26.x 預留空間）

1. **任何 Minecraft／Bukkit 版本差異只能出現在 `compat` 邊界**：`ItemManager`（方塊／物品／材質）、`nms` 套件（每版一個 adapter）、`integration` 套件（外部插件）。核心業務程式碼不得 import 版本相關類別。
2. **不寫死版本字串**（例如不用 `startsWith("1.21")`）；以功能偵測（feature detection）或 adapter 註冊表選擇實作，找不到相符 adapter 時啟動失敗並印出明確訊息，而不是靜默降級。
3. **盡量用 Bukkit／Paper 公開 API**，少用 NMS；必要的 NMS 集中、每版一個 adapter；改用 Mojang 官方對照（paperweight）以減少混淆名稱帶來的成本。
4. **相依以 `plugin.yml` 的 `libraries:` 載入**，取代 manifest `Class-Path`；不 shade 會與伺服器衝突的函式庫。
5. **Java 版本隨目標升級**，不在程式碼裡依賴特定 Java 版本的行為；26.x 要求的 Java 版本待其發布後確認（未驗證）。
6. **資料格式不綁版本**：資料庫與設計資料保存舊數字 ID，只在 `ItemManager` 的邊界轉換；新增的資料用字串鍵（Minecraft namespaced key）。

---

## 3. 兩條軌道

### Track R：在 1.12.2 上打接縫（行為不變，以基準線驗證）

| 步驟 | 內容 | 規模 | 狀態 |
|---|---|---|---|
| R0 | 可重現的 build、基準線 [基準線](testing/00-baseline-1.12.2.md) | S | 完成 |
| R1 | 外部插件整合層（`integration` 套件） | M | 完成（[Phase 1 紀錄](testing/01-phase1-integration.md)） |
| **R2** | **方塊／物品存取層：所有舊 API（數字 ID、`MaterialData`、舊 `Material` 常數、`getDurability` 當 data 用）只留在 `ItemManager` 邊界** | **L** | **完成**（見 §4；紀錄：[02-r2-legacy-api](testing/02-r2-legacy-api.md)） |
| **R3** | **排程與生命週期：`TaskMaster` 介面化、修 `cancelTimer`、`onDisable` 取消任務、審計 async 是否碰世界** | **M** | **完成（已驗證）**；未觸發：重生傳送、間諜失敗、產出；擱置：Roman Barracks 箱子朝向與拆除破損，未歸因（紀錄：[03-r3-scheduling](testing/03-r3-scheduling.md)） |
| **R4** | **NMS 與 NBT 隔離：伺服器內部類別只留在 `nms` 套件（`NmsAdapter`／`ItemNbt`／`HorseAccess`，每版一個 adapter）；`AttributeUtil`、`HorseModifier` 成為外殼，NBT 鍵逐字保留；`tools/check-nms.sh` 接入提交前檢查** | **M** | **程式完成，部分驗證**：啟動與物品 NBT（4.1、4.2、4.3、4.5）PASS；Stable 馬被既有 bug 擋住（日後獨立修復）；牧場、塔、砲、Ruffian、聊天物品提示未觸發（紀錄：[04-r4-nms](testing/04-r4-nms.md)） |
| R5 | 指令層：頂層指令每次執行建新實例、方法查找快取並於啟動時檢查 `_cmd`、補上真正的 `TabCompleter` | S–M | 待做 |
| R6 | 持久化：收斂 12 個直接 JDBC 的檔案、`SQLUpdate` 去重、盤點 DB 內序列化的物品 | L | 待做 |
| R7 | `CivSettings` 型別化門面 | L | 待做 |
| R8 | `CivGlobal`、`Town`、`Buildable` 瘦身 | XL | 待做 |
| R9 | 自動化測試（Template 解析與對照表完整性、稅與債務、Localize key、SQL 字串、CommandBase 分派） | M | 待做 |

R2、R3、R4 是升級的前置；R5 以後可與升級交錯進行。

### Track U：升級跳躍

| 步驟 | 內容 | 前置 | 主要風險 |
|---|---|---|---|
| U0 | 建置依賴改為 Paper API 1.12.2（Spigot API 的超集），伺服器仍為 1.12.2。**程式完成，待驗證**：`paper-api` 取代 `spigot-api`，NMS 仍用 BuildTools 的 `spigot`；javac、Maven 建置、Paper 1.12.2 啟動皆 PASS，基準線回歸（U0.3）擱置，已打 tag `u0-paper-api`（紀錄：[05-u0-paper-api](testing/05-u0-paper-api.md)） | R1 | 低 |
| U1 | **1.13**（壓平）：`api-version`、`Material` 改名、不再用 `MaterialData`／short data、NMS adapter、藍圖載入時轉換 | R2、R3、R4 | 最高：物品 NBT、DB 內序列化物品格式。**U1a 程式完成（分支 `u1-1.13`），javac／Maven／離線檢查通過，伺服器待驗證**（紀錄：[06-u1a-1.13.2](testing/06-u1a-1.13.2.md)） |
| U2 | **1.16.5**（Java 8 最後一版）：`libraries:` 載入相依、NBT 改 `PersistentDataContainer`（雙讀舊資料） | U1 | 中 |
| U3 | **1.17–1.20.4**（Java 17）：BoneCP 換 HikariCP、NMS 改 Mojang 對照、評估 Paper 的舊插件支援 | U2 | 中高 |
| U4 | **1.21.x**（Java 21，目前終點） | U3 | 中 |
| U5 | **26.x**（預留）：只新增 adapter、更新相依，不改核心 | U4 | 待其發布後評估 |

### 每次跳躍的固定流程
1. 備份 `civ_game` 與世界；世界升級先在**複製的世界**上試。
2. 開分支，只改該版本需要的部分（不同時改行為）。
3. 複製 [基準線](testing/00-baseline-1.12.2.md) 為 `testing/NN-<名稱>.md`，全部回歸，與基準線逐項對照。
4. 通過後合併並打 tag；失敗則回滾到上一個 tag。

---

## 4. R2 方塊／物品存取層

### 4.1 現況盤點（grep 實測，概估）
- `ItemManager` 已是集中點：**約 798 處呼叫 / 97 檔**；其中 `getId(Material.*)` 175 處、`setTypeId/setData/setTypeIdAndData` 270 處、`getData` 55 處、`getBlock*`／`sendBlockChange` 26 處。
- `CivData` 提供 147 種方塊／物品 ID 常數，使用約 569 處 / 70 檔。
- **洩漏（繞過 `ItemManager` 直接用舊 API）**：直接呼叫約 106 處（含註解與 `SimpleBlock`／`BlockSnapshot` 自己的 `getType/getData`），其餘主要是：
  - `MaterialData`／`org.bukkit.material.*`：`ArmorListener`、`CustomItemManager`、`ShowRecipe`、`LoreCraftableMaterial`、`MobSpawnerPopulator`、`TradeGoodPopulator`、`PostBuildSyncTask`、`TrommelAsyncTask`。
  - 舊命名 `Material` 常數：約 73 處 / 21 檔（`CROPS`、`SIGN_POST`、`STEP`、`SKULL_ITEM`…）。
  - `getDurability/setDurability`（當作 data 使用）：約 27 處。
  - `new ItemStack(int, …)`：約 4 處。
- 藍圖 `.def` 的格式是 `x:y:z,id:data`，載入點是 `Template`／`TemplateStream`。

### 4.2 設計
- **舊數字 ID:data 是 CivCraft 的「領域貨幣」**：在 1.12.2 上它直接對應原版；升級後，由 `ItemManager`（與一張「舊 ID:data → 目標版本材質」對照表）在邊界轉換。業務程式碼不必知道目標版本。
- R2 的目標不是換掉數字 ID，而是**讓舊 API 只出現在 `ItemManager`**。完成後 U1 只需要重寫 `ItemManager` 與對照表。
- 用腳本（`tools/check-legacy-api.sh`）把「邊界外的舊 API 使用數」做成**棘輪**：只能減少、不能增加，並逐步清零。

### 4.3 步驟
| 步驟 | 內容 | 驗證 |
|---|---|---|
| R2.1 | 建立檢查腳本 `tools/check-legacy-api.sh` 與基準 `tools/legacy-api-baseline.txt`；列出每個洩漏點 | 腳本輸出（**完成**） |
| R2.2 | 把直接呼叫 `getTypeId/setTypeId/getData/setData/getRawData` 收進 `ItemManager` | javac；基準線 2.3、2.2（**完成，待伺服器驗證**） |
| R2.3 | `MaterialData`／`org.bukkit.material.*` 的使用收進 `ItemManager`（方向、看板、發射器等） | 看板／箱子／發射器相關測項 |
| R2.4 | 舊命名 `Material` 常數改為透過 `compat/LegacyMaterials`（語意判斷與設定集合）取得 | javac；對應功能測項（**完成，待伺服器驗證**） |
| R2.5 | `get/setDurability` 分成「損耗」（`ItemManager.getDamage/setDamage`）與「變體」（`LegacyMaterials` 的語意判斷）；`new ItemStack(int,…)` 改走 `ItemManager.createItemStack` | 自訂物品、耐久、附魔（**完成，待伺服器驗證**） |
| R2.6 | 藍圖 id:data：確認解析與貼上已全部走 `ItemManager`／`SimpleBlock`（無需改程式）；新增離線盤點 `tools/scan-templates.sh` 與基準 `tools/template-blocks-baseline.txt`（**完成**，見 §4.5） | 2.8.8、2.3.1 |
| R2.7 | 棘輪歸零（剩 `gpl` 的 4 處併入 R4／R6），並把兩支檢查腳本接上提交前檢查（`tools/pre-commit-check.sh`、`.githooks/pre-commit`；`.gitattributes` 固定腳本為 LF）（**完成**） | 腳本；故意加入舊 API 會被擋 |

每個子步驟獨立 commit，**行為不變**，編譯通過並在伺服器上跑相關基準線測項。

### 4.4 R2 進度（棘輪基準，`tools/legacy-api-baseline.txt`）
| 類別 | 起始 | 目前 | 下一步 |
|---|---:|---:|---|
| raw-block-id（`getTypeId` 等） | 26 | **0** | 完成 |
| raw-block-data（`getData/setData`） | 54（多為 `SimpleBlock`／`BlockSnapshot` 的同名方法，屬領域型別） | **0** | 完成 |
| material-data（`MaterialData`、`org.bukkit.material.*`） | 6 | **0** | 完成（新增 `ItemManager.getDispenserFacing/setSignFacing/reapplyData/copyData`） |
| durability-as-data（`get/setDurability`） | 27 | 3 | 剩 `gpl/InventorySerializer`（物品序列化），併入 R6 |
| int-item-stack（`new ItemStack(int,…)`） | 3 | **0** | 完成 |
| legacy-material-const（舊命名 `Material` 常數） | 45 | **0** | 完成（新增 `compat/LegacyMaterials`；邊界擴大為 `ItemManager` 與 `compat/`） |
| material-getid（`Material#getId`） | — | **0** | 完成（R4 刪除未使用的 `gpl/ImprovedOfflinePlayer`） |

檢查腳本的限制：grep 看不到型別，`sb`、`bs`、`nextBs`、`nextBlock`、`commandBlock` 等慣用變數名視為 CivCraft 自己的 `SimpleBlock`／`BlockSnapshot` 而略過；若 Bukkit 物件剛好用這些名字會被漏掉。

---

### 4.5 藍圖資料盤點（R2.6，`tools/scan-templates.sh`）

實測（`civcraft_data/templates`）：

| 項目 | 數值 |
|---|---:|
| `.def` 藍圖檔 | 1274 |
| 方塊行（`x:y:z,id:data`） | 47,440,440 |
| 帶指令欄位的行（看板、箱子等） | 36,801 |
| 不同的 `id:data` 組合 | 708 |
| 不同的方塊 id | 158 |
| 座標超出標頭尺寸、id 超出 0–255、data 超出 0–15、格式錯誤 | 全部 0 |
| 在 1.12.2 找不到對應 `Material` 的 id | 0 |

- 解析（`TemplateStream.getSimpleBlockFromLine`）把 id、data 讀成整數放進 `SimpleBlock`（領域型別）；貼上走 `ItemManager.setTypeIdAndData`。`Template`／`TemplateStream` 沒有直接使用舊 Bukkit API（檢查腳本佐證），所以 R2.6 **不需要改 Java 程式**。
- 這 708 組合就是 U1 對照表必須涵蓋的清單；`tools/scan-templates.sh --check` 在新藍圖引入基準以外的組合時會失敗，掃描約需 1 分鐘。
- 執行期還會產生同格式的藍圖：`templates/undo/…`（建造前的方塊備份）與 `templates/inprogress/…`。既有伺服器上的這些檔案存的是**舊 id:data**，升級時必須能被讀回（見 §5）。

---

## 5. 風險與待查
- **DB 內序列化的物品**（`InventorySerializer`、貿易商品物品等）在壓平後格式可能改變；R6 前先盤點，U1 前要有遷移或雙讀方案。
- **自訂物品靠 lore＋NBT 識別**（`AttributeUtil`，46 檔）：既有玩家物品要能被新版讀取。
- **世界升級**（Mojang DataFixer）會改世界檔，務必先在複製世界上試。
- **執行期產生的藍圖檔**（`templates/undo`、`templates/inprogress`）保存舊 id:data：升級後仍要能讀回，或升級前先清空進行中的建造與可復原的紀錄。
- **async 任務碰世界**（41 檔使用 `asyncTask/asyncTimer`）：現代 Paper 對此更嚴格，R3 要審計。
- **授權不一致**（檔頭 proprietary 與根目錄 GPL）：重新發布前需釐清。

---

## 6. 相依插件去留（待決定）
| 插件 | 現況 | 建議 |
|---|---|---|
| CustomMobs | hard depend；4.17 在 1.12.2 就自行停用 | 降為可選或以 Paper 原生機制重做生成點 |
| TagAPI／iTag | 已無人維護 | 改用 scoreboard team／Paper API |
| HeroChat | 與 Vault 的環境問題未解 | 評估是否保留聊天頻道 |
| NoCheatPlus、VanishNoPacket、WorldBorder | 有新版 | 保留於整合層，升級時換版本 |
| TitleAPI | — | 以 Paper 的 `Player#sendTitle` 取代 |
| Anti-Cheat（`ACManager`、`WarAntiCheat`） | 依賴已無法控制的外部網站 | 在 U1 之前移除 |
| dynmap＋`civcraft_dynmap` | 3.0-beta 可用 | 等 R8 後配合 |

---

## 7. U1（1.13.2）執行計畫

> 決定（2026-10-06）：目標 **1.13.2**（NMS `v1_13_R2`）；**兩階段**；分支 `u1-1.13`（從 `ai-refactor` 開出，回滾點 tag `u0-paper-api`）。此分支**不再支援 1.12.2**。

### 7.1 現況盤點（以 Paper 1.13.2 API 試編譯，實測）
- 約 **212 個編譯錯誤、20 個檔案**（javac 在符號解析階段就停，進入型別檢查後還會有更多）：`LegacyMaterials` 55、`FisheryAsyncTask` 41、`CivGlobal` 19、`ItemManager` 18、`Template` 17、`CivTutorial` 13、其餘各 ≤7。
- 1.13 **移除**（不是棄用）：`ItemStack(int,…)`、`getTypeId`、`setTypeId`、`Material#getMaterial(int)`、`MaterialData(int,byte)`；`Material#getId()` 只剩 `LEGACY_*` 有效。
- 還沒收進邊界的舊名稱：`Biome` 改名（`ICE_MOUNTAINS`、`ROOFED_FOREST`、`SWAMPLAND`…，`CivGlobal`／`BiomeCache`）、`RAW_FISH` 等魚類（`FisheryAsyncTask`）、`Material` 在 `switch` 的 case 標籤。R2 的棘輪沒有涵蓋 `Biome`。
- `nms/v1_12_R1` 對 1.13.2 的 spigot 編譯**沒有錯誤**，但**執行期語意改變**：`display.Name` 在 1.13 起是 JSON 文字元件（舊為純字串）；`display.Lore` 到 1.14 才改 JSON，1.13 仍是純字串；物品 ID 壓平；實體類型、屬性名稱 `generic.*` 需實測。
- 可用的橋：`Bukkit.getUnsafe().fromLegacy(MaterialData)`／`fromLegacy(Material, byte)`（舊 → 新）、`toLegacy(Material)`；`Block#getData()`、`BlockState#getRawData()` 仍可讀舊 data。

### 7.2 U1a：在 1.13.2 跑起來（保留數字 ID:data，轉換交給伺服器的 legacy 表）
| 步驟 | 內容 | 驗證 |
|---|---|---|
| U1a.0 | 備份、複製世界與 DB 副本、Paper 1.13.2 測試伺服器（使用者準備）；`pom` 改 1.13.2（本分支已做） | — |
| U1a.1 | `compat/LegacyBridge`：id → `LEGACY_*` 材質表（掃 `Material.values()`），`(id,data)` → 新 `Material`／`BlockData`（快取），新 `Material` → `(id,data)` 反查（啟動時反向建表） | 離線：708 組藍圖組合全部能轉換且互相對得上（擴充 `scan-templates`） |
| U1a.2 | `ItemManager` 改用 `LegacyBridge`（`createItemStack`、`getId`、`setTypeIdAndData`、`getBlockTypeId` 等簽名不變） | javac；貼藍圖 |
| U1a.3 | `LegacyMaterials` 改成 1.13 名稱；`Biome`、魚類、`switch` case 洩漏補進 `compat`；`check-legacy-api.sh` 的規則改為 1.13 後的禁用清單 | javac；棘輪 |
| U1a.4 | `nms/v1_13_R2`：複製 adapter、`display.Name` 讀寫改 JSON、`Lore` 維持純字串（讀取同時接受 JSON 與純字串）；`Nms` 登記 `v1_13_R2`；刪除 `v1_12_R1`（可由 tag 取回） | 4.2、4.3 物品相容 |
| U1a.5 | `plugin.yml` 設 `api-version: 1.13`（**修正**：原計畫不設，但無 `api-version` 的插件會被伺服器以 legacy 模式重寫 `Material` 常數，與新名稱衝突）；在 Paper 1.13.2 啟動、跑 [基準線](testing/00-baseline-1.12.2.md) | 新紀錄 `06-u1a-1.13.2.md` |

### 7.3 U1b：換成新 API
設 `api-version: 1.13`、移除 `LEGACY_*` 與 `MaterialData`；以明確對照表（708 組）取代 `fromLegacy`，並與 `LegacyBridge` 結果逐項比對；DB 內序列化物品（`InventorySerializer`）與 `templates/undo`、`inprogress` 的雙讀。

### 7.4 風險
- **世界升級**只在複製的世界上試；升級後無法回到 1.12.2。
- 既有物品的 `display` 為純字串：Bukkit 的 `ItemMeta` 讀得到，但直接寫 NMS 的路徑必須同時接受兩種格式。
- 1.13 起藥水、附魔、實體 ID 皆改為命名空間字串，`materials.yml` 中以數字 ID 描述的附魔（`getEnchantById`）需檢查。
