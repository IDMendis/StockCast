import React, { useState } from 'react';
import './SubscriptionPanel.css';

function SubscriptionPanel({ availableTickers, subscriptions, onSubscribe, onUnsubscribe }) {
  const [selectedTickers, setSelectedTickers] = useState([]);
  const [query, setQuery] = useState('');

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
  const filtered = unsubscribedTickers.filter(t => t.toLowerCase().includes(query.toLowerCase()));

  const hasSelection = selectedTickers.length > 0;

  return (
    <div className="subscription-panel">
      <h2>Available Stocks</h2>

      <div className="search-row">
        <input
          type="text"
          placeholder="Search tickers..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search tickers"
        />
      </div>
      
      <div className="ticker-list">
        {filtered.length === 0 ? (
          <p className="no-tickers">{query ? 'No matching tickers' : 'All stocks subscribed!'}</p>
        ) : (
          filtered.map(ticker => (
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

      <button
        className="subscribe-btn"
        onClick={handleSubscribe}
        disabled={!hasSelection}
        title={hasSelection ? '' : 'Select one or more tickers'}
      >
        {hasSelection ? `Subscribe to ${selectedTickers.length} stock${selectedTickers.length > 1 ? 's' : ''}` : 'Subscribe'}
      </button>

      {subscriptions.length > 0 && (
        <div className="subscribed-section">
          <h3>Subscribed ({subscriptions.length})</h3>
          <div className="subscribed-list">
            {subscriptions.map(ticker => (
              <div key={ticker} className="subscribed-item">
                <span>{ticker}</span>
                <button onClick={() => onUnsubscribe(ticker)} aria-label={`Unsubscribe ${ticker}`}>✕</button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default SubscriptionPanel;
