import React, { useEffect, useRef, useState } from 'react';
import './StockCard.css';

function StockCard({ ticker, data, history = [], onUnsubscribe }) {
  // ✅ Move all hooks to the top — must run every render
  const prevPriceRef = useRef(null);
  const [flash, setFlash] = useState(''); // 'up' | 'down' | ''

  useEffect(() => {
    const prev = prevPriceRef.current;
    if (typeof prev === 'number' && typeof data?.price === 'number') {
      if (data.price > prev) setFlash('up');
      else if (data.price < prev) setFlash('down');
      const t = setTimeout(() => setFlash(''), 600);
      return () => clearTimeout(t);
    }
    prevPriceRef.current = data?.price;
  }, [data?.price]);

  useEffect(() => {
    prevPriceRef.current = data?.price;
  }, [data?.price]);

  // 🩶 If no data, show loading
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

  // Build a simple sparkline path from history
  const sparkPoints = history.map(Number).filter(n => !Number.isNaN(n));
  const hasSpark = sparkPoints.length >= 2;
  let pathD = '';
  if (hasSpark) {
    const w = 100;
    const h = 40;
    const min = Math.min(...sparkPoints);
    const max = Math.max(...sparkPoints);
    const span = max - min || 1;
    const stepX = w / (sparkPoints.length - 1);
    const points = sparkPoints.map((v, i) => {
      const x = i * stepX;
      const y = h - ((v - min) / span) * h;
      return [x, y];
    });
    pathD = points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`).join(' ');
  }

  return (
    <div className={`stock-card ${isPositive ? 'positive' : isNegative ? 'negative' : 'neutral'}`}>
      <div className="stock-header">
        <h3>{ticker}</h3>
        <button className="close-btn" onClick={() => onUnsubscribe(ticker)} aria-label={`Unsubscribe ${ticker}`}>
          ✕
        </button>
      </div>
      
      <div className={`stock-price ${flash ? `flash-${flash}` : ''}`}>
        ${typeof price === 'number' ? price.toFixed(2) : price}
      </div>
      
      <div className="stock-change" aria-live="polite">
        <span className="change-icon" aria-hidden>
          {isPositive ? '▲' : isNegative ? '▼' : '●'}
        </span>
        <span className="change-value">
          {isPositive ? '+' : ''}{typeof changePercent === 'number' ? changePercent.toFixed(2) : changePercent}%
        </span>
      </div>

      {hasSpark && (
        <div className="sparkline" aria-hidden>
          <svg viewBox="0 0 100 40" preserveAspectRatio="none">
            <path className={`sparkline-path ${isPositive ? 'positive' : isNegative ? 'negative' : 'neutral'}`} d={pathD} />
          </svg>
        </div>
      )}

      <div className="stock-pulse"></div>
    </div>
  );
}

export default StockCard;
