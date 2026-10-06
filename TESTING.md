# CivCraft 基準線測試清單（TESTING.md）

用途：在 **Spigot/Paper 1.12.2** 上記錄目前行為，作為階段 A 每次升級（1.13 → 1.16 → 1.20/1.21）後的回歸對照。

結果欄位填：`PASS` / `FAIL` / `原本就壞`（1.12.2 基準線已壞，非遷移造成）/ `N/A`。
每一階段複製一份本表另存（例如 `TESTING-1.13.md`），不要覆蓋基準線。

## 0. 環境（每次都要記錄）

| 項目 | 值 |
|---|---|
| 伺服器 jar / build 版本 | |
| Java 版本 | |
| CivCraft commit | |
| MySQL 版本 / 資料庫（**全新本機庫，勿用 INSTALL.txt 的遠端帳密**） | |
| 已安裝插件與版本 | CustomMobs、Vault、WorldBorder、dynmap … |
| `use_server_listing_service` | 必須為 `false` |
| `online-mode` | **主測試庫必須為 `true`**（見下方雙環境說明） |
| 測試日期 / 測試者 | |

### 雙環境（2026-10-05 決定）

CivCraft 以 UUID 字串關聯 leader、權限群組、營地 owner、城鎮 outlaws、任務紀錄；UUID 查不到時會**靜默丟棄**。因此資料的 UUID 必須與登入時一致。正版服一定是 online-mode，測試也以此為準。

| 環境 | online-mode | 資料 | 用途 |
|---|---|---|---|
| 主測試庫 | `true` | 沿用現有 `civ_game`（Mapleland25244 的 UUID 為 `f41d1dcf-…`） | 基準線：重啟還原、leader、建築、經濟等需要真實資料的項目 |
| 多人測試庫 | `false` | 全新資料庫，重新建立文明與城鎮 | 只測雙人互動（`/trade`、外交、戰爭）；**不當基準線資料來源**。若有第二個正版帳號，可直接用主測試庫，不需此環境 |

注意：
- **不要**單獨修改 `residents.uuid`，否則 leader 與群組成員會消失。
- 日後若使用代理（BungeeCord / Velocity）：後端 `online-mode=false` 並開啟 IP forwarding，由代理驗證，UUID 才會與正版一致。

## 1. 啟動煙霧測試

2026-10-05 首輪（Spigot 1.12.2、Java 1.8.0_504、本機 MySQL）：

| # | 檢查項 | 結果 | 備註 |
|---|---|---|---|
| 1.1 | 日誌無 `NoClassDefFoundError` / `ClassNotFoundException` | PASS | 缺 TagAPI / NCP / VanishNoPacket / TitleAPI / Vault 仍正常啟動 |
| 1.2 | 日誌無 `InvalidConfiguration` | PASS | |
| 1.3 | 日誌無 SQL 例外，各實體資料表已建立 | PASS（啟動階段） | 登入後出現 Duplicate entry，見第 4 節 |
| 1.4 | 26 個 `data/*.yml` 與 `localization/default_lang.yml` 載入（首次啟動會匯出到資料夾） | PASS | 全量載入 Civ / Town / Resident / TownChunk 等成功 |
| 1.5 | `CivCraft.isError()` 為 false（插件不會自動停用，必須讀日誌確認，不能只看有沒有崩潰） | PASS | 啟動約 2.8 秒 |
| 1.6 | `plugin.yml` 的 24 個指令中，`ac` / `gc` / `sb` 實際有無反應 | FAIL（原本就壞） | 玩家端日誌確認三者執行後完全沒有輸出，與「未註冊 executor」一致 |
| 1.7 | 無可選插件時啟動（移除 HeroChat、TagAPI、NCP…）仍可啟動 | PASS（部分） | 已涵蓋 TagAPI、NCP、VanishNoPacket、TitleAPI、Vault 缺席；HeroChat 因缺 Vault 沒載入 |
| 1.8 | `onDisable` 正常（`SQLUpdate.save()` 沒有丟例外） | PASS | 2026-10-05 17:54 第二輪啟動後立即關服，`Disabling CivCraft` 無例外，世界正常存檔。**該次沒有玩家登入，佇列幾乎為空，不代表大量待寫入時也正常** |

## 2. 功能測試

先用 `/ad`、`/dbg` 建立場景。

### 2.1 文明、城鎮、玩家
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.1.1 | `/civ` 建立文明 | 文明建立，扣款 | | |
| 2.1.2 | `/town` 建立城鎮 | 城鎮與 TownHall 建立 | | |
| 2.1.3 | 邀請、加入、離開城鎮 | `/accept` `/deny` 流程正常 | | |
| 2.1.4 | `/res`、`/res friend`、`/res toggle` | 設定儲存 | | |
| 2.1.5 | `/town info` `/civ info` | 顯示正確 | PASS | 多次實測，Score、Leaders、Beakers、Treasury、Culture、Happiness、Wars 欄位皆正常顯示 |
| 2.1.6 | 稅與債務（等到 DailyEvent） | 計算正確 | | |
| 2.1.7 | `/town group`、`/civ group` | 權限群組生效 | | |
| 2.1.8 | 以 `mat_found_civ` 建立第二個文明 | 文明與首都建立，首都開始建造 | PASS | 2026-10-06 00:32 `/ad item give Toxicnnan mat_found_civ 1` 成功（Toxicnnan 同時獲得進度 `Into Fire`，因旗幟的物品 ID 是 369 = 烈焰棒，無害）；00:36:59 日誌：`[Town:Berlin] The town has started construction on Capitol`、`GermanReich Founded! - Capitol: Berlin - Leader: Toxicnnan` |
| 2.1.9 | `/camp disband`（Toxicnnan 輸入 `/camp disbAND`，子指令不分大小寫） | 營地刪除並持久化 | PASS | 重啟前 `Loaded 1 Camps`，之後 `Loaded 0 Camps` |

### 2.2 領地與保護
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.2.1 | 買地、`/plot perm` | 權限生效 | | |
| 2.2.2 | 非成員在領地放置與破壞方塊 | 被阻擋 | PASS（破壞） | 2026-10-05：Toxicnnan（非 Rome 成員）破壞被擋，訊息 `You do not have permission to destroy blocks in the town of Rome`。`/plot info`：Owner none、Group residents，Build/Destroy/Interact/Item Use 皆為 Owner yes、Group yes、Others no。放置方塊尚未單獨記錄。注意：該訊息用 `sendErrorNoRepeat`，同一玩家連續兩次相同訊息只顯示第一次，「無提示」不一定代表沒擋 |
| 2.2.2b | Mapleland 破壞 Capitol 結構方塊 | 被擋並顯示提示 | PASS | 出現 `This block belongs to a Capitol and cannot be destroyed right now.`（`StructureBlockHitEvent`）；使用者確認各種方式都破壞不了。沒有提示的情況推測是 `sendErrorNoRepeat` 吞掉重複訊息 |
| 2.2.2c | `/ad perm` 管理員覆寫（Toxicnnan 已 op） | 非成員可破壞領地內一般方塊 | PASS | 2026-10-05 使用者確認 Toxicnnan 開啟 `/ad perm` 後，可破壞領地內非建築的方塊。`/ad perm` 只繞過 plot 權限（`PlotPermissions.java:164`）；繞過結構方塊保護的是 `/ad sbperm`（`BlockListener.java:766`），未測以免破壞建築 |
| 2.2.3 | 爆炸、點燃、活塞、紅石 | 依保護規則處理 | | |
| 2.2.4 | `/here` | 顯示所在領地 | PASS（部分） | 玩家端日誌：`You stand in wilderness.`；在領地內的輸出尚未記錄 |
| 2.2.5 | 超出 WorldBorder 的建築 | 被拒絕 | | |

