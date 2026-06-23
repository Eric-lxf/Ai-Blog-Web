const fs = require('fs')
const path = require('path')

// ASCII-only source file: Chinese via \u escapes (safe on Windows).
const dir = path.join(__dirname, '../frontend/src/views/wechat')
const expected = {
  account: ['\u8d26\u53f7\u540d\u79f0'],
  menu: ['\u8d26\u53f7', '\u83dc\u5355'],
  reply: ['\u8d26\u53f7', '\u81ea\u52a8\u56de\u590d'],
  publish: ['\u5f85\u5904\u7406'],
  material: ['\u7d20\u6750'],
  fans: ['\u5df2\u5173\u6ce8', '\u6635\u79f0'],
  message: ['\u63a5\u6536', '\u53d1\u9001'],
}

// Mojibake markers when GBK bytes were saved as UTF-8 text.
const mojibake = ['\u94b6\u2039\u5f39\u53f7', '\u9398\u6001', '\u93c8\u6b3a\u6ce8']

let failed = false
for (const [name, words] of Object.entries(expected)) {
  const file = path.join(dir, name, 'index.vue')
  const s = fs.readFileSync(file, 'utf8')
  const ok = words.every((w) => s.includes(w))
  const bad = mojibake.some((w) => s.includes(w))
  if (!ok || bad) {
    console.error('FAIL', file, !ok ? 'missing-utf8' : '', bad ? 'mojibake' : '')
    failed = true
  } else {
    console.log('OK', name)
  }
}

if (failed) {
  console.error('\nFix: edit scripts/gen_wechat_views*.js (use \\uXXXX), then:')
  console.error('  node scripts/gen_wechat_views.js')
  console.error('  node scripts/verify-wechat-encoding.js')
  process.exit(1)
}
