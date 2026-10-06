# CivCraft R2（方塊／物品存取層）測試紀錄

> 更新：2026-10-06 ｜ 範圍：舊方塊／物品 API 收進 `ItemManager`（R2.1–R2.3） ｜ 狀態：進行中（待伺服器驗證）
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

**棘輪（`tools/legacy-api-baseline.txt`）**
| 類別 | 起始 | 目前 |
|---|---:|---:|
| raw-block-id | 26 | 0 |
| raw-block-data | 54 | 0 |
| material-data | 6 | 0 |
| durability-as-data | 27 | 26（R2.5） |
| int-item-stack | 3 | 3（R2.5） |
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

**實測紀錄**
- R2-A：站在空曠平地執行 `/dbg createtradegood good_cotton`（id 見 `civcraft/data/goods.yml`；不帶 id 只會回 `Enter trade goodie id`，不會列清單）。預期貼牆看板、朝向正常、第 3 行為商品名；失敗：朝向錯誤或 `ClassCastException`。
- R2-B：`/town deposit 15000` → `/build Barracks` → `yes`；加速：`/ad town hammerrate Rome 5000`，**測完務必還原** `/ad town hammerrate Rome 261.375` 並用 `/town info` 對照。預期箱子可開、朝向正確，日誌無例外。
- R2-B 實測（客戶端日誌）：12:59:55 `The town has started construction on Barracks`；13:00:18 `Rome hammer rate has been set to 99999`；13:00:32 30% → 13:00:54 90% → 13:00:58 `[Global] The town of Rome has completed a Barracks!`，過程無錯誤訊息。位置預覽的 Layer 有效率 0%～18%，由 OP 繞過（`Since you're OP we'll let you build here anyway.`）。使用者目視確認箱子朝向正確（`ItemManager.reapplyData` 路徑 PASS）。伺服器日誌 13:00:18–13:02:31 無例外（建造 30%→90%→完成，僅有 `Cover Me With Diamonds` 進度訊息）。**待補**：hammer rate 還原（此日誌尾端尚未看到還原指令）。
- R2-A 實測：使用者回報「正常」（`ItemManager.setSignFacing` 路徑 PASS）。
- R2-H（伺服器日誌 14:11:51 啟動）：`LegacyMaterials.fill*` 載入無例外，`Done (3.580s)`；載入筆數 2 Civs、2 Towns、4 Residents、40 TownChunks、5 Structures（含新蓋的 Barracks）、12 Trade Goods、56 Protected Blocks；Mapleland 登入正常。
- R2-I：使用者回報附魔台右鍵被擋（成功）。
- R2-J：使用者回報農地仍會被踩壞。`BlockListener` 的條件在 HEAD 與現在等價（`Material.SOIL || Material.CROPS` → `LegacyMaterials.isTrampleable`），故非本次改動造成；該判斷只看「玩家腳下位置的下方一格」（`player.getLocation().getBlock().getRelative(DOWN)`），站在農地邊緣或跨在兩格之間時抓不到，**推測**是原本的限制，未用舊 jar 對照。
- R2-K：使用者回報火藥可放入釀造台但釀造不會成功。`OnBrewEvent` 就是在配方含火藥等材料時取消 `BrewEvent`，與預期一致。
- R2-A（再確認）：14:16:37 `/dbg createtradegood good_cotton` 執行，使用者回報無錯誤，伺服器日誌無例外。
- R2-C：發射器放一件鐵頭盔，面向站在前方的玩家，紅石觸發；只能確認無例外。

---

## 4. 新增／更新的已知問題

| 日期 | 現象 | 原因 | 狀態 |
|---|---|---|---|
| 2026-10-06 | `check-legacy-api.sh` 依變數名略過 `sb`、`bs`、`nextBs`、`nextBlock`、`commandBlock`，Bukkit 物件剛好用這些名字會被漏掉 | grep 看不到型別 | 已知限制（見 ROADMAP §4.4） |
| 2026-10-06 | 農地仍可被踩壞（`BlockListener.OnPlayerInteractEvent` 的 PHYSICAL 分支） | 只檢查玩家腳下位置正下方的一格，站在農地邊緣或跨格時抓不到；與 HEAD 邏輯等價（推測原本如此，未對照舊 jar） | 待查，列入 R6 之後的行為修正 |
| 2026-10-06 | 日誌出現 `Ignoring grant of non existent recipe minecraft:tnt`、`minecraft:ender_eye` | 推測與 CivCraft 移除或改寫原版配方有關（未驗證） | 觀察項，與 R2 無關 |

---

## 5. 尚未完成

1. 確認 hammer rate 已還原為 261.375；R2-C 發射器可略過（僅編譯與日誌覆蓋）。
2. R2.4 驗證完成，待 commit：`refactor(compat): move pre-1.13 Material names into LegacyMaterials`。
2. 驗證通過後 commit：`refactor(compat): route legacy block/item APIs through ItemManager`。
3. R2.4：舊命名 `Material` 常數（主要在 `CivSettings` 的受限清單）。
4. R2.5：`get/setDurability` 與 `new ItemStack(int, …)`。
5. R2.6、R2.7：`Template`／`TemplateStream` 解析、棘輪歸零。