### 2.3 建築
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.3.1 | `/build` 各主要建築 | 藍圖貼上、進度正常 | 進行中（開始建造 PASS） | 2026-10-05 22:35：`/ad civ givetech` 解鎖 Religion 後 `/build Bank`；流程為「基岩輪廓預覽（只有自己看得到）→ 輸入 `yes` 確認 → `Since you're OP we'll let you build here anyway.`（OP 繞過位置檢查）→ `Structure position is valid.` → `The town has started construction on Bank`」。非 OP 的位置檢查規則尚未測。進度：23:00 `/build progress` 顯示 Bank 13.0%（159.014/1200 hammers，441/3328 方塊），約 25 分鐘累積 159 hammers（約 6.3/分鐘），完成約需再 2 小時 45 分；22:54 收到 `The Bank is now 10% complete.`；建造期間日誌無例外。`/build undo` 只支援 Wall/Road（`Town.processUndo`，`town_undo_notRoadOrWall`），Bank 不適用，屬設計；`/build demolish` 不一定退款（`Town.demolish` 看不到退款邏輯，待實測）。建築名稱不分大小寫（`/build bank` 可用）。完成尚未測。23:04 重啟後載入 2 個 Structure（Capitol 與建造中的 Bank）、TownChunks 17→21（使用者 22:29 的 `/town claim`），建築記錄有保存；重啟後 `/build progress` 顯示 17%（重啟前 23:00 為 13%），建造有接續、沒有重置，**建築進度持久化 PASS**。`/town info` 顯示 600、750（600 為 Structure Upkeep = 500 + Bank 100；750 為當時城鎮金庫，23:12 使用者 `/town deposit 6000` 後為 6750）。23:12 收到 `The Bank is now 20% complete.`。後續進度時間：30% 23:30、40% 23:44、50% 23:57、60% 00:11、70% 00:25、80% 00:40，約每 14 分鐘 10%（約 8.7 hammers/分鐘），跨越 23:04 重啟與 00:33 崩潰後都有接續。2026-10-06 戰爭期間收到 `[Global] The town of Rome has completed a Bank!`（建造完成，PASS）。位置檢查規則（取自 War Camp 與加農砲的位置預覽）：每一層需至少 80% 有效，例如 `Layer: 71 is 48.232% (491.0/1018.0) valid. It needs to be at least 80%.`，OP 以 `Since you're OP we'll let you build here anyway.` 繞過；非 OP 玩家的實測仍待做。`/build` 說明：`/build [structure name]` 在目前位置建造；另有 `list`、`progress`、`undo`、`preview`、`demolish`、`demolishnearest`、`repairnearest`、`refreshnearest`、`validatenearest`。建築費用由城鎮金庫支付（`structures.yml` 的 `cost`），Rome 金庫目前為 0，需先 `/town deposit` |
| 2.3.2 | 建造中 undo | 退款、方塊還原 | | |
| 2.3.3 | 建築損壞、修復 | 血量機制正常 | PASS | 2026-10-06 10:22：`/ad build destroynearest Rome`（不帶 yes：`Would destroy the Bank at location world,-304,70,288. Are you sure?`）→ `yes` → `[Global] Bank has been destroyed in the town of Rome!`；`/build repairnearest`：`Are you sure you want to repair the Bank at location … for 1125.0 Coins?`（= 建造費 2250 / 2）→ `yes` → `[Town] The town of Rome has repaired the Bank located at world,-304,70,288`；Rome 金庫 5800 → 4675（扣 1125，吻合）。毀損時結構方塊以掉落物形式散落（看板、火把、紅玫瑰等被撿起）。提示文字誤植：預覽訊息寫 `Type '/ad destroynearest [town] yes'`，實際指令為 `/ad build destroynearest [town] yes`。反例：10:25 對未損壞的 Bank 再執行 `/build repairnearest` → `Bank at location world,-304,70,288 is not destroyed.`（PASS）。`/build refreshnearest`（10:36）：提示 `Are you sure you want to refresh the blocks for your Bank? Any blocks inside the structure (or where the structure ought to be) will be replaced with whats inside the template. You may lose some blocks.`→ `yes` → `Bank refreshed.`；之後破壞結構方塊回 `This block belongs to a Bank and cannot be destroyed right now.`（結構方塊保護恢復，PASS）。方塊是否視覺復原、`validatenearest` 輸出待使用者回報；戰爭期間修復尚未測 |
| 2.3.4 | 農場、礦場、圖書館、銀行 | 產出正常 | | |
| 2.3.5 | 奇觀（Wonders） | 建造與效果正常 | | |
| 2.3.6 | 看板、箱子等結構方塊互動 | 正常 | | |

### 2.4 經濟
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.4.1 | `/pay`、`/econ` | 金額正確 | PASS | `/pay Toxicnnan 23`、轉帳 5800 皆正確（SQL 合計核對吻合）；`/econ` 加款（`Added 50000 Coins to Toxicnnan.`）成功；`/town deposit`、`/civ deposit` 成功 |
| 2.4.2 | `global.use_vault=true` | 走 Vault | | |
| 2.4.3 | `global.use_vault=false` | 走內建經濟 | | |
| 2.4.4 | `/market buy` | 交易正確 | | |

