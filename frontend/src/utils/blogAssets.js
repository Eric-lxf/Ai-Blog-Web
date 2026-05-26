/**
 * 博客静态资源（上传图片、Markdown 内 /uploads 路径）解析。
 * 可通过 .env 配置 VITE_APP_UPLOAD_ORIGIN（如 http://localhost:8080）拼完整 URL；
 * 未配置时返回相对路径，由部署代理或网关访问后端。
 */
const UPLOAD_ORIGIN = (import.meta.env.VITE_APP_UPLOAD_ORIGIN || '').replace(/\/$/, '')

export function resolveUploadUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (!UPLOAD_ORIGIN) return url.startsWith('/') ? url : `/${url}`
  return `${UPLOAD_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`
}

export function resolveMarkdownAssets(markdown) {
  if (!markdown) return ''
  if (!UPLOAD_ORIGIN) return markdown
  const origin = UPLOAD_ORIGIN
  return markdown
    .replace(/\]\(\/uploads\//g, `](${origin}/uploads/`)
    .replace(/src="\/uploads\//g, `src="${origin}/uploads/`)
}
