class WebSocketService {
  constructor() {
    this.ws = null;
    this.listeners = {
      onMessage: [],
      onConnect: [],
      onDisconnect: [],
      onError: []
    };
    this.reconnectInterval = null;
    this.reconnectDelay = 3000;
  }

  connect(url = 'ws://localhost:9091/ws/stock') {
    // Prevent duplicate connections - check CONNECTING state too
    if (this.ws && (
      this.ws.readyState === WebSocket.OPEN || 
      this.ws.readyState === WebSocket.CONNECTING
    )) {
      console.log('WebSocket already connected or connecting (state:', this.ws.readyState, ')');
      return;
    }

    // Close any existing connection before creating new one
    if (this.ws) {
      console.log('Closing existing WebSocket before reconnecting');
      this.ws.close();
      this.ws = null;
    }

    console.log('Connecting to WebSocket:', url);
    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.listeners.onConnect.forEach(cb => cb());

      if (this.reconnectInterval) {
        clearInterval(this.reconnectInterval);
        this.reconnectInterval = null;
      }
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('Received:', data);
        this.listeners.onMessage.forEach(cb => cb(data));
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.listeners.onDisconnect.forEach(cb => cb());

      if (!this.reconnectInterval) {
        this.reconnectInterval = setInterval(() => {
          console.log('Attempting to reconnect...');
          this.connect(url);
        }, this.reconnectDelay);
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      this.listeners.onError.forEach(cb => cb(error));
    };
  }

  disconnect() {
    if (this.reconnectInterval) {
      clearInterval(this.reconnectInterval);
      this.reconnectInterval = null;
    }

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.close();
      this.ws = null;
    }
  }

  // ✅ New universal send method
  send(command, payload = {}) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ command, ...payload });
      console.log('Sending:', message);
      this.ws.send(message);
    } else {
      console.warn('WebSocket not connected. Cannot send:', command);
    }
  }

  subscribe(tickers) {
    this.send('SUBSCRIBE', { tickers });
  }

  unsubscribe(tickers) {
    this.send('UNSUBSCRIBE', { tickers });
  }

  listSubscriptions() {
    this.send('LIST');
  }

  ping() {
    this.send('PING');
  }

  on(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event].push(callback);
    }
  }

  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
    }
  }

  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN;
  }
}

const webSocketService = new WebSocketService();
export default webSocketService;