### 2.5 科技、政體、外交、戰爭
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.5.1 | `/civ research` | 研究進度推進 | PASS（開始與進度） | 2026-10-05 22:00：`list` 顯示 Armory / Religion / Mining / Agriculture（各 5000 金幣、500 beakers）；`on Armory` 成功；再次 `on` 回 `use /civ research switch instead`；`progress` 顯示 64%（323.68/500）。機制（讀碼）：`BeakerTimer` 每 60 秒執行，每次加 `civ.getBeakers()/60`（目前約 2.01）。323.68 約等於 161 次，代表研究約 2 小時 40 分前就開始，與「剛剛才開始」不符。使用者確認：研究在這輪測試前很久就開始，與推算吻合，進度在期間有跨過重啟保存（`researchProgress` 欄位）。重啟後研究進度接續：22:47 為 80%，23:04 重啟後約 23:07 為 88%（PASS）。研究完成：23:38:31 `Our civilization has discovered Armory!`（PASS）；之後 00:00 每小時事件 `Converted 50 beakers into 5 culture as we are not researching any new technologies.`（未研究時 beakers 轉文化，PASS）。科技解鎖：2026-10-06 `/build list` 顯示 Barracks（15000、upkeep 1000、1500 hammers，`tech_armory`）、Monument（15000、upkeep 1500、2000 hammers，`tech_religion`）、Capitol（剩 1）。與 `structures.yml` 的 `require_tech` 吻合（PASS）。Bank 因 limit 1 已用完（建造中）而未列出，此點為推論 |
| 2.5.2 | `/civ gov` 切換政體 | 效果生效 | | |
| 2.5.3 | `/civ diplomacy`（含 gift） | 關係改變 | PASS | 實測（2026-10-06 08:59–09:03）：`show` 預設為 NEUTRAL（未列出即中立）→ `request GermanReich peace` + 對方 `respond yes` → `AT PEACE`（show 顯示 PEACE）→ `declare hostile` → `HOSTILE` → `declare war` → `AT WAR`（`wars` 與 `global` 都列出，show 顯示 `engaged in 1 wars`）；戰爭期間 `request` / `respond` 被拒：`You cannot use this diplomacy command while it is WarTime.`。拒絕路徑（`respond no`）、`ally`、`gift` 尚未測。說明（2026-10-06）：`gift`、`request [civ] [neutral｜peace｜ally]`、`respond [yes｜no]`、`declare [civ] [hostile｜war]`、`show [civ]`、`global`、`wars`、`capitulate`、`liberate`。讀碼規則：除 `show/global/wars` 外，戰爭期間一律拒絕（`cmd_civ_dip_errorDuringWar`）；`request`/`declare` 需文明 leader 或 adviser；`request ... war` 只在 `casual_mode: true` 才可，目前為 `false`，所以只能用 `declare ... war`；`declare war` 條件：非戰爭期間、非 casual、不在 `war.time_declare_days`（3 天）窗口內（`war.yml`：`time_day: 7`、`time_hour: 10`）、宣戰方金庫不可負債；`request ally` 在宣戰窗口內且任一方處於戰爭時被拒 |
| 2.5.4 | WarEvent 開戰與結束 | 流程完整 | 部分 PASS | 2026-10-06 09:01:45 開戰：廣播 `WarTime Has Started`、`War time will last for 2.0 hours`、`Teleportation is disabled until after War Time`、`All 'at war' players not using CivCraft's Anti-Cheat have been expelled during WarTime`。未開 op 的 Toxicnnan 被踢出，約每 38 秒被踢一次（`ACManager` 週期檢查）。戰爭中實測（Toxicnnan 端日誌，2026-10-06）：①控制方塊：逐次 `Damaged Control Block (64 / 100)` 遞減到 1，之後 `[Civ] We've destroyed a control block in the town of Rome!`，共摧毀 4 個；再打 `Control Block already destroyed.`；直接打首都結構方塊顯示 `Cannot damage Capitol, go after the control points!`（PASS）。②War Camp：`mat_found_warcamp` 右鍵 → 位置檢查 → `Ready for War! War Camp.` → `yes` → `You have set up a war camp!`（PASS）。③加農砲：右鍵 `We've deployed a cannon at location …`；裝填 3 次 `Added TNT to cannon.` → `Bombs Away!`；冷卻中 `Wait for the cooldown.`；命中 `[Civ] Your cannon hit Rome's Capitol. (1900/2000)`，之後 1800、1700、1600，每發 100 傷害，與 `structure_damage: 100` 一致；兩座砲可同時發射（PASS）。④限制訊息：`Cannot build here, another cannon in the way.`、`Cannot build here, you must be closer to the surface.`、`Can't destroy your own civ's cannons during war.`、`Must use TNT to break blocks in at-war civilization cultures during WarTime.`、`Can only place grass, dirt, and TNT blocks in at-war civilization cultures during WarTime.`（另可放鐵、金、鑽石、綠寶石方塊）（皆與程式相符）。⑤戰爭中 `[Global] The town of Rome has completed a Bank!`（建造在戰爭期間完成）。⑥占領（第二份 Toxicnnan 端日誌）：摧毀 Rome 全部控制方塊後出現 `-------[ HolyRomanEmpire Defeated! ]--------` / `conquered by GermanReich!`（PASS）；`/civ info GermanReich`：Score 30700、Towns: Berlin、Leaders: Toxicnnan、Treasury 0.0；戰爭未結束前 `request`/`respond` 仍被拒；`/civ dip show` 仍為 `WAR with HolyRomanEmpire`，`engaged in 1 wars`。結構方塊提示：`Cannot damage this structure block. Choose another.`、`Cannot break protected item frames. Right click to interact instead.`、`You cannot place a non-trade goodie items in a trade goodie item frame.`、`You do not have permission to interact with this painting/itemframe.`（皆為預期保護）。還原用指令（讀碼）：`/civ dip liberate [城鎮]`（占領方 leader，非戰爭期間）、`/civ dip capitulate [城鎮]`（被占領方 leader，需 `yes` 確認，非戰爭期間）、`/ad civ liberate [文明]`、`/ad civ unconquer [文明]`、`/ad civ conquered [文明]`。⑦戰爭結束與解除占領（客戶端日誌時間：第 1 場 09:01:45 開始、09:29:55 結束；第 2 場 09:30:38 開始、09:47:58 結束，可重複開戰）：結束時依序廣播 `Teleportation is now enabled.` → `You've been teleported back to your town hall. WarTime ended and you were in enemy territory.` → `WarTime Has Ended` → `Most Lethal: (0 kills)` → `[Global] GermanReich Conquered: HolyRomanEmpire,` → `WarTime has been disabled.`（PASS）；結束後 `/civ dip show` 仍為 `WAR with HolyRomanEmpire`（關係不會自動恢復，Berlin 仍顯示 `[WAR-PvP]`）；Toxicnnan 執行 liberate 後 `[Global] The Civilization of HolyRomanEmpire has been liberated by the good graces of its owner, GermanReich.`（PASS，HolyRomanEmpire 恢復）；`deop Toxicnnan` 成功（第一次誤打 `/deop` 無參數收到用法提示）。⑧被占領方視角（Mapleland 端日誌）：占領在戰爭結束時才生效（`[Civ] Welcome our new overlords, the civilization of GermanReich!` 與結束廣播同為 09:47:58；戰爭中只看到 `Defeated!`）；`/civ dip capitulate Rome` 不帶 `yes` 顯示確認提示（`Capitualting means that this civ will be DELETED and all of its towns will become normal towns in GermanReich and can no longer revolt. Are you sure?`，PASS）；Toxicnnan liberate 後 Mapleland 再 capitulate 回 `Cannot capitulate unless captured by another civilization.`（PASS）；`/civ info` 顯示 HolyRomanEmpire 完整恢復（Towns: Rome、Leaders: Mapleland25244、Treasury 22500，與占領前相同）。**發現不對稱**：liberate 後 `/civ dip global` 顯示 `HolyRomanEmpire: NEUTRAL with GermanReich` 但 `GermanReich: WAR with HolyRomanEmpire`（見第 4 節）。尚未測：控制方塊重生、`capitulate yes`、以 `request peace` 解除 WAR、每日事件的宣戰方維護費、PvP 擊殺 |
| 2.5.5 | `/team`、營地、攻城 | 正常 | | |

### 2.6 事件與排程
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.6.1 | `/ad timer` 觸發 DailyEvent | 稅、產出結算 | PASS | 2026-10-05 18:08 `/ad timer run daily`：居民稅 0、城鎮稅 0、Rome 付維護費 500、文明維護費 0，流程完整結束並設定下次時間。日誌顯示 HolyRomanEmpire 處於負債（6 天後出售），需確認是否為既有狀態 |
| 2.6.2 | HourlyTickEvent | 正常 | PASS | 2026-10-05 18:00 自動觸發，Rome 產出 75 文化，Camp Roger 營火訊息正常 |
| 2.6.3 | 約 35 個週期任務運行 24 小時以上 | 無例外、無明顯 lag | | |

### 2.7 外部整合（每項分別在「有 / 無」該插件下測）
| # | 插件 | 功能 | 有 | 無 | 備註 |
|---|---|---|---|---|---|
| 2.7.1 | Vault | 經濟 | | | |
| 2.7.2 | WorldBorder | 建築邊界檢查 | | | `Buildable` 缺插件時可能 NoClassDefFoundError |
| 2.7.3 | TagAPI / iTag + ProtocolLib | 名牌顏色 | | | `Town` 直接引用 iTag |
| 2.7.4 | HeroChat | 頻道聊天 | | | |
| 2.7.5 | TitleAPI | 標題訊息 | | | |
| 2.7.6 | NoCheatPlus | 飛行豁免 | | | |
| 2.7.7 | VanishNoPacket | 隱身玩家處理 | | | |
| 2.7.8 | CustomMobs | 怪物生成點 | | | hard depend |
| 2.7.9 | dynmap + `civcraft_dynmap` | 邊界繪製 | PASS（啟用） | PASS（啟用） | 2026-10-05 23:04 換成 dynmap 3.0-beta-4-213 後，dynmap 與 `dynmap-civcraft v1.0` 都正常啟用（`enabled...`），WorldBorder 也接上 dynmap。仍有 `Error reading plugins\dynmap\markers.yml`；邊界是否真的畫在地圖上待用網頁（8123 埠）確認。先前的 dynmap 3.6-899 啟用失敗： `BukkitVersionHelperSpigot120 has been compiled by a more recent version of the Java Runtime (class file version 61.0)`，需要 Java 17，伺服器是 Java 8；連帶 `dynmap-civcraft v1.0` 在 `DynmapCivcraftPlugin.onEnable:39` NPE，`/dynmap` 指令也 NPE。需改用與 Java 8 / 1.12.2 相容的舊版 dynmap |

### 2.8 物品與其他
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.8.1 | 自訂物品、合成配方 | 配方註冊，lore 正確 | | |
| 2.8.2 | 自訂物品耐久與附魔 | 正常 | | |
| 2.8.3 | 交易船、貿易商品 | 正常 | | |
| 2.8.4 | 競技場 | 正常 | | |
| 2.8.5 | 釣魚、隨機事件 | 正常 | | |
| 2.8.6 | 騎乘、馬匹（`HorseModifier`） | 正常 | | |
| 2.8.7 | 護甲事件（`ArmorListener`） | 正常 | | |
| 2.8.8 | `Template` 藍圖載入（`civcraft_data/templates`） | 全部可載入 | | |

