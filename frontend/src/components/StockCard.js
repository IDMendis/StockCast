import React from 'react';
import './StockCard.css';

function StockCard({ ticker, data, onUnsubscribe }) {
  if (!data) {
    return (
      <div className="stock-card loading">
        <h3>{ticker}</h3>
        <p>Loading...</p>
      </div>
    );
  }

  const { price, changePercent } = data;
  const isPositive = changePercent > 0;
  const isNegative = changePercent < 0;

  return (
    <div className={`stock-card ${isPositive ? 'positive' : isNegative ? 'negative' : 'neutral'}`}>
      <div className="stock-header">
        <h3>{ticker}</h3>
        <button className="close-btn" onClick={() => onUnsubscribe(ticker)}>
          ✕
        </button>
      </div>
      
      <div className="stock-price">
        ${typeof price === 'number' ? price.toFixed(2) : price}
      </div>
      
      <div className="stock-change">
        <span className="change-icon">
          {isPositive ? '▲' : isNegative ? '▼' : '●'}
        </span>
        <span className="change-value">
          {isPositive ? '+' : ''}{typeof changePercent === 'number' ? changePercent.toFixed(2) : changePercent}%
        </span>
      </div>

      <div className="stock-pulse"></div>
    </div>
  );
}

export default StockCard;
