# StockCast Frontend Launcher
Write-Host "Starting StockCast React Frontend..." -ForegroundColor Green
Write-Host ""

# Check if node_modules exists
if (-not (Test-Path "frontend\node_modules")) {
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    cd frontend
    npm install
    cd ..
}

# Start the frontend
cd frontend
npm start