### 2.9 持久化
| # | 步驟 | 預期 | 結果 | 備註 |
|---|---|---|---|---|
| 2.9.1 | 建立完整場景後正常 `stop`、重啟 | Civ、Town、Resident、建築、領地、關係全部還原 | PASS（帳目核對） | 使用者確認轉給 Toxicnnan 為 5800，與推算吻合（Toxicnnan 的餘額未直接查看，由合計反推為 56073）。2026-10-05 18:20 重啟：載入筆數與重啟前一致（1 Camp、1 Civ、1 Town、4 Resident、5 Group、17 TownChunk、1 Structure、12 Protected Blocks），Mapleland 以 `f41d1dcf-…` 登入且沒有 `No resident found` / `Duplicate entry`。18:26 SQL 快照：residents 4（coins 合計 121001）、towns 1、civilizations 1、townchunks 17、structures 1、groups 5、camps 1，與日誌載入筆數相同。重啟後 `/res info` 顯示 Mapleland 64426、civ 金庫 23000、Rome 金庫 0。**重啟前沒有留下對照數字，所以金額是否「原樣還原」尚未證明**；下次請在 `stop` 前先記下這三個數字再比對。帳目核對：Mapleland 少了 29323 = civ 存款 23000 + 500 + `/pay 23` + 轉給 Toxicnnan 約 5800；若轉帳確為 5800，Toxicnnan 應為 56073，與 SQL 合計 121001 扣除其他三人（64426、250、252）完全吻合；該次重啟之前的關服日誌已被覆蓋，沒有看到 `onDisable` 在有真實操作後的紀錄 |
| 2.9.2 | 強制終止（kill）後重啟 | 記錄丟失範圍（`SQLUpdate` 佇列） | PASS（未觀察到丟失） | 2026-10-06 10:26–10:28：`/town deposit 100`（10:26:52）、`/econ add Mapleland25244 1`（10:27:05）、`/town deposit 100`（10:27:58）、`/econ add Mapleland25244 1`（10:28:07），約 1–8 秒後由使用者結束伺服器（新伺服器 10:28:18 啟動）。重啟後 Rome 金庫 4675 → 4875（+200，兩次存款都在）、Mapleland 金幣 53228（= 53426 − 200 + 2，兩次 +1 都在）。**未觀察到任何丟失**，即使最後一筆變動距離終止只有 1–8 秒。限制：不確定終止方式是否為硬殺（未見前一場日誌的結尾）；未測「變動後 0 秒內」的更短窗口；`SQLUpdate` 為非同步佇列輪詢，風險窗口理論上存在但極短。**重要補充（世界方塊不一致）**：重啟後使用者發現 10:23 修復好的 Bank 方塊又是損壞狀態。檢查：資料庫 `structures` 表 `s_bank` 的 `hitpoints` 為 200（完好）、`/build repairnearest` 回 `Bank … is not destroyed.`，所以 CivCraft 資料正確，是**世界方塊**在強制結束時沒存檔（Minecraft 區塊定期自動存檔，硬殺丟失最近的方塊變動），造成資料庫與世界不一致。同一張表另顯示 Rome 的 `s_capitol` hitpoints 為 2000（戰爭中被加農砲打到 1600，戰爭結束後已回復，`Buildable` 有血量回復邏輯，約第 1452 行）。因此 C4 的「無丟失」只適用於 CivCraft 資料庫那一半 |
| 2.9.3 | SessionDB 暫存狀態 | 還原 | | |

### 2.10 首輪實測結果（2026-10-05，玩家端日誌）

| 項目 | 結果 | 備註 |
|---|---|---|
| `/pay` | PASS | 輸出 `Paid 2.0 Coins to OPchannel` |
| `/ad res` 加款（`Added 50000 Coins`） | PASS | |
| `/market`、`/camp`、`/team`、`/team top10`、`/plot`、`/town`、`/res`、`/civ`、`/dbg`、`/ad` 說明頁 | PASS | 都有輸出；`/ad` 需先 `op` |
| `/civ info`（HolyRomanEmpire） | PASS | Score、Towns、Leaders、Beakers 顯示正常 |
| 進出領地提示（Wilderness / Camp Roger / Rome / civ 邊界） | PASS | 「Wilderness - [PvP]」與「Entering civ」之間缺換行，外觀問題 |
| `/report player` | PASS | |
| `/trade` | PASS | 距離過遠會被拒，靠近後 `Trade Invitation Sent` |
| `/kill`、死亡訊息 | PASS | |
| `/deny` 無文明時 | FAIL → 已修 → PASS | 修正前客戶端看到 `An internal error occurred…`；修正後收到 `No question to respond to.` |
| `/accept`、`/yes` | PASS | `No question to respond to.` 與 `Unknown command` 皆為預期 |
| `/sb`、`/gc`、`/ac` | FAIL（原本就壞） | 無任何輸出 |
| 自己城鎮內的建造權限 | FAIL | 受 Resident 重複問題影響，不能視為真正基準 |
| 建築、科技、戰爭、持久化重啟 | 已測 | 此表為首輪紀錄，後續結果見 2.3、2.5、2.9 |

## 2A. 測試用物品與指令速查

所有 id 皆查自 `civcraft/data/*.yml`；「未驗證」代表只從原始碼或設定檔推得，尚未在伺服器實測。

### 取得物品
| 用途 | 指令 | 備註 |
|---|---|---|
| 給任意自訂物品 | `/ad item give [玩家] [materials.yml 的 id] [數量]` | `AdminItemCommand.give_cmd`，id 必須存在於 `materials.yml`，背包滿了會掉在地上 |
| 圖形介面挑選 | `/ad items` | 依分類（Tier 1 到 4 材料、Gear、Eggs、Catalyst、Fish、Tools、Special）開啟選單 |
| 附魔（物品拿在手上） | `/ad item enhance [soulbound｜attack｜defence｜arena]` | 不帶參數會列出可用清單 |
| 教學書 | `/res book` | 對應 `mat_tutorial_book` |
| 單位 | `/ad spawnunit [單位 id] [城鎮]` | `units.yml`：`u_settler`、`u_spy` |
| 貿易商品 | `/dbg createtradegood [good_id]` | id 見 `goods.yml`（共 28 種） |
| 魔物生成點 | `/dbg createmobspawner [id]` | **不要用**：CustomMobs 在 1.12.2 不可用，會觸發 `CustomMobsAPI` 崩潰 |

### 金錢與科技
| 用途 | 指令 |
|---|---|
| 個人金幣 | `/econ add｜sub｜set｜give [玩家] [金額]` |
| 城鎮金庫 | `/econ addtown｜subtown｜settown [城鎮] [金額]`（或 `/town deposit [金額]`） |
| 文明金庫 | `/econ addciv｜subciv｜setciv [文明] [金額]`（或 `/civ deposit [金額]`） |
| 給科技 | `/ad civ givetech [文明] [科技 id]`，例如 `tech_religion`（解鎖 Bank） |
| 全科技 | `/ad civ alltech [文明]`（會改動大量資料，先備份） |
| 研究速度 | `/ad civ beakerrate [文明] [數值]` |

### 重要物品 id
| 物品 | id | 用途 / 取得方式 |
|---|---|---|
| 建立文明旗幟 | `mat_found_civ` | 右鍵使用；合成需 皇冠 `mat_royal_crown`、石材 `mat_compacted_stone`、砌磚砂漿 `mat_masonry_mortar`（形狀 `srs / sss / mmm`）。皇冠 = 3 `mat_jewelry_grade_gold` + 2 `mat_decorative_jewels` + 1 `mat_proof_of_leadership`；證明 = 4 `mat_badge_of_leadership`；徽章 = 9 `mat_token_of_leadership`（不可合成）。**測試請直接用 `/ad item give`** |
| 營地 | `mat_found_camp` | 右鍵使用；Camp Roger 已存在 |
| 戰爭營地 | `mat_found_warcamp` | 僅戰爭期間可用（`WarCamp.java`：`warcamp_notWarTime`） |
| 戰爭加農砲 | `mat_build_cannon` | 戰爭測試用（`CannonListener`），細節未驗證 |
| 教學書 | `mat_tutorial_book` | `/res book` |
| 工具 | `mat_vanilla_iron_pickaxe` 等（`mat_vanilla_{stone,iron,gold,diamond}_{pickaxe,shovel,axe,hoe}`） | 自訂工具 |
| 基礎材料 | `mat_milled_lumber`、`mat_masonry_mortar`、`mat_compacted_stone`、`mat_refined_*`（Tier 1）等 | 合成測試用，可用 `/ad items` 瀏覽 |

