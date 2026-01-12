export interface FrontendVersionInfo {
  commit?: string
  branch?: string
  buildTime?: string
  buildNumber?: string | number
}

export async function getFrontendVersionInfo(): Promise<FrontendVersionInfo> {
  // version.json 由前端构建脚本生成，属于静态资源，不走 /api 代理
  const resp = await fetch('/version.json', {
    cache: 'no-store'
  })

  if (!resp.ok) {
    throw new Error(`Failed to fetch /version.json: ${resp.status}`)
  }

  return (await resp.json()) as FrontendVersionInfo
}
