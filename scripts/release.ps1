# Local build -> push registry -> print server start commands
# Usage: .\scripts\release.ps1 [-SkipPush] [-SkipFrontendBuild]
param(
    [switch]$SkipPush,
    [switch]$SkipFrontendBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$Registry = "crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/nova_mall"
$WebImage = "$Registry/nova-mall-web:latest"
$ServerImage = "$Registry/nova-mall-server:latest"

if (-not $SkipFrontendBuild) {
    Write-Host "==> build frontend dist"
    Push-Location frontend
    if (-not (Test-Path node_modules)) { npm ci }
    npm run build:prod
    if (-not (Test-Path dist/index.html)) { throw "frontend/dist/index.html missing" }
    Pop-Location
}

Write-Host "==> docker build backend"
docker build -f backend/Dockerfile -t $ServerImage .

Write-Host "==> docker build frontend (prebuilt dist)"
docker build -f frontend/Dockerfile.release -t $WebImage .

if (-not $SkipPush) {
    Write-Host "==> docker push"
    docker push $ServerImage
    docker push $WebImage
}

Write-Host ""
Write-Host "==> On server (after git pull or copy compose files):"
Write-Host "docker login crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com"
Write-Host "docker compose -f docker-compose.prod.yml --env-file .env pull"
Write-Host "docker compose -f docker-compose.prod.yml --env-file .env up -d"
Write-Host ""
Write-Host "Existing DB (menu fix): mysql ... < sql/wechat_menu_route_fix.sql"