### 各測試項目需要什麼
| 測試項目 | 需要的物品 / 前置 | 指令 |
|---|---|---|
| 建築（Bank 等） | 對應科技、城鎮金庫足夠、hammers | `/ad civ givetech …` → `/town deposit …` → `/build list` → `/build [名稱]` → `yes` |
| 第二個文明（外交、戰爭前置） | `mat_found_civ` ×1 給第二位玩家 | `/ad item give Toxicnnan mat_found_civ 1` → 該玩家**手持右鍵** → 基岩輪廓預覽 → 輸入 `yes` → 依提示輸入文明名與首都名（取消：輸入其他文字） |
| 外交 | 兩個文明 | `/civ diplomacy show`、`request`、`respond`、`declare`、`wars`；快速設定關係 `/ad civ setrelation [文明A] [文明B] [狀態]` |
| 戰爭 | 兩個文明、已宣戰 | `/ad war start`、`/ad war stop`、`/ad war setlastwar`、`/ad war onlywarriors`；期間可測 `mat_found_warcamp`、`mat_build_cannon` |
| 營地 | `mat_found_camp` | `/ad item give [玩家] mat_found_camp 1`，右鍵；管理 `/camp info｜add｜remove｜upgrade｜undo` |
| 附魔與耐久 | 任一自訂裝備 | `/ad item give …`，再 `/ad item enhance soulbound` |
| 合成配方 | 材料 | `/ad items` 取材料，在工作台合成，對照 `materials.yml` 的 `shape` |
| 權限覆寫 | op 的玩家 | `/ad perm`（plot 權限）；`/ad sbperm`（結構方塊保護，會破壞建築，需先備份） |
| 每日 / 每小時事件 | — | `/ad timer run daily`（計時器名稱 `daily`） |

### 測試安全守則
1. 建立第二個文明、`alltech`、`sbperm` 這類會大量改資料的操作，**前先備份 `civ_game`**。
2. 建立文明的位置要選在**已探索過的區域**：目前探索新區塊可能觸發 CustomMobs 崩潰（見第 4 節）。
3. 每次操作前記下 `/res info`、`/civ info`、`/town info` 的金額。
4. 不使用 `/reload`，一律 `stop` 後重啟。
5. 測完用 `/deop` 取消 op，避免之後的權限測試被覆蓋。

## 2B. 戰爭物品與機制詳解

資料來源：`data/war.yml`、`data/materials.yml`、`data/structures.yml`、`siege/Cannon.java`、`siege/CannonListener.java`、`items/components/BuildCannon.java`、`items/components/FoundWarCamp.java`。標「未驗證」者尚未在伺服器實測。

### 戰爭時間與規則（`war.yml`）
| 設定 | 值 | 說明 |
|---|---|---|
| `time_day` / `time_hour` | 7 / 10 | 每週六 10:00 自動開戰（1 = 週日、7 = 週六） |
| `time_declare_days` | 3 | 開戰前 3 天起不能 `declare war` |
| `time_length` | 120 | 註解寫「小時」，但 `/ad war start` 實測廣播 `War time will last for 2.0 hours`，推測實際單位是分鐘（未驗證） |
| `disable_tp_time_day` / `hour` | 7 / 9 | 開戰前一小時起停用傳送 |
| `cooldown_time` | 168 小時 | 侵略方對同一城鎮再宣戰的冷卻 |
| `upkeep_per_war` | 3000 | **侵略方每場戰爭額外維護費**；`upkeep_per_war_multiplier` 0.005 依雙方分數差加成 |
| `respawn_time` / `control_block_respawn_time` | 30 秒 / 30 秒 | 重生時間；每破壞一個控制方塊再加 30 秒 |
| `control_block_hitpoints_capitol` / `camp` / `townhall` | 100 / 100 / 20 | 控制方塊血量 |
| `captured_penalty` | 0.5 | 被占領後收入與 hammers 的懲罰 |
| `logout_time` / `zombie_time` | 120 秒 / 30 秒 | 被攻擊後登出的 PvP 登出機制 |

目前狀態：HolyRomanEmpire 是宣戰方（`CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv)`），**每日事件會多收 3000 以上的戰爭維護費**。

### 物品一：War Cannon（`mat_build_cannon`）
| 項目 | 內容 |
|---|---|
| 物品 | 外觀為漏斗（item 154），元件 `BuildCannon` + `NoRightClick`，不可正常放置 |
| 合成 | 5 × `mat_cannon_barrel_part` + 2 × `mat_cannon_wheel`（形狀 `ppp / ppp / wpw`）；測試用 `/ad item give [玩家] mat_build_cannon 1` |
| 使用 | **只能在戰爭期間**右鍵（否則 `buildCannon_NotWar`），在玩家所在位置放出一座加農砲（模板 `cannon`），物品被消耗，並向全文明廣播座標 |
| 放置限制 | 不能太高、不能超過高度上限、不能壓到箱子、保護方塊、建築、營地、其他加農砲、道路、不能埋太深 |
| 操作 | 砲身有三塊牌子：**angle**（左右角度，-35 到 35）、**power**（0 到 50）、**fire**。angle/power 牌子左鍵減、右鍵加（固定步進）；**fire 牌子：手持 TNT 左鍵裝填**，每次消耗 1 個，裝到 `tnt_cost` = 3 個後，再點 fire 牌子發射 |
| 冷卻 | 發射後 30 秒（每秒遞減，牌子會顯示） |
| 權限 | 只有同文明成員可操作（否則 `cannon_notMember`） |
| 威力 | 每發破壞方塊 7 個；對玩家傷害 50；對建築傷害 100；最大射程 256 格；砲身血量 50（被打壞後無法使用） |

### 物品二：War Camp 旗幟（`mat_found_warcamp`）
| 項目 | 內容 |
|---|---|
| 合成 | `mat_masonry_mortar`、`mat_milled_lumber`、`mat_compacted_stone`（形狀 `lll / cmc / ccc`） |
| 使用條件 | 右鍵；**只有 leader 或 adviser**；**只能在戰爭期間**；需有城鎮（`FoundWarCamp.foundCamp`） |
| 流程 | 先做位置預覽 → 輸入 `yes` 確認 → 進入互動式提示（`InteractiveWarCampFound`） |
| 規則 | 每個文明最多 3 個（`warcamp.max`）；被毀後重建等待 30 分鐘（`rebuild_timeout`）；控制方塊血量 20 |
| 用途 | 作為前線重生點與據點，控制方塊被破壞會延長該文明的重生時間 |

### 物品三：TNT（原版）
- 兩種用途：**裝填加農砲**（見上），以及戰爭中直接放置炸牆。
- 直接放置：`tnt.yield` 3 個方塊、對玩家傷害 20、對建築傷害 20。

### 戰爭相關建築（`structures.yml`）
| 建築 | id | 需要科技 | 費用 | 維護費 | hammers | 上限 | 數值（`war.yml`） |
|---|---|---|---|---|---|---|---|
| 瞭望塔 Scout Tower | `s_scouttower` | `tech_masonry` | 15000 | 800 | 1500 | 4 | 偵測範圍 400，每 120 秒通報敵人 |
| 箭塔 Arrow Tower | `s_arrowtower` | `tech_artillery` | 50000 | 1500 | 2500 | 6 | 傷害 7、範圍 100、射速 1 秒、最小距離 5 |
| 加農砲塔 Cannon Tower | `s_cannontower` | `tech_advanced_artillery` | 250000 | 3500 | 5000 | 1 | 傷害 8、範圍 130、濺射 30、塔間距 130、最小距離 30 |
| 特斯拉塔 Tesla Tower | `s_teslatower` | `tech_electricity` | 350000 | 4500 | 7500 | 1 | 傷害 8、範圍 150、塔間距 100、最小距離 10 |
| 加農砲船 / 偵察船 | `s_cannonship` / `s_scoutship` | `tech_advanced_artillery` / `tech_sailing`（另需造船廠） | 250000 / 15000 | 3500 / 800 | 5000 / 1500 | 1 / 4 | 需要水域，本環境不建議測 |
| 城牆 Wall / 強化城牆 | — | — | 每垂直段 50 / 100 | — | — | — | 高 6 / 10，最高 200，最多 300 段 |
| 兵營 Barracks | `s_barracks` | `tech_armory` | 15000 | 1000 | 1500 | 1 | 與單位生產相關（細節未驗證） |

