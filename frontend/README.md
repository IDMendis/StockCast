# StockCast React Frontend

Interactive web interface for the StockCast real-time stock price broadcasting system.

## Features

- 🔄 Real-time stock price updates via WebSocket
- 📊 Live price charts with visual indicators
- ✅ Easy subscription management
- 🎨 Beautiful, responsive UI
- 🔌 Auto-reconnect on connection loss
- 📱 Mobile-friendly design

## Prerequisites

- Node.js 14+ and npm
- StockCast backend server running on port 9090

## Installation

```powershell
cd D:\StockCast\frontend
npm install
```

## Running the App

```powershell
npm start
```

The app will open at `http://localhost:3000` and connect to the backend at `ws://localhost:9090/ws/stock`.

## Usage

1. **Start the backend server first**:
   ```powershell
   cd D:\StockCast
   mvn spring-boot:run
   ```

2. **Start the React frontend**:
   ```powershell
   cd D:\StockCast\frontend
   npm start
   ```

3. **Use the interface**:
   - Check available stocks in the left panel
   - Select stocks and click "Subscribe"
   - Watch real-time price updates in cards
   - Green = price increasing
   - Red = price decreasing
   - Click ✕ to unsubscribe from a stock

## Project Structure

```
frontend/
├── public/
│   └── index.html          # HTML template
├── src/
│   ├── components/
│   │   ├── StockCard.js           # Individual stock display
│   │   ├── StockCard.css
│   │   ├── SubscriptionPanel.js   # Subscription management
│   │   ├── SubscriptionPanel.css
│   │   ├── ConnectionStatus.js    # Connection indicator
│   │   └── ConnectionStatus.css
│   ├── services/
│   │   └── WebSocketService.js    # WebSocket client
│   ├── App.js              # Main component
│   ├── App.css
│   ├── index.js            # Entry point
│   └── index.css
└── package.json
```

## WebSocket Protocol

The frontend communicates with the backend using JSON messages:

### Client → Server
```json
{
  "command": "SUBSCRIBE",
  "tickers": ["AAPL", "GOOG"]
}
```

### Server → Client
```json
{
  "type": "PRICE",
  "ticker": "AAPL",
  "price": 150.25,
  "changePercent": 0.17,
  "timestamp": "2025-01-09T12:34:56"
}
```

## Building for Production

```powershell
npm run build
```

The optimized build will be in the `build/` directory.

## Customization

### Change Backend URL

Edit `src/services/WebSocketService.js`:

```javascript
connect(url = 'ws://your-server:port/ws/stock') {
  // ...
}
```

### Styling

- Main colors: Edit `src/index.css`
- Component styles: Edit individual `.css` files
- Gradient background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`

## Troubleshooting

**"WebSocket disconnected"**
- Ensure backend server is running
- Check backend URL in WebSocketService.js
- Verify firewall allows WebSocket connections

**"Cannot connect to server"**
- Make sure backend is running on port 9090
- Check console for detailed error messages

**Prices not updating**
- Ensure you've subscribed to stocks
- Check browser console for WebSocket messages
- Verify backend is generating prices

## Technologies Used

- React 18
- WebSocket API
- CSS3 with animations
- ES6+ JavaScript

## License

Part of the StockCast educational project.
