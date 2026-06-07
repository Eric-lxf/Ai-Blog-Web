# WeChat Official Account - Design Spec

Date: 2026-06-07  
Branch: `develop`  
Status: draft

## Problem

The project has blog and AI writing features, but no WeChat official account capability.
We need a standalone module and one-click article push to WeChat.

## Scope

In scope:
- independent backend module `ruoyi-wechat`
- account/material/menu/reply/fans/message management
- article push from editor to WeChat draft/publish
- callback verification and message logging

Out of scope (v1):
- payment / mini-program / enterprise wechat
- advanced multi-account orchestration
- complex media reply types

## Architecture

Module layout:

```text
ruoyi
- ruoyi-blog
- ruoyi-wechat
- ruoyi-admin
```

Dependency rule:
- `ruoyi-wechat -> ruoyi-blog` (read article only)
- no reverse dependency

## API Contract

Admin APIs:
- `GET /wechat/account/list`
- `POST /wechat/account/save`
- `POST /wechat/account/test/{id}`
- `POST /wechat/push`
- `GET /wechat/publish/list`
- `POST /wechat/publish/retry/{id}`
- `GET /wechat/material/list`
- `POST /wechat/material/upload`
- `GET /wechat/menu/get/{accountId}`
- `POST /wechat/menu/save`
- `POST /wechat/menu/sync/{accountId}`
- `GET /wechat/reply/list`
- `POST /wechat/reply/save`
- `POST /wechat/reply/delete/{id}`
- `GET /wechat/fans/list`
- `POST /wechat/fans/sync/{accountId}`
- `GET /wechat/message/list`

Callback APIs:
- `GET /public/wechat/callback/{accountId}`
- `POST /public/wechat/callback/{accountId}`

## Data Model

SQL file: `sql/wechat_schema.sql`

Tables:
- `wechat_account`
- `wechat_publish_record`
- `wechat_material`
- `wechat_menu`
- `wechat_auto_reply`
- `wechat_fans`
- `wechat_message_log`

Config keys:
- `wechat.enabled`
- `wechat.callbackBaseUrl`
- `wechat.defaultAccountId`
- `wechat.content.sourceUrlTemplate`

Menu ids:
- directory `2050`
- pages `2051-2057`
- button permissions `2300+`

## Core Flows

Push flow:
1. editor calls `/wechat/push`
2. load article via `BlogArticleService`
3. clean html and upload images
4. create draft
5. optional publish
6. write publish record

Callback flow:
1. verify signature for GET callback
2. parse xml for POST callback
3. store message log
4. reply by rules or return `success`

## Frontend

New pages:
- `views/wechat/config.vue`
- `views/wechat/publish.vue`
- `views/wechat/material.vue`
- `views/wechat/menu.vue`
- `views/wechat/reply.vue`
- `views/wechat/fans.vue`
- `views/wechat/message.vue`

Editor integration:
- update `views/blog/article/edit.vue`
- add one-click push action

Route fallback:
- add `wechat/*` fallback entries in `permission.js`

## Security and Config

`application.yml`:

```yaml
wechat:
  enabled: ${WECHAT_ENABLED:false}
  connect-timeout-ms: 10000
  read-timeout-ms: 30000
```

Also:
- mask secret fields in responses
- cache token in redis
- add `com.ruoyi.wechat.controller` to OpenAPI scan

## Acceptance Criteria

Functional:
- account management and connectivity test work
- one-click push creates records and supports retry
- callback GET and POST work
- operations pages are reachable

Technical:
- `mvn -pl ruoyi-admin -am compile` passes
- `mvn -pl ruoyi-wechat test` passes
- frontend build passes
- no WeChat implementation code in `ruoyi-blog`

## Traceability

Execution plan is in:
`docs/superpowers/plans/2026-06-07-wechat-official-account.md`