以上塔類建築都標有 `strategic: true`、`allow_outside_town: true`，可建在城鎮外。

### 戰爭目標（控制方塊）
- 首都、城鎮核心、營地都有「控制方塊」，血量分別為 100 / 20 / 100。
- 攻擊方破壞控制方塊 → 守方重生時間變長；全數破壞可占領城鎮（收入與 hammers 受 50% 懲罰）。
- 結構方塊平時不可破壞，戰爭期間改為「受傷」模式（`WarRegen`）。

### 測試步驟：加農砲
1. 戰爭已開始，Mapleland 為 op（避免被反作弊踢出）。
2. `/ad item give Mapleland mat_build_cannon 1`，並給 TNT：`/give Mapleland tnt 10`。
3. 站在空曠、未被占用的平地，右鍵加農砲物品 → 預期廣播 `var_buildCannon_Success` 與座標，物品消失。
4. 看砲身的 angle、power、fire 三塊牌子；右鍵 power 牌子加到適當數值，右鍵 angle 牌子調方向。
5. 手持 TNT，左鍵 fire 牌子三次，牌子顯示 `(n/3) TNT` → `Loaded`。
6. 再點 fire 牌子發射，確認冷卻 30 秒倒數、TNT 被清空。
7. 讓另一個文明的玩家（Toxicnnan，需 op）嘗試操作 → 預期 `cannon_notMember`。
8. 破壞砲身方塊測 `cannon.onHit`（砲身血量 50），確認砲毀後牌子提示 `cannon_destroyed`。

### 測試步驟：War Camp
1. `/ad item give Mapleland mat_found_warcamp 1`。
2. 右鍵 → 位置預覽（基岩輪廓）→ `yes` → 依互動提示完成。
3. 再用另一個非 leader 帳號測 `buildWarCamp_errorNotPerms`；非戰爭時間測 `buildWarCamp_errorNotWarTime`。
4. 超過 3 個測 `warcamp.max`。

## 2C. 基準線補測步驟（2026-10-06，階段 A 前的 4 項高優先）

所有步驟前：備份 `civ_game`；記下 `/res info`、`/town info`、`/civ info` 的金額；測完對照 `latest.log`。

**起始快照（2026-10-06 10:04，客戶端）**：Mapleland 金幣 53426、Taxes Owed 0.0；Rome 金庫 6750、Structure Upkeep 600、Tax Rate 0%、Flat Tax 0.0、Growth 122、Hammers 261.375、Beakers 177、Happiness 94% Ecstatic、Culture 845/10000、Interest Rate 0%、`Wars:` 欄位為空；HolyRomanEmpire 金庫 22500、Beakers 178.0、Score 36400。備註：Mapleland 金幣自 64426 降到 53426，期間另有約 11000 的支出（含 23:12 的 `/town deposit 6000`），其餘約 5000 的去向待確認；Hammers 與 Beakers 會隨 Happiness 波動（09:45 為 226.525 / 153.4，Happiness 86%），比較產出時需同時記下 Happiness。`Wars:` 欄位為空與 `/civ dip global` 的不對稱關係一致。

### C1. 稅與債務（2.1.6）
指令（讀碼）：`/town set taxrate [百分比]`（居民稅率，`town.setTaxRate(x/100)`）、`/town set flattax [金額]`（每位居民每日固定稅）、`/civ set taxes [百分比]`（文明向城鎮抽稅）、`/resident paydebt`。
1. 記下 Mapleland 金幣、Rome 金庫、civ 金庫。
2. `/town set flattax 100`、`/town set taxrate 10`。
3. `/ad timer run daily`，日誌預期 `[Town:Rome] Collected 100.0 Coins in resident taxes.`（或依稅率計算），Mapleland 金幣減少，Rome 金庫增加。
4. 欠債路徑：`/econ set Mapleland 50`，再 `/ad timer run daily`；預期稅收不足 → 產生 Taxes Owed / 債務（`/res info` 顯示），再 `/resident paydebt` 還清。
5. 測完還原：`/town set flattax 0`、`/town set taxrate 0`、`/econ set Mapleland [原值]`。
6. 注意：`daily` 會同時扣 Rome 維護費 600 與宣戰方戰爭維護費（若還有 WAR 關係），先確認 Rome 金庫夠。

**第 1 次實測（2026-10-06 10:08，`flattax 100`、`taxrate 10`，`/ad timer run daily`）**：Rome 與 Berlin 皆 `Collected 0.0 Coins in resident taxes`；Rome 付維護費 600（金庫 6750 → 6150）、Berlin 付 500；civ 維護費 0.0；`/town info` 顯示 `Tax Rate: 10% Flat Tax: 100.0`；Mapleland 金幣不變（53426），但 `/res info` 顯示 `Taxes Owed: 100.0`。原因（讀碼）：`Resident.isTaxExempt()` 讓 `mayors`、`assistants` 群組成員免稅（`Town.collectFlatTax` 跳過），而 `/res info` 的 `Taxes Owed` 只是 `getFlatTaxOwed()` 的顯示值，不判斷免稅，造成「顯示欠 100 但實際不收」的外觀誤導（列入已知問題）。因此測稅必須用**非 mayor/assistant 的一般成員**。
**第 2 次嘗試（10:11）**：`/ad res settown Ryo5Syo5 Rome` 成功（`Ryo5Syo5 was moved into town Rome`，Members 變 2，`/resident show` 顯示 `Town: Rome`、`Groups: residents(Rome)`、`Taxes Owed: 100.0`、金幣 250）。但 `/ad timer run daily` 被擋：`CivException: TRIED TO EXECUTE DAILY EVENT TWICE: 6`（`DailyEvent.process:62`）；`/resident show Ryo5Syo5` 在 `Groups:` 之後出現 `Internal Command error`（離線居民，見第 4 節）。原因（讀碼）：`DailyEvent` 以靜態變數 `dayExecuted` 記錄「今天（日期）已執行」，同一個日期只能執行一次，且**不持久化**，伺服器重啟後歸零；因此手動執行過今天後，今晚 20:00 的自然每日事件也會被擋，除非先重啟。
**修正後的作法**：每次要再跑 daily 前都要重啟伺服器（`stop`）；為了一次跑完收稅與欠債兩條路徑，同時準備兩位居民：Ryo5Syo5（金幣 250）與 OPchannel（`/ad res settown OPchannel Rome`、`/econ set OPchannel 50`，不夠付 100），重啟後只跑一次 daily。

**第 3 次實測（10:15–10:18，重啟後只跑一次 daily）**：前置 `OPchannel was moved into town Rome`、`Set Opchannel's balance to 50 Coins.`、Rome `Members: 3 Tax Rate: 10% Flat Tax: 100.0`、金庫 6150。`daily` 日誌：`[Town] Collected 100.0 Coins in resident taxes.`、`Paid 600.0 Coins in town upkeep costs.`。結果：Rome 金庫 6150 → **5800**；Ryo5Syo5 250 → 150（繳 100）；OPchannel 50 → 0.0（不足額，只付得出 50，餘 50 轉為債務）。稅率與固定稅設定、兩位居民搬進 Rome、OPchannel 餘額在重啟後都保留（持久化 PASS）。**金額不吻合**：正確應為 6150 + 100（Ryo）+ 50（OPchannel 部分）− 600 = 5700，實際多了 **100**。原因（讀碼，已用數字驗證）：`EconObject.payToCreditor` 在付得起時已 `objToPay.deposit(amount)`（直接進城鎮金庫）並回傳 amount，`Town.collectFlatTax` 加總後 `DailyTimer.collectTownTaxes` 又呼叫 `t.depositTaxed(townTotal)` 再存一次，**居民成功繳的稅被記入城鎮兩次**（重複入帳／通膨）；付不足時 `payToCreditor` 回傳 0，所以 OPchannel 的 50 只入帳一次、且 `Collected` 訊息少算。詳見第 4 節。

