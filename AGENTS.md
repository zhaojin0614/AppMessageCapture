# AGENTS.md — 项目协作约定

## Git 提交

- **每次代码修改完成并验证通过后，自动执行 `git commit`，无需询问用户。**
  这是用户（zhaojin0614）的明确要求。
- 提交信息用中文，遵循仓库现有 conventional 风格：
  `feat(scope):` / `fix:` / `refactor:` / `docs:` / `build:` / `chore:`
- **不要自动 push**，除非用户明确要求。

## 验证

- 提交前至少通过 `./gradlew :app:compileDebugKotlin`；
  涉及账单解析/去重等业务逻辑时跑 `./gradlew :app:testDebugUnitTest`。
- 真机验证脚本见 `运行清单.md` 第九/十节（模拟通知 / 模拟支付成功页）。

## 项目要点（速查）

- 账单解析纯函数在 `service/BillParsing.kt`、应用门槛表在
  `service/SupportedPaymentApps.kt`——改这两处必须补/改对应单元测试。
- 通知捕获与无障碍屏幕捕获共用入库管线 `service/BillIngestor.kt`
  （去重 + 跨 App 合并），不要在单一服务里另写入库逻辑。
- 无障碍监视名单两处同步：`SupportedPaymentApps.screenWatchPackages`
  与 `res/xml/payment_screen_accessibility_config.xml` 的 `packageNames`。
- 平台余额变动必须走 `AccountRepository` 的事务方法，禁止直接改余额。
