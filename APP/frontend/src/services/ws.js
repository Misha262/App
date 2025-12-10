import { WS_BASE } from "./api";

const WS_URL = `${WS_BASE}/ws/chat`;

let socket = null;
let listeners = new Set();

export function connectWebSocket() {
  if (socket && socket.readyState === WebSocket.OPEN) return socket;

  socket = new WebSocket(WS_URL);

  socket.onopen = () => {
    console.log("[WS] Connected");
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      listeners.forEach((fn) => fn(data));
    } catch (e) {
      console.error("[WS] Parse error:", e);
    }
  };

  socket.onclose = () => {
    console.log("[WS] Disconnected, reconnecting in 2s...");
    setTimeout(connectWebSocket, 2000);
  };

  return socket;
}

// подписка
export function subscribe(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

// отправка сообщения
export function sendWS(data) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(data));
  }
}

// Join multiple groups to receive events even when not viewing them
export function joinGroups(groupIds, user) {
  if (!groupIds || groupIds.length === 0 || !user) return;
  connectWebSocket();
  const payload = {
    type: "joinMultiple",
    userId: user.userId,
    userName: user.name,
    groupIds,
  };
  const trySend = () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(payload));
    }
  };
  if (socket && socket.readyState === WebSocket.OPEN) {
    trySend();
  } else {
    setTimeout(trySend, 300);
  }
}