**（舊）下一步**：用 `/ad res settown Ryo5Syo5 Rome`（離線帳號資料存在於 DB，coins 250）讓他成為 Rome 居民，再重跑 daily；預期 Rome `Collected 100.0`，Ryo5Syo5 250 → 150；連跑 3 次可走到欠債路徑（第 3 次餘額不足 100）。

### C2. 建築產出（2.3.4）
- **Farm**（`ti_farm`）：`tech_agriculture`，2500 金幣、800 hammers、upkeep 500；效果：`PLAINS` 生物群系 `GROWTH` +1.5。`/ad civ givetech HolyRomanEmpire tech_agriculture` → `/town deposit 5000` → 站在已 claim 的平原區 `/build Farm` → 完成後比較 `/town info` 的 `Growth`（目前 122）。
- **Library**（`s_library`）：`tech_productivity`，5000 金幣、1500 hammers；完成後比較 Beakers（目前 Rome 153.4）。
- **Bank 利息**（`upgrade_bank_interest_level_1`，每日 0.75%）：需先有 Bank、`tech_writing`、`upgrade_bank_level_2`（需 `tech_innovation`），費用先用 `/town upgrade list` 確認；利息 = `floor(本金 × 利率)`，本金為城鎮金庫餘額。預期：金庫 6750 時每日利息 floor(6750 × 0.0075) = 50。日誌訊息 `var_bank_interestMsg1`。
- 每種都等 hammers 完成（以先前速度，800 hammers 約 90 分鐘），建造期間可並行做其他測試。
- **Farm 啟動紀錄（2026-10-06）**：10:39 `/build Farm` → 基岩輪廓預覽 → 輸入 `yes`（誤打 `/yes` 得 `Unknown command`，確認時要輸入不帶斜線的 `yes`）→ 10:39:40 `[Town] The town has started construction on Farm`；這次沒有出現 `Since you're OP we'll let you build here anyway.`，代表位置檢查本身通過。起始值：Growth 122、Happiness 94%、Hammers 261.375、Beakers 177、Rome 金庫 4875（建造前）。完成後比較 Growth（`PLAINS` 加成 +1.5，預期上升）與 Happiness。

### C3. 損壞與修復（2.3.3）
- **首都與 Town Hall 不能修**（`Structure.repairStructure` 拋 `structure_repair_notCaporHall`）；用 Bank 測。
- 站在 Bank 旁：
  1. `/ad build destroynearest Rome`（不帶 yes 會先預覽要毀哪個）→ `/ad build destroynearest Rome yes`。
  2. `/build repairnearest`：顯示修復費用 = 建造費用 / 2（Bank 2250 → 1125），不帶 `yes` 只提示。
  3. `/build repairnearest yes`：扣城鎮金庫 1125，Bank 恢復。
  4. 反例：對沒壞的結構執行 → `var_cmd_build_repairNotDestroyed`；戰爭期間執行 → `cmd_build_repairNotDuringWar`。
- 同時測：`/build validatenearest`（非戰爭期間）、`/ad build listinvalid`、`/ad build validateall`。

### C4. 強制終止重啟（2.9.2）
1. 做一組可量測的變動：`/pay Toxicnnan 100`、`/town deposit 100`、`/econ add Mapleland 1`。
2. 立刻（約 1 秒內）用工作管理員強制結束 `java.exe`（不要用 `stop`）；MySQL 保持執行。
3. 重啟後比對 `/res info`、`/town info`；記錄哪些變動丟失。
4. 再做一輪：變動後等 10 秒再強制結束，比較丟失範圍，量化 `SQLUpdate` 佇列的風險窗口。
5. 預期有丟失；結果作為重構 `SQLUpdate` 的基準。

## 3. 自動化測試（不需伺服器，接 `mvn test`）

- [ ] `Template` 解析
- [ ] `Town` 稅與債務計算
- [ ] `Localize` key 完整性
- [ ] `SQL` 字串組裝
- [ ] `CommandBase` 分派

## 4. 已知問題登錄（基準線已存在者）

