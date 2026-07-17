const fs = require('fs');
const path = require('path');

const h = (hex) => Buffer.from(hex, 'hex').toString('utf8');
const q = (s) => `'${String(s).replace(/'/g, "''")}'`;

const T = {
  cfgEnabledName: h('e5beaee4bfa1e585ace4bc97e58fb7e58a9fe883bde5bc80e585b3'),
  cfgEnabledRemark: h('e698afe590a6e590afe794a8e5beaee4bfa1e585ace4bc97e58fb7e6a8a1e59d97'),
  cfgDefaultName: h('e5beaee4bfa1e585ace4bc97e58fb7e9bb98e8aea4e8b4a6e58fb74944'),
  cfgDefaultRemark: h('e4b8bae7a9bae697b6e99c80e5898de7abafe698bee5bc8fe4bca0206163636f756e744964'),
  cfgEncryptName: h('e585ace4bc97e58fb7e59b9ee8b083e5af86e69687e6a8a1e5bc8f'),
  cfgEncryptRemark: h('747275653de585bce5aeb92fe5ae89e585a8e6a8a1e5bc8fefbc8c66616c73653de6988ee69687e6a8a1e5bc8f'),
  menuRoot: h('e585ace4bc97e58fb7'),
  menuRootRemark: h('e5beaee4bfa1e585ace4bc97e58fb7e7aea1e79086e79baee5bd95'),
  menuAccount: h('e8b4a6e58fb7e7aea1e79086'),
  menuPublish: h('e68ea8e98081e8aeb0e5bd95'),
  menuMaterial: h('e7b4a0e69d90e7aea1e79086'),
  menuMenu: h('e88f9ce58d95e7aea1e79086'),
  menuReply: h('e887aae58aa8e59b9ee5a48d'),
  menuFans: h('e7b289e4b89de7aea1e79086'),
  menuMessage: h('e6b688e681afe697a5e5bf97'),
  btnAccountQuery: h('e8b4a6e58fb7e69fa5e8afa2'),
  btnAccountAdd: h('e8b4a6e58fb7e696b0e5a29e'),
  btnAccountEdit: h('e8b4a6e58fb7e4bfaee694b9'),
  btnAccountRemove: h('e8b4a6e58fb7e588a0e999a4'),
  btnPublishPush: h('e68ea8e98081e689a7e8a18c'),
  btnMaterialRemove: h('e7b4a0e69d90e588a0e999a4'),
  btnMenuAdd: h('e88f9ce58d95e696b0e5a29e'),
  btnMenuEdit: h('e88f9ce58d95e4bfaee694b9'),
  btnMenuPublish: h('e88f9ce58d95e58f91e5b883'),
  btnReplyAdd: h('e59b9ee5a48de696b0e5a29e'),
  btnReplyEdit: h('e59b9ee5a48de4bfaee694b9'),
  fixComment: h('e4bfaee5a48de4b9b1e7a081e78988wechat_schema.sqle58699e585a5e79a84e4b8ade69687e88f9ce58d95e4b88e9858de7bdaeefbc8ce58fafe9878de5a48de689a7e8a18c'),
};

const sql = `SET NAMES utf8mb4;
USE nova_mall;

-- ${T.fixComment}

UPDATE sys_config SET config_name = ${q(T.cfgEnabledName)}, remark = ${q(T.cfgEnabledRemark)}
WHERE config_key = 'wechat.enabled';

UPDATE sys_config SET config_name = ${q(T.cfgDefaultName)}, remark = ${q(T.cfgDefaultRemark)}
WHERE config_key = 'wechat.defaultAccountId';

UPDATE sys_config SET config_name = ${q(T.cfgEncryptName)}, remark = ${q(T.cfgEncryptRemark)}
WHERE config_key = 'wechat.callback.encrypt';

UPDATE sys_menu SET menu_name = ${q(T.menuRoot)}, remark = ${q(T.menuRootRemark)} WHERE menu_id = 2050;
UPDATE sys_menu SET menu_name = ${q(T.menuAccount)} WHERE menu_id = 2051;
UPDATE sys_menu SET menu_name = ${q(T.menuPublish)} WHERE menu_id = 2052;
UPDATE sys_menu SET menu_name = ${q(T.menuMaterial)} WHERE menu_id = 2053;
UPDATE sys_menu SET menu_name = ${q(T.menuMenu)} WHERE menu_id = 2054;
UPDATE sys_menu SET menu_name = ${q(T.menuReply)} WHERE menu_id = 2055;
UPDATE sys_menu SET menu_name = ${q(T.menuFans)} WHERE menu_id = 2056;
UPDATE sys_menu SET menu_name = ${q(T.menuMessage)} WHERE menu_id = 2057;
UPDATE sys_menu SET menu_name = ${q(T.btnAccountQuery)} WHERE menu_id = 2300;
UPDATE sys_menu SET menu_name = ${q(T.btnAccountAdd)} WHERE menu_id = 2301;
UPDATE sys_menu SET menu_name = ${q(T.btnAccountEdit)} WHERE menu_id = 2302;
UPDATE sys_menu SET menu_name = ${q(T.btnAccountRemove)} WHERE menu_id = 2303;
UPDATE sys_menu SET menu_name = ${q(T.btnPublishPush)} WHERE menu_id = 2304;
UPDATE sys_menu SET menu_name = ${q(T.btnMaterialRemove)} WHERE menu_id = 2305;
UPDATE sys_menu SET menu_name = ${q(T.btnMenuAdd)} WHERE menu_id = 2306;
UPDATE sys_menu SET menu_name = ${q(T.btnMenuEdit)} WHERE menu_id = 2307;
UPDATE sys_menu SET menu_name = ${q(T.btnMenuPublish)} WHERE menu_id = 2308;
UPDATE sys_menu SET menu_name = ${q(T.btnReplyAdd)} WHERE menu_id = 2309;
UPDATE sys_menu SET menu_name = ${q(T.btnReplyEdit)} WHERE menu_id = 2310;
`;

const out = path.join(__dirname, 'wechat_encoding_fix.sql');
fs.writeFileSync(out, sql, 'utf8');
console.log('written', out);
