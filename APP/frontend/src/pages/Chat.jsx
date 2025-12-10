import { useEffect, useRef, useState, useCallback } from "react";
import { apiGet, apiDownload, WS_BASE } from "../services/api";

function SkeletonMessage() {
  return <div className="chat-message skeleton" style={{ width: "60%" }}></div>;
}

export default function Chat({ user, group, goBack, onNotify }) {
  const groupId = group?.groupId || group?.id;

  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [typingUsers, setTypingUsers] = useState([]);
  const [online, setOnline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [resources, setResources] = useState([]);
  const [resourceId, setResourceId] = useState("");

  const socketRef = useRef(null);
  const logRef = useRef(null);
  const typingTimeouts = useRef({});

  const scrollToBottom = useCallback(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, []);

  const handleTyping = useCallback((msg) => {
    const { userId, userName } = msg;

    setTypingUsers((prev) => {
      if (!prev.includes(userName)) return [...prev, userName];
      return prev;
    });

    if (typingTimeouts.current[userId]) clearTimeout(typingTimeouts.current[userId]);

    typingTimeouts.current[userId] = setTimeout(() => {
      setTypingUsers((prev) => prev.filter((u) => u !== userName));
    }, 2000);
  }, []);

  // Load resources for quick sharing
  useEffect(() => {
    if (!groupId) return;
    (async () => {
      try {
        const res = await apiGet(`/api/resources?groupId=${groupId}`);
        if (Array.isArray(res)) setResources(res);
        else if (Array.isArray(res?.content)) setResources(res.content);
        else setResources([]);
      } catch {
        setResources([]);
      }
    })();
  }, [groupId]);

  useEffect(() => {
    if (!groupId) return;

    const ws = new WebSocket(`${WS_BASE}/ws/chat`);
    socketRef.current = ws;

    ws.onopen = () => {
      ws.send(
        JSON.stringify({
          type: "join",
          groupId: groupId,
          userId: user.userId,
          userName: user.name,
        })
      );

      setTimeout(() => setLoading(false), 300);
    };

    ws.onerror = (e) => console.error("WS error", e);
    ws.onclose = () => console.log("WS Closed");

    ws.onmessage = (event) => {
      let msg = {};
      try {
        msg = JSON.parse(event.data);
      } catch {
        return;
      }

      if (msg.type === "online") {
        setOnline(msg.users || []);
        return;
      }

      if (msg.type === "typing") {
        handleTyping(msg);
        return;
      }

      if (msg.type === "message") {
        setMessages((prev) => [...prev, msg]);
        scrollToBottom();

        if (msg.userId !== user.userId && onNotify) {
          const summary = msg.resourceTitle
            ? `Shared resource: ${msg.resourceTitle}`
            : msg.text;
          onNotify({
            title: `New message in ${group.name}`,
            text: summary,
          });
        }
      }
    };

    return () => ws.close();
  }, [groupId, user, onNotify, handleTyping, scrollToBottom, group?.name]);

  const send = useCallback(() => {
    const trimmed = text.trim();
    if (!trimmed && !resourceId) return;

    const ws = socketRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;

    const resource = resources.find((r) => (r.id ?? r.resourceId) === Number(resourceId));

    ws.send(
      JSON.stringify({
        type: "message",
        groupId: groupId,
        userId: user.userId,
        userName: user.name,
        text: trimmed,
        resourceId: resource ? (resource.id ?? resource.resourceId) : undefined,
        resourceTitle: resource?.title || resource?.originalFilename,
        timestamp: new Date().toISOString(),
      })
    );

    setText("");
    setResourceId("");
  }, [text, user, groupId, resourceId, resources]);

  const sendTyping = useCallback(() => {
    const ws = socketRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;

    ws.send(
      JSON.stringify({
        type: "typing",
        userId: user.userId,
        userName: user.name,
      })
    );
  }, [user]);

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    } else {
      sendTyping();
    }
  };

  const renderResourceChip = (m) => {
    if (!m.resourceId) return null;
    return (
      <div className="chat-resource-pill">
        <div>
          <div className="chat-resource-title">{m.resourceTitle || "Shared file"}</div>
          <div className="chat-resource-meta">Resource #{m.resourceId}</div>
        </div>
        <button
          className="btn-outline"
          onClick={() => apiDownload(`/api/resources/${m.resourceId}/download`, m.resourceTitle)}
        >
          Download
        </button>
      </div>
    );
  };

  return (
    <div className="chat-wrapper fade-in">
      <button className="btn-ghost" onClick={goBack}>
        ← Back to groups
      </button>

      <div className="chat-header">
        <h2>{group.name} — Chat</h2>
        <div className="chat-online">👥 {online.length} online</div>
      </div>

      <div ref={logRef} className="chat-log">
        {loading && (
          <>
            <SkeletonMessage />
            <SkeletonMessage />
            <SkeletonMessage />
          </>
        )}

        {!loading && messages.length === 0 && (
          <div style={{ opacity: 0.6 }}>No messages yet. Start the conversation 🚀</div>
        )}

        {!loading &&
          messages.map((m, idx) => {
            const mine = m.userId === user.userId;
            return (
              <div key={idx} className={`chat-message ${mine ? "mine" : ""}`}>
                <div className="chat-meta">
                  {m.userName} · {m.timestamp ? new Date(m.timestamp).toLocaleTimeString() : ""}
                </div>
                <div className="chat-text">{m.text}</div>
                {renderResourceChip(m)}
              </div>
            );
          })}

        {typingUsers.length > 0 && (
          <div className="typing-indicator">
            {typingUsers.join(", ")} typing…
            <span className="typing-dot"></span>
            <span className="typing-dot"></span>
            <span className="typing-dot"></span>
          </div>
        )}
      </div>

      <div className="chat-attach-row">
        <select
          className="task-select"
          value={resourceId}
          onChange={(e) => setResourceId(e.target.value)}
        >
          <option value="">Attach resource…</option>
          {resources.map((r) => {
            const id = r.id ?? r.resourceId;
            return (
              <option key={id} value={id}>
                {r.title || r.originalFilename || `Resource #${id}`}
              </option>
            );
          })}
        </select>
        <button className="btn-primary" onClick={send} disabled={!resourceId && !text.trim()}>
          Share
        </button>
      </div>

      <div className="chat-input-row">
        <textarea
          className="chat-input"
          value={text}
          placeholder="Type a message…"
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <button className="btn-send" onClick={send} disabled={!text.trim() && !resourceId}>
          ➤
        </button>
      </div>
    </div>
  );
}
