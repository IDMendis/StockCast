# StockCast Server Launcher
Write-Host "Starting StockCast Server..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Run the server using Maven
mvn spring-boot:run
