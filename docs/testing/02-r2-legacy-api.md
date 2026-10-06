# CivCraft R2（方塊／物品存取層）測試紀錄

> 更新：2026-10-06 ｜ 範圍：舊方塊／物品 API 收進 `ItemManager` 與 `compat/LegacyMaterials`（R2.1–R2.6） ｜ 狀態：R2.1–R2.6 已驗證（R2.5 的 Market/Grocer 僅編譯確認）
> 對照：[基準線](00-baseline-1.12.2.md)。本檔只記錄改動後的結果，不覆蓋基準線。
> 慣例與索引：[README](README.md) ｜ 設計：[ROADMAP §4](../ROADMAP.md)

結果欄位：`PASS` / `FAIL` / `原本就壞` / `N/A` / `待測`。
版面約定：表格的「結果」欄只放簡短結論；日誌、數字、推論都移到各表下方的「實測紀錄」。

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
| 測試日期 | |
| 伺服器 | Spigot 1.12.2，Java 1.8.0_504 |
| CivCraft commit | （R2.1–R2.3 尚未 commit） |
| 資料庫 | 本機 MySQL，`civ_game` / `civ_global`（`online-mode=true`） |
| 已安裝插件 | 同 [Phase 1 紀錄](01-phase1-integration.md)；WorldBorder 是否在 plugins 內請填 |
| 未安裝 | TagAPI/iTag、ProtocolLib、TitleAPI、NoCheatPlus、VanishNoPacket |

---

## 1. 改動摘要

**新增 `tools/check-legacy-api.sh` 與基準 `tools/legacy-api-baseline.txt`**：統計邊界（`ItemManager`）外的舊 API 使用數，只能減少。

**`ItemManager` 新增四個方法**
- `getDispenserFacing(BlockState)`：取發射器朝向。
- `setSignFacing(BlockState, BlockFace)`：設定看板朝向（呼叫端仍需 `update()`）。
- `reapplyData(BlockState)`：把狀態自己的 data 寫回（呼叫端仍需 `update()`）。
- `copyData(ItemStack, ItemStack)`：複製材質 data。

**呼叫端搬移（行為不變）**
| 檔案 | 改動 |
|---|---|
| `UnitMaterial` | `getTypeId()` → `ItemManager.getId` |
| `Resident` | 手持物品與 `MaterialData` 的判斷改走 `ItemManager` |
| `Blacksmith` | `getTypeId()` → `ItemManager.getId`（4 處） |
| `PostBuildSyncTask` | 箱子的 `getData/setData` 往返改為 `ItemManager.reapplyData`（2 處） |
| `ArmorListener` | 發射器朝向 → `ItemManager.getDispenserFacing` |
| `MobSpawnerPopulator`、`TradeGoodPopulator` | 看板朝向 → `ItemManager.setSignFacing` |
| `ItemFrameStorage` | `setData` → `ItemManager.copyData` |

**R2.4（`compat/LegacyMaterials`）**
- 新增 `compat/LegacyMaterials`：舊命名 `Material` 常數只留在這裡（與 `ItemManager` 並列為邊界，檢查腳本已把 `/compat/` 納入）。
- 語意判斷：`isSign`、`wallSign`、`isRedstoneTorch`、`isGoldArmor`（任一金護甲）、`isDye`、`isBoneMeal`、`isTrampleable`、`isCarrotItem`、`isEnchantingTable`、`gunpowder`。
- 設定集合：`CivSettings` 的受限方塊、受限物品、開關方塊、放置例外改由 `LegacyMaterials.fill*` 填入（內容原樣搬移）。
- 呼叫端：`ArenaManager`、`Unit`、`BlockListener`、`DisableXPListener`、`PlayerListener`、兩個 Populator、`WarListener`。

**R2.5（耐久度與數字 ID 物品）**
- `get/setDurability` 依語意拆成兩類：
  - 損耗（tool wear）：`ItemManager.getDamage/setDamage`。呼叫端：`Barracks`（修復）、`QuarryAsyncTask`、`DurabilityOnDeath`、`NoDurability`、`ItemDuraSyncTask`、`ArmorListener`（護甲破損事件取消時）、`DebugCommand`（`getdura`/`setdura`）。
  - 變體（variant/data）：`LegacyMaterials.isNotchApple`、`isInvisibilityPotion`、`isUnbrewablePotionBase`（`BlockListener`、`PlayerListener`）；`ItemManager.copyWithAmount` 取代 `ItemFrameStorage` 的手動複製（並移除 `copyData`）。
