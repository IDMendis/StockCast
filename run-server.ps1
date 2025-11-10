# StockCast Server Launcher
Write-Host "Starting StockCast Server..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Change into backend and run the server using Maven
Push-Location "$PSScriptRoot\backend"
try {
    mvn spring-boot:run
}
finally {
    Pop-Location
}
