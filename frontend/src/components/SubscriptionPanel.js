import React, { useState } from 'react';
import './SubscriptionPanel.css';

function SubscriptionPanel({ availableTickers, subscriptions, onSubscribe, onUnsubscribe }) {
  const [selectedTickers, setSelectedTickers] = useState([]);

  const handleTickerToggle = (ticker) => {
    setSelectedTickers(prev => 
      prev.includes(ticker)
        ? prev.filter(t => t !== ticker)
        : [...prev, ticker]
    );
  };

  const handleSubscribe = () => {
    if (selectedTickers.length > 0) {
      onSubscribe(selectedTickers);
      setSelectedTickers([]);
    }
  };

  const unsubscribedTickers = availableTickers.filter(
    ticker => !subscriptions.includes(ticker)
  );

  return (
    <div className="subscription-panel">
      <h2>Available Stocks</h2>
      
      <div className="ticker-list">
        {unsubscribedTickers.length === 0 ? (
          <p className="no-tickers">All stocks subscribed!</p>
        ) : (
          unsubscribedTickers.map(ticker => (
            <label key={ticker} className="ticker-checkbox">
              <input
                type="checkbox"
                checked={selectedTickers.includes(ticker)}
                onChange={() => handleTickerToggle(ticker)}
              />
              <span className="ticker-name">{ticker}</span>
            </label>
          ))
        )}
      </div>

      {selectedTickers.length > 0 && (
        <button className="subscribe-btn" onClick={handleSubscribe}>
          Subscribe to {selectedTickers.length} stock{selectedTickers.length > 1 ? 's' : ''}
        </button>
      )}

      {subscriptions.length > 0 && (
        <div className="subscribed-section">
          <h3>Subscribed ({subscriptions.length})</h3>
          <div className="subscribed-list">
            {subscriptions.map(ticker => (
              <div key={ticker} className="subscribed-item">
                <span>{ticker}</span>
                <button onClick={() => onUnsubscribe(ticker)}>✕</button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default SubscriptionPanel;