- `new ItemStack(int, …)` 改走 `ItemManager.createItemStack`：`ConfigMarketItem`、`Resident.giveItem`、`Blacksmith`。
- 剩餘 3 處 `get/setDurability` 在 `gpl/InventorySerializer`（物品序列化），併入 R6。
- 讀碼備註：藥水在 1.9 之後改用 PotionMeta 存類型，舊的「以 damage 判斷藥水」在 1.12.2 上**可能本來就失效**（推測，未驗證）；本次只保持原判斷，不改行為。

**R2.6（藍圖資料盤點，沒有 Java 改動）**
- 新增 `tools/ScanTemplates.java`、`tools/scan-templates.sh`、基準 `tools/template-blocks-baseline.txt`：離線掃描 `civcraft_data/templates` 全部 `.def`，統計 `id:data` 組合並對照 1.12.2 的 `Material`。
- 實測：1274 檔、47,440,440 個方塊行、36,801 個指令行、708 組 `id:data`、158 個 id；座標超界、id 超出 0–255、data 超出 0–15、格式錯誤、找不到 `Material` 的 id 全為 0。
- `Template`/`TemplateStream` 的解析與貼上本來就走 `SimpleBlock`/`ItemManager`，檢查腳本顯示它們沒有舊 API 洩漏，所以這一步只加工具、不改程式。
- 用途：U1 的「舊 id:data → 新方塊」對照表必須涵蓋這 708 組；`tools/scan-templates.sh --check` 會在新藍圖帶入基準以外的組合時失敗。

**棘輪（`tools/legacy-api-baseline.txt`）**
| 類別 | 起始 | 目前 |
|---|---:|---:|
| raw-block-id | 26 | 0 |
| raw-block-data | 54 | 0 |
| material-data | 6 | 0 |
| durability-as-data | 27 | 3（`gpl/InventorySerializer`，併入 R6） |
| int-item-stack | 3 | 0 |
| legacy-material-const | 45 | 0 |

靜態驗證：javac 編譯通過（單元測試檔除外）。

---

## 2. 啟動煙霧測試

| # | 檢查項 | 結果 |
|---|---|---|
| 1.1 | 無 `NoClassDefFoundError` / `NoSuchMethodError` / `ClassCastException` | 待測 |
| 1.3 | 無 SQL 例外，資料表齊全 | 待測 |
| 1.5 | `isError()` 為 false | 待測 |
| — | 載入筆數：2 Civs、2 Towns、4 Residents、40 TownChunks、4 Structures | 待測 |
| — | Mapleland 登入無 `Duplicate entry` | 待測 |
| 1.8 | `stop` 後 `onDisable` 無例外 | 待測 |
| — | `bash tools/check-legacy-api.sh` 結束碼 0 | 待測 |

**實測紀錄**
- 

---

## 3. 功能測試

