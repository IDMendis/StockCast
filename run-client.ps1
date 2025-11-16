# StockCast Client Launcher
param(
    [string]$serverHost = "localhost",
    [int]$port = 9092
)

# Fix Windows PowerShell terminal input issues
Write-Host "🔧 Configuring terminal for clean input..." -ForegroundColor Cyan

# Reset console input mode to prevent control character issues
[Console]::TreatControlCAsInput = $false
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Clear any stuck input buffer
while ([Console]::KeyAvailable) {
    [Console]::ReadKey($true) | Out-Null
}

Write-Host "✓ Terminal configured" -ForegroundColor Green
Write-Host ""
Write-Host "Connecting to StockCast Server at $serverHost`:$port..." -ForegroundColor Green
Write-Host ""

# Compile and run from backend directory
Push-Location "$PSScriptRoot\backend"
try {
    mvn compile -q
    
    # Run with proper console mode
    $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
    java -cp target/classes com.stockcast.client.StockCastClient $serverHost $port
}
finally {
    Pop-Location
    # Clear environment variable
    Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
}
