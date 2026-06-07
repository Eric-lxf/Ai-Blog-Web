# WeChat Official Account - Implementation Plan

Branch: `develop`  
Scope: create independent `ruoyi-wechat` module (parallel to `ruoyi-blog`)

## Goal

Build a standalone WeChat module with:
- account management
- article push (draft / publish)
- callback handling
- material/menu/reply/fans/message pages

## Architecture

- New backend module: `backend/ruoyi-wechat`
- Keep one-way dependency: `ruoyi-wechat -> ruoyi-blog`
- Do NOT put WeChat implementation code into `ruoyi-blog`
- API prefixes:
  - admin: `/wechat/*`
  - callback: `/public/wechat/*`

## Task 0 - Baseline

- [ ] Confirm branch: `git branch --show-current` == `develop`
- [ ] Check clean/known workspace: `git status`

## Task 1 - Module scaffold

Files:
- create `backend/ruoyi-wechat/pom.xml`
- modify `backend/pom.xml`
- modify `backend/ruoyi-admin/pom.xml`
- create `backend/ruoyi-wechat/src/main/java/com/ruoyi/wechat/config/WechatModuleConfig.java`

Steps:
- [ ] create module POM with required dependencies
- [ ] register `ruoyi-wechat` in parent modules + dependencyManagement
- [ ] add dependency in `ruoyi-admin`
- [ ] add `@Configuration` + `@ComponentScan("com.ruoyi.wechat")`
- [ ] verify compile: `cd backend && mvn -pl ruoyi-admin -am compile -q`
- [ ] commit: `feat(wechat): scaffold independent ruoyi-wechat module`

## Task 2 - SQL and menu seed

Files:
- create `sql/wechat_schema.sql`

Steps:
- [ ] create tables:
  - `wechat_account`
  - `wechat_publish_record`
  - `wechat_material`
  - `wechat_menu`
  - `wechat_auto_reply`
  - `wechat_fans`
  - `wechat_message_log`
- [ ] seed `sys_config`: `wechat.enabled`, `wechat.callbackBaseUrl`, `wechat.defaultAccountId`
- [ ] seed menu:
  - directory `2050`
  - pages `2051-2057`
  - buttons `2300+`
- [ ] verify in DB
- [ ] commit: `feat(wechat): add schema and menu seed`

## Task 3 - Domain/Mapper/DTO/VO

Files:
- create `backend/ruoyi-wechat/src/main/java/com/ruoyi/wechat/domain/*.java`
- create `backend/ruoyi-wechat/src/main/java/com/ruoyi/wechat/mapper/*.java`
- create `backend/ruoyi-wechat/src/main/java/com/ruoyi/wechat/dto/*.java`
- create `backend/ruoyi-wechat/src/main/java/com/ruoyi/wechat/vo/*.java`

Steps:
- [ ] map each table to one domain model
- [ ] create mapper interfaces extending `BaseMapper<T>`
- [ ] create initial DTO/VO for account and push
- [ ] verify compile
- [ ] commit

## Task 4 - Config and constants

Files:
- create `WechatProperties.java`
- create `WechatConstants.java`
- modify `backend/ruoyi-admin/src/main/resources/application.yml`

Steps:
- [ ] add config keys:
  - `wechat.enabled`
  - `wechat.connect-timeout-ms`
  - `wechat.read-timeout-ms`
- [ ] bind with `@ConfigurationProperties`
- [ ] commit

## Task 5 - API client and token cache

Files:
- create `WechatApiClient.java`
- create `WechatTokenService.java`
- create unit test `WechatTokenServiceTest.java`

Steps:
- [ ] implement common GET/POST wrapper
- [ ] implement token cache key: `wechat:token:{accountId}`
- [ ] handle token invalid errors with one retry
- [ ] run test
- [ ] commit

## Task 6 - Callback verify and message log

Files:
- create `WechatSignatureUtils.java`
- create `PublicWechatController.java`
- create `WechatWebhookService.java` + impl
- create `WechatSignatureUtilsTest.java`

Steps:
- [ ] GET callback returns `echostr` after signature validation
- [ ] POST callback parses xml and stores message log
- [ ] default response `success` when no reply rule matched
- [ ] run test
- [ ] commit

## Task 7 - Account management page

Files:
- backend account controller/service
- frontend `frontend/src/api/wechat/index.js`
- frontend page `frontend/src/views/wechat/config.vue`

Steps:
- [ ] add APIs: list/save/test
- [ ] add permission keys for config
- [ ] implement config page CRUD + connectivity test
- [ ] commit

## Task 8 - Content conversion and materials

Files:
- `WechatContentConverter.java`
- `WechatMaterialService.java` + impl
- `WechatContentConverterTest.java`

Steps:
- [ ] clean html with Jsoup
- [ ] upload images and replace src
- [ ] cache media mapping
- [ ] run test
- [ ] commit

## Task 9 - Draft and publish services

Files:
- `WechatDraftService.java` + impl
- `WechatPublishService.java` + impl

Steps:
- [ ] implement draft add API call
- [ ] implement publish submit API call
- [ ] persist errors to publish record
- [ ] commit

## Task 10 - Push orchestration and publish page

Files:
- `WechatPublishController.java`
- `WechatPushOrchestrator.java` + impl
- `frontend/src/views/wechat/publish.vue`

Steps:
- [ ] load article via `BlogArticleService`
- [ ] validate article status is published
- [ ] orchestrate convert -> upload -> draft -> optional publish
- [ ] add push/list/retry APIs
- [ ] implement publish record page
- [ ] commit

## Task 11 - Article editor integration

Files:
- modify `frontend/src/views/blog/article/edit.vue`
- modify `frontend/src/api/wechat/index.js`

Steps:
- [ ] add push button on published article
- [ ] add modal for account and push mode
- [ ] call `/wechat/push`
- [ ] commit

## Task 12-15 - Operations completion

- [ ] Task 12 material management page
- [ ] Task 13 menu management + sync
- [ ] Task 14 auto reply rules + webhook matching
- [ ] Task 15 fans and message log pages

## Task 16 - Route fallback and OpenAPI scan

Files:
- modify `frontend/src/store/modules/permission.js`
- modify `backend/ruoyi-admin/src/main/resources/application.yml`

Steps:
- [ ] add `wechat/*` fallback imports
- [ ] add `com.ruoyi.wechat.controller` to scan packages
- [ ] commit

## Final verification

- [ ] `cd backend && mvn -pl ruoyi-admin -am compile -q`
- [ ] `cd backend && mvn -pl ruoyi-wechat test -q`
- [ ] `cd frontend && npm run build`
- [ ] wechat menu appears in admin
- [ ] account test works
- [ ] push from editor works
- [ ] callback GET/POST works
- [ ] no WeChat implementation code inside `ruoyi-blog`
