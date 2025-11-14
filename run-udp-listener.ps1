# StockCast UDP Multicast Listener Launcher
Write-Host "Starting StockCast UDP Multicast Listener..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

# Compile and run from backend directory
Push-Location "$PSScriptRoot\backend"
try {
    # Compile if needed
    mvn compile -q
    
    # Run the UDP listener
    java -cp target/classes com.stockcast.client.UDPMulticastListener
}
finally {
    Pop-Location
}
