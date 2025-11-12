
import React, { useState, useEffect } from 'react';
import './App.css';
import WebSocketService from './services/WebSocketService';
import StockCard from './components/StockCard';
import SubscriptionPanel from './components/SubscriptionPanel';
import ConnectionStatus from './components/ConnectionStatus';
import MetricsPanel from './components/MetricsPanel';


function App() {
  const [connected, setConnected] = useState(false);
  const [clientId, setClientId] = useState('');
  const [availableTickers, setAvailableTickers] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [stockPrices, setStockPrices] = useState({});
  const [priceHistory, setPriceHistory] = useState({}); // ticker -> [prices]
  const [message, setMessage] = useState('');


  useEffect(() => {
    WebSocketService.connect();

    const handleMessage = (data) => {
      switch (data.type) {
        case 'WELCOME':
          setConnected(true);
          setClientId(data.clientId);
          setAvailableTickers(data.availableTickers || []);
          if (data.currentPrices) {
            const prices = {};
            const histories = {};
            Object.entries(data.currentPrices).forEach(([ticker, price]) => {
              prices[ticker] = {
                ticker,
                price,
                changePercent: 0,
                timestamp: new Date().toISOString()
              };
              histories[ticker] = [Number(price)];
            });
            setStockPrices(prices);
            setPriceHistory(histories);
          }
          setMessage('Connected to StockCast server!');
          break;
        case 'PRICE':
          setStockPrices(prev => ({
            ...prev,
            [data.ticker]: {
              ticker: data.ticker,
              price: data.price,
              changePercent: data.changePercent,
              timestamp: data.timestamp
            }
          }));
          setPriceHistory(prev => {
            const prevArr = prev[data.ticker] || [];
            const nextArr = [...prevArr.slice(-29), Number(data.price)];
            return { ...prev, [data.ticker]: nextArr };
          });
          break;
        case 'ACK':
          setMessage(data.message);
          setTimeout(() => setMessage(''), 3000);
          break;
        case 'SUBSCRIPTIONS':
          setSubscriptions(data.subscriptions || []);
          break;
        case 'ERROR':
          setMessage('Error: ' + data.message);
          setTimeout(() => setMessage(''), 5000);
          break;
        default:
          console.log('Unknown message type:', data.type);
      }
    };

    const handleConnect = () => {
      setConnected(true);
      setMessage('Connected!');
    };

    const handleDisconnect = () => {
      setConnected(false);
      setMessage('Disconnected. Reconnecting...');
    };

    WebSocketService.on('onMessage', handleMessage);
    WebSocketService.on('onConnect', handleConnect);
    WebSocketService.on('onDisconnect', handleDisconnect);

    return () => {
      WebSocketService.off('onMessage', handleMessage);
      WebSocketService.off('onConnect', handleConnect);
      WebSocketService.off('onDisconnect', handleDisconnect);
      WebSocketService.disconnect();
    };
  }, []);


  const handleSubscribe = (tickers) => {
    if (tickers && tickers.length > 0) {
      WebSocketService.subscribe(tickers);
      setSubscriptions(prev => [...new Set([...prev, ...tickers])]);
    }
  };

  const handleUnsubscribe = (ticker) => {
    WebSocketService.unsubscribe([ticker]);
    setSubscriptions(prev => prev.filter(t => t !== ticker));
  };


  return (
    <div className="App">
      <header className="app-header">
        <h1>📈 StockCast</h1>
        <p>Real-Time Stock Price Broadcasting</p>
        <ConnectionStatus connected={connected} clientId={clientId} />
      </header>

      {message && (
        <div className="message-banner">
          {message}
        </div>
      )}

      <MetricsPanel />

      <div className="app-container">
        <SubscriptionPanel
          availableTickers={availableTickers}
          subscriptions={subscriptions}
          onSubscribe={handleSubscribe}
          onUnsubscribe={handleUnsubscribe}
        />

        <div className="stock-grid">
          {subscriptions.length === 0 ? (
            <div className="empty-state">
              <h2>No subscriptions yet</h2>
              <p>Select stocks from the panel to start tracking prices</p>
            </div>
          ) : (
            subscriptions.map(ticker => (
              <StockCard
                key={ticker}
                ticker={ticker}
                data={stockPrices[ticker]}
                history={priceHistory[ticker] || []}
                onUnsubscribe={handleUnsubscribe}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