| # | 項目 | 結果 |
|---|---|---|
| R2-A | 貿易商品看板朝向（`/dbg createtradegood`） | PASS |
| R2-B | 建築完成後箱子朝向（建 Barracks） | PASS（hammer rate 還原待確認） |
| R2-C | 護甲發射器 | 待測 |
| R2-D | 鐵匠鋪冶煉 | 待測（僅編譯確認） |
| R2-E | `Resident.takeItemInHand` | 待測（僅編譯確認） |
| R2-F | 展示框放物品（`ItemFrameStorage`） | 待測（僅編譯確認） |
| R2-G | 單位物品互換（`UnitMaterial`） | 待測（僅編譯確認） |
| R2-H | 啟動時受限清單載入（`CivSettings` → `LegacyMaterials.fill*`） | PASS |
| R2-I | 附魔台右鍵被擋（`DisableXPListener`） | PASS |
| R2-J | 農地不被踩壞、骨粉規則、豬繁殖（`BlockListener`） | 農地仍可被踩壞（條件與 HEAD 等價，視為原本如此，見 §4）；骨粉、豬未測 |
| R2-K | 釀造台放火藥被取消（`PlayerListener`） | PASS |
| R2-L | 戰爭中放置紅石火把等限制（`WarListener`） | 待測（僅編譯確認） |
| R2-M | 損耗：`/dbg getdura`、`/dbg setdura <n>`（`DebugCommand`） | PASS |
| R2-N | 損耗：Barracks 修復看板（`Barracks.repairItem`） | PASS |
| R2-O | 變體：附魔金蘋果被擋（`BlockListener`，`/give Mapleland25244 golden_apple 1 1`） | PASS（訊息出現兩次，原本如此） |
| R2-P | 變體：藥水判斷（隱形藥水、基底藥水） | 待測（可能原本即失效） |
| R2-Q | 數字 ID 物品：Market 看板購買、Grocer 購買、冶煉產出（`ConfigMarketItem`、`Resident.giveItem`、`Blacksmith`） | 待測（需先蓋對應建築，目前僅編譯確認） |
| R2-R | 展示框放物品（`ItemFrameStorage.copyWithAmount`） | 待測（僅編譯確認） |
| R2-S | `tools/scan-templates.sh --check`（離線，約 1 分鐘） | PASS（708 組、無異常） |

**實測紀錄**
- R2-A：站在空曠平地執行 `/dbg createtradegood good_cotton`（id 見 `civcraft/data/goods.yml`；不帶 id 只會回 `Enter trade goodie id`，不會列清單）。預期貼牆看板、朝向正常、第 3 行為商品名；失敗：朝向錯誤或 `ClassCastException`。
- R2-B：`/town deposit 15000` → `/build Barracks` → `yes`；加速：`/ad town hammerrate Rome 5000`，**測完務必還原** `/ad town hammerrate Rome 261.375` 並用 `/town info` 對照。預期箱子可開、朝向正確，日誌無例外。
- R2-B 實測（客戶端日誌）：12:59:55 `The town has started construction on Barracks`；13:00:18 `Rome hammer rate has been set to 99999`；13:00:32 30% → 13:00:54 90% → 13:00:58 `[Global] The town of Rome has completed a Barracks!`，過程無錯誤訊息。位置預覽的 Layer 有效率 0%～18%，由 OP 繞過（`Since you're OP we'll let you build here anyway.`）。使用者目視確認箱子朝向正確（`ItemManager.reapplyData` 路徑 PASS）。伺服器日誌 13:00:18–13:02:31 無例外（建造 30%→90%→完成，僅有 `Cover Me With Diamonds` 進度訊息）。**待補**：hammer rate 還原（此日誌尾端尚未看到還原指令）。
- R2-A 實測：使用者回報「正常」（`ItemManager.setSignFacing` 路徑 PASS）。
- R2-H（伺服器日誌 14:11:51 啟動）：`LegacyMaterials.fill*` 載入無例外，`Done (3.580s)`；載入筆數 2 Civs、2 Towns、4 Residents、40 TownChunks、5 Structures（含新蓋的 Barracks）、12 Trade Goods、56 Protected Blocks；Mapleland 登入正常。
- R2-I：使用者回報附魔台右鍵被擋（成功）。
- R2-J：使用者回報農地仍會被踩壞。`BlockListener` 的條件在 HEAD 與現在等價（`Material.SOIL || Material.CROPS` → `LegacyMaterials.isTrampleable`），故非本次改動造成；該判斷只看「玩家腳下位置的下方一格」（`player.getLocation().getBlock().getRelative(DOWN)`），站在農地邊緣或跨在兩格之間時抓不到，**推測**是原本的限制，未用舊 jar 對照。
- R2-M：手持一把鐵鎬，`/dbg setdura 100` 後 `/dbg getdura`，應顯示 `Set Durability:100` 與 `Durability:100`、`MaxDura:250`。
- R2-N：`/ad item give Mapleland25244 mat_vanilla_iron_pickaxe 1`，`/dbg setdura 100`，右鍵 Barracks 的修復看板；預期扣款並顯示 `barracks_repair_Success` 的訊息，`/dbg getdura` 回到 0；再對滿耐久物品操作應顯示 `barracks_repair_atFull`。
- R2-O：使用者回報被擋，且訊息出現兩次。原因（讀碼）：`PlayerListener.onConsume`（LOW）本來就禁止**所有**金蘋果並送出 `itemUse_errorGoldenApple`，`BlockListener.OnPlayerConsumeEvent`（HIGHEST）對附魔金蘋果又送一次；被取消的事件仍會送到後面的監聽器。兩處在 HEAD 都存在，所以重複訊息是原本如此；兩則訊息同時出現也證明 `LegacyMaterials.isNotchApple` 判斷成立（一般金蘋果只會出現一則）。我先前寫的「一般金蘋果仍可吃」是錯的，一般金蘋果同樣被禁止。
- R2-M、R2-N（14:35–14:36 客戶端日誌）：`Set Durability:100`、`MaxDura:250`、`Durability:100`；給鐵鎬後再設 100，Barracks 修復看板顯示 `Looks like we can get you fixed up for 1000.0 Coins.`，確認後 `Iron Pickaxe was repaired for 1000.0 Coins!`，再操作顯示 `This item is already at full durability.`，與預期一致。
- R2-Q：`/market buy` 只是「購買城鎮與文明」的指令，不是物品買賣（我先前指引錯誤）。物品買賣在 Market 建築的看板（`Market.processBuy` → `ConfigMarketItem.buy`）與 Grocer（`Resident.buyItem` → `giveItem`）；需先蓋對應建築才能測，暫列為僅編譯確認。
- R2-K：使用者回報火藥可放入釀造台但釀造不會成功。`OnBrewEvent` 就是在配方含火藥等材料時取消 `BrewEvent`，與預期一致。
- R2-A（再確認）：14:16:37 `/dbg createtradegood good_cotton` 執行，使用者回報無錯誤，伺服器日誌無例外。
- R2-C：發射器放一件鐵頭盔，面向站在前方的玩家，紅石觸發；只能確認無例外。