| 日期 | 現象 | 原因（若已知） | 狀態 |
|---|---|---|---|
| | `TaskMaster.cancelTimer` 取消不到計時器 | 從 `tasks` 而非 `timers` 取值 | 待修 |
| 2026-10-05 | `/deny` 在不屬於任何文明的玩家身上 NPE（`DenyCommand:55`） | 未判斷 `getCiv()` 為 null；`/accept` 已有 `hasTown()` 檢查 | 已修，已驗證：無城鎮玩家執行 `/deny` 收到 `No question to respond to.`，不再 NPE |
| 2026-10-05 | 登入與 `/pay` 後 `Duplicate entry '<name>' for key 'name'` | 已證實：`RESIDENTS` id=1 的 uuid 是線上 UUID `f41d1dcf-…`，offline-mode 登入的 UUID 是 `86bcba30-…`，查不到既有 Resident，新物件 id=0 反覆 INSERT（失敗的 INSERT 也消耗了 AUTO_INCREMENT，目前已到 13） | 採方案 A：主測試庫切到 `online-mode=true`，不改資料。**已驗證**：登入無 `No resident found` / `Duplicate entry`；`/res info` 顯示 `Town: Rome`、群組 `mayors(Rome)`、`residents(Rome)`，civ Leaders 與 Rome Mayors 都是 Mapleland25244，個人金庫 64426（不再是新建物件的預設 250）。Rome 內建造權限尚未測 |
| 2026-10-05 | 玩家端：`/res` 顯示 `Town: none`，但 `/civ info` 的 Leaders 仍列出本人；在自己城鎮（Rome）出現「沒有權限破壞方塊」；登入時被當新玩家給 120 分鐘 PvP 保護 | 與 Resident 重複寫入同源：`residentsViaUUID` 查不到舊列，`new Resident` 後 `CivGlobal.addResident` 以 name 覆蓋 `residents` map，原本掛在城鎮與群組的舊物件被架空 | 同上，已由 `residents.sql` 證實；優先級最高 |
| 2026-10-05 | UUID 字串關聯（`Civilization.leaderName`、`PermissionGroup.members`、`Camp.owner_name`、`Town` outlaws、`MissionLogger`）查不到時靜默丟棄，不報錯，leader 與群組成員會無聲消失 | 設計缺陷：`PermissionGroup.loadMembersFromSaveString` 對 null 直接略過，`Civilization.getLeader()` 回傳 null | 持久化重構（Phase 3）處理：至少載入時對查不到的 UUID 印出警告 |
| 2026-10-05 | 登入訊息直接顯示本地化 key `PlayerLoginAsync_loginNotAllies` | `default_lang.yml` 沒有這個 key（`PlayerLoginAsyncTask.java:172` 在用） | 待補 key |
| 2026-10-05 | 客戶端每次進服都在主執行緒丟 `NullPointerException`（`brz.a` 內 `forEach`，之後才出現 `Loaded N advancements`） | 未知；混淆碼看不出來源，推測與配方或進階清單封包有關，**尚未驗證**。需用「移除 CivCraft」對照，確認是不是插件造成 | 2026-10-06 Toxicnnan 的戰爭測試日誌**又大量出現**（每次出現後 `Loaded N advancements` 的數字增加，24 → 26），所以與 Resident 狀態無關；更可能與進度（advancement）封包有關。屬客戶端錯誤，不影響遊戲，Phase 4 升級 API 後再觀察 |
| 2026-10-05 | `/reload` 後日誌出現 `Nag author … not properly shutting down its async tasks` ×2 | `CivCraft.onDisable` 只呼叫 `SQLUpdate.save()`，沒有取消排程任務；`/reload` 後舊任務（含 `SQLUpdate` 無限迴圈）與新任務並存。Spigot 本就不支援 `/reload` | 原本就壞；Scheduler 介面（階段 A 步驟 3）時一併處理。測試時請勿使用 `/reload`，一律 `stop` 後重啟 |
| 2026-10-05 | `/reload` 時 `Failed to save player data for Mapleland25244 / Toxicnnan` | 原因不明，發生在 `/reload` 的插件關閉階段，與 CivCraft 是否有關**未驗證** | 不再用 `/reload`，若 `stop` 重啟仍出現再查 |
| 2026-10-05 | 安裝 Vault 後 Herochat 5.6.7 啟用失敗：`NoSuchMethodError: Server._INVALID_getOnlinePlayers()` | 這份 Herochat jar 是針對很舊的 Bukkit API 編譯，與 1.12.2 不相容 | 環境問題：2.7.4 記 N/A，需要較新的 Herochat 版本才能測 |
| 2026-10-05 | Vault 1.7.3 已載入，但只有 `SuperPermissions`，沒有任何經濟插件 | Vault 只是橋接層，需要另有經濟提供者（如 Essentials） | 2.4.2（Vault 經濟）仍無法測 |
| 2026-10-05 | **伺服器崩潰**（23:17:44，`crash-reports/crash-2026-10-05_23.17.44-server.txt`）：`ReportedException: Exception ticking world` ← `ClassNotFoundException: de.hellfirepvp.api.CustomMobsAPI`，堆疊 `MobSpawner.setActive:142` ← `MobSpawner.<init>:37` ← `MobSpawnerPopulator.buildMobSpawner:35` ← `populate:193`（產生新區塊時） | CustomMobs 4.17 在 1.12.2 自行停用，其類別不再可用；但 `MobSpawner.setActive` 無條件呼叫 `CustomMobsAPI.getSpawnerEditor()`，`MobSpawnerPopulator.populate` 也沒有檢查 `hasCustomMobs`；`CivSettings` 只用 `hasPlugin`（插件存在，不代表已啟用）判斷。玩家探索到預選的魔物生成點區塊就會崩潰 | 原本就壞（環境觸發）。待修：以 `isPluginEnabled("CustomMobs")` 判斷，並在 `setActive` / `populate` 加上保護。**2026-10-06 00:33:14 再次崩潰**（`crash-2026-10-06_00.33.14-server.txt`，這次顯示為 `NoClassDefFoundError`，同一路徑），距上次約 75 分鐘，由玩家探索新區域觸發；兩次都是 Spigot 的正常關服流程，資料有存檔。「避免探索」不可靠 |
| 2026-10-06 | liberate 後兩個文明的關係不對稱：`/civ dip global` 顯示 `HolyRomanEmpire: NEUTRAL with GermanReich`、`GermanReich: WAR with HolyRomanEmpire` | 推測：文明被征服時其關係被清除，liberate（`CivDiplomacyCommand.liberate_cmd`：`setConquered(false)`、`addCiv`）只還原文明本身，沒有重建雙向的 `Relation`；**未驗證** | 待查 `DiplomacyManager` 與 `CivGlobal.setRelation`；Phase 3 持久化重構時處理雙向資料一致性 |
| 2026-10-06 | 強制結束伺服器後，資料庫與世界方塊不一致：修復後的 Bank 資料庫 `hitpoints` 為 200（完好），但方塊在重啟後仍是損壞狀態 | 世界區塊由 Minecraft 定期自動存檔，硬殺會丟失最近的方塊變動；CivCraft 的結構資料則由自己的 `SQLUpdate` 佇列寫入，兩者時機不同；啟動後的結構驗證（`Doing a structure validate... Bank`）不會自動補回方塊 | 預期行為（非 CivCraft bug），但屬一致性風險；正式環境避免硬殺。復原可用 `/build refreshnearest`（待實測）。重構時評估在關鍵變動後觸發 `World.save()` 或在結構驗證時自動偵測並修補 |
| 2026-10-06 | **居民稅重複入帳**：居民成功繳的固定稅被記入城鎮兩次（實測 Rome 多出 100） | `EconObject.payToCreditor` 已把錢存進債權人（城鎮金庫），`DailyTimer.collectTownTaxes` 又用回傳值呼叫 `Town.depositTaxed` 再存一次（`DailyTimer.java:181-187`） | 原本就壞（經濟通膨）；Phase 3 / 經濟層重構時修正，並補單元測試 |
| 2026-10-06 | 居民付不足稅額時 `payToCreditor` 回傳 0（雖已存入部分餘額、其餘轉債務），導致 `Collected N Coins` 訊息少算 | `EconObject.java:166-180` 的 `total` 在部分付款分支從未被更新 | 原本就壞，同上一併處理 |
| 2026-10-06 | 文明所得稅計算疑似使用錯誤變數 | `DailyTimer.java:184` `taxesToCiv = total*taxrate`，其中 `total` 是跨城鎮累計的文明稅額而非該城鎮的 `townTotal`；`Town.depositTaxed` 又再以 `getIncomeTaxRate()` 扣一次 | 推測（本次稅率為 0，尚未實測）；待設 `/civ set taxes` 後驗證 |
| 2026-10-06 | `/resident show [離線玩家]` 在 `Groups:` 行之後出現 `Internal Command error` | `ResidentCommand.show` 第 349–350 行：`Bukkit.getPlayer(resident.getUUID())` 對離線居民回傳 null，隨即 `player.hasPermission(...)` NPE | 原本就壞；管理員查離線居民資料會失敗，現代化時修正（改用 offline player 或先檢查 null） |
| 2026-10-06 | 每日事件同一個日期只能執行一次，且該紀錄（`DailyEvent.dayExecuted`）不持久化 | 靜態變數；手動 `/ad timer run daily` 會用掉當天額度，使當晚自然排程被擋（`TRIED TO EXECUTE DAILY EVENT TWICE`）；若同日重啟則額度歸零，同一天可能執行兩次 | 原本就如此；Phase 3 持久化時改為存 DB。測試時每次再跑 daily 前先重啟 |
| 2026-10-06 | mayor 對自己的城鎮 `/res info` 顯示 `Taxes Owed: 100.0`，但實際免稅（每日稅收 `Collected 0.0`） | `ResidentCommand` 顯示 `getPropertyTaxOwed() + getFlatTaxOwed()`，沒有套用 `isTaxExempt()` | 外觀 / 誤導；現代化時一併修正 |
| 2026-10-06 | 提示訊息寫「War Time starts is 4-6pm CST every Saturday」，但 `war.yml` 為 `time_day: 7`、`time_hour: 10`（週六 10:00） | 提示文字寫死在本地化檔，與設定不一致 | 外觀問題，現代化時改成讀設定 |
| 2026-10-06 | 戰爭期間「處於戰爭狀態的文明成員」若沒有 CivCraft Anti-Cheat 客戶端就會被反覆踢出（`WarAntiCheat.onWarTimePlayerCheck`、`ACManager` 週期檢查）；有 `civ.ac_exempt` 權限者豁免 | 舊版專屬反作弊客戶端機制；`plugin.yml` 沒宣告 `civ.ac_exempt`，未宣告的權限預設只有 op 擁有，所以 op 玩家不受影響 | 原本就如此。測試戰爭時需讓雙方玩家都是 op；列入評估移除 `ACManager` / `WarAntiCheat`。2026-10-06 玩家端截圖確認訊息：`Kicked: You are required to have CivCraft's Anti-Cheat plugin installed to participate in WarTime.Visit https://www.minetexas.com/ to get it.`；該機制依賴已無法控制的外部網站，屬應移除的遺留功能 |
| 2026-10-05 | dynmap 3.6-899 在 Java 8 上無法啟用，`civcraft_dynmap` 隨之 NPE | dynmap 3.6 的部分類別以 Java 17 編譯；`civcraft_dynmap` 在 dynmap 不可用時沒有判斷就取用 API | 環境問題；另外 `civcraft_dynmap` 缺少 null 檢查，列入整合層重構（Phase 1） |
| 2026-10-05 | 玩家端 `Online but NOT validated by CivCraft's Anti-Cheat` | 舊版 AC 驗證需要專用客戶端 mod，正常客戶端永遠不會通過 | 預期行為，之後評估移除 `ACManager` |
| 2026-10-05 | CustomMobs 4.17 在 1.12.2 自行停用，但 CivCraft 仍印 `CustomMobs hooks enabled` | 4.17 只支援 v1_9 到 v1_11；`hasCustomMobs` 只檢查插件存在 | 環境問題，2.7.8 記 N/A |
| 2026-10-05 | Herochat 載入失敗 `UnknownDependencyException: Vault` | 測試服未裝 Vault | 需安裝，2.4.2 / 2.7.4 尚未測 |
| 2026-10-05 | JDBC SSL 警告洗版（約 30 行）；日誌亂碼 | JDBC URL 未設 `useSSL`；JVM 未設 `-Dfile.encoding=UTF-8` | 待處理 |
