# StockCast UDP Multicast Listener
Write-Host "Starting UDP Multicast Listener..." -ForegroundColor Green
Write-Host "This will receive market-wide announcements via UDP" -ForegroundColor Yellow
Write-Host ""

# Compile if needed
mvn compile -q

# Run the UDP listener
java -cp target/classes com.stockcast.client.UDPMulticastListener