---

## 4. 新增／更新的已知問題

| 日期 | 現象 | 原因 | 狀態 |
|---|---|---|---|
| 2026-10-06 | `check-legacy-api.sh` 依變數名略過 `sb`、`bs`、`nextBs`、`nextBlock`、`commandBlock`，Bukkit 物件剛好用這些名字會被漏掉 | grep 看不到型別 | 已知限制（見 ROADMAP §4.4） |
| 2026-10-06 | 農地仍可被踩壞（`BlockListener.OnPlayerInteractEvent` 的 PHYSICAL 分支） | 只檢查玩家腳下位置正下方的一格，站在農地邊緣或跨格時抓不到；與 HEAD 邏輯等價（推測原本如此，未對照舊 jar） | 待查，列入 R6 之後的行為修正 |
| 2026-10-06 | 食用金蘋果被擋時訊息出現兩次 | `PlayerListener.onConsume` 與 `BlockListener.OnPlayerConsumeEvent` 都送 `itemUse_errorGoldenApple`，且前者已禁止所有金蘋果，後者的附魔金蘋果判斷實際是多餘的 | 原本如此（HEAD 即有），外觀問題，現代化時合併 |
| 2026-10-06 | 日誌出現 `Ignoring grant of non existent recipe minecraft:tnt`、`minecraft:ender_eye` | 推測與 CivCraft 移除或改寫原版配方有關（未驗證） | 觀察項，與 R2 無關 |

---

## 5. 尚未完成

1. 確認 hammer rate 已還原為 261.375（`/town info` 的 Hammers）。
2. R2-C（護甲發射器）、R2-Q（Market／Grocer 購買、冶煉產出）、R2-R（展示框）、R2-L（戰爭中紅石火把限制）：僅編譯確認，待有場景再補。
3. R2.7：把 `tools/check-legacy-api.sh`（舊 API 棘輪）與 `tools/scan-templates.sh --check`（藍圖盤點）納入提交前檢查；剩餘 `gpl/InventorySerializer` 的 3 處 `get/setDurability` 併入 R6。
4. 下一階段 R3（排程與生命週期）；U1 的「舊 id:data → 新方塊」對照表以 `tools/template-blocks-baseline.txt` 的 708 組為涵蓋清單。
