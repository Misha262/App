import { useEffect, useState, useCallback } from "react";
import { apiGet, apiPost, apiDelete, apiDownload, apiTaskResources } from "../services/api";
import { connectWebSocket, subscribe } from "../services/ws";

export default function Tasks({ user, group, goBack, focusTaskId, onFocusHandled }) {
  const [tasks, setTasks] = useState([]);
  const [title, setTitle] = useState("");
  const [desc, setDesc] = useState("");
  const [deadline, setDeadline] = useState("");
  const [loading, setLoading] = useState(false);
  const [resources, setResources] = useState([]);
  const [attachments, setAttachments] = useState({});
  const [attachChoice, setAttachChoice] = useState({});

  // ===== LOAD RESOURCES =====
  const loadResources = useCallback(async () => {
    if (!group?.groupId) return;
    try {
      const data = await apiGet(`/api/resources?groupId=${group.groupId}`);
      if (Array.isArray(data)) setResources(data);
      else if (Array.isArray(data?.content)) setResources(data.content);
      else setResources([]);
    } catch {
      setResources([]);
    }
  }, [group]);

  // ===== LOAD TASKS =====
  const load = useCallback(async () => {
    if (!group?.groupId) return;

    setLoading(true);
    try {
      const data = await apiGet(`/api/tasks?groupId=${group.groupId}`);
      const list = Array.isArray(data) ? data : [];
      setTasks(list);

      // load attachments per task
      const entries = await Promise.all(
        list.map(async (t) => {
          try {
            const ids = await apiTaskResources.list(t.taskId);
            return [t.taskId, Array.isArray(ids) ? ids : []];
          } catch {
            return [t.taskId, []];
          }
        })
      );
      const map = {};
      entries.forEach(([taskId, ids]) => (map[taskId] = ids));
      setAttachments(map);
    } catch (e) {
      console.error("Failed to load tasks:", e);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }, [group]);

  // ===== LOAD ON MOUNT =====
  useEffect(() => {
    let mounted = true;
    (async () => {
      await loadResources();
      if (mounted) await load();
    })();
    return () => (mounted = false);
  }, [load, loadResources]);

  // ===== WEBSOCKET EVENTS =====
  useEffect(() => {
    connectWebSocket();

    const unsubscribe = subscribe((msg) => {
      if (msg.type !== "EVENT") return;
      if (msg.groupId !== group.groupId) return;

      if (
        msg.event === "TASK_CREATED" ||
        msg.event === "TASK_UPDATED" ||
        msg.event === "TASK_DELETED" ||
        msg.event === "TASK_STATUS_CHANGED" ||
        msg.event === "TASK_RESOURCE_ATTACHED" ||
        msg.event === "TASK_RESOURCE_DETACHED"
      ) {
        load();
      }
    });

    return unsubscribe;
  }, [group, load]);

  useEffect(() => {
    if (!focusTaskId) return;
    const target = document.querySelector(`[data-task-id="${focusTaskId}"]`);
    if (target) {
      target.scrollIntoView({ behavior: "smooth", block: "center" });
    }
    onFocusHandled?.();
  }, [focusTaskId, tasks, onFocusHandled]);

  // ===== CREATE TASK =====
  const createTask = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;

    await apiPost("/api/tasks", {
      groupId: group.groupId,
      createdBy: user.userId,
      title,
      description: desc,
      status: "OPEN",
      deadline: deadline || null,
    });

    setTitle("");
    setDesc("");
    setDeadline("");
    await load();
  };

  // ===== UPDATE STATUS (CORRECT ENDPOINT) =====
  const changeStatus = async (taskId, status) => {
    await apiPost(`/api/tasks/${taskId}/status`, { status });
    await load();
  };

  // ===== DELETE TASK =====
  const deleteTask = async (taskId) => {
    await apiDelete(`/api/tasks/${taskId}`);
    await load();
  };

  // ===== ATTACH RESOURCE =====
  const attachResource = async (taskId) => {
    const resId = attachChoice[taskId];
    if (!resId) return;
    await apiTaskResources.attach(taskId, Number(resId));
    setAttachChoice((c) => ({ ...c, [taskId]: "" }));
    await load();
  };

  const detachResource = async (taskId, resId) => {
    await apiTaskResources.detach(taskId, resId);
    await load();
  };

  const attachmentNames = (taskId) => {
    const ids = attachments[taskId] || [];
    return ids.map((id) => {
      const meta = resources.find((r) => (r.id ?? r.resourceId) === id);
      return { id, name: meta?.title || meta?.originalFilename || `Resource #${id}` };
    });
  };

  // ===== SKELETON =====
  const SkeletonTask = () => (
    <div className="task-pill">
      <div style={{ flex: 1 }}>
        <div className="skeleton skeleton-text" style={{ width: "50%" }} />
        <div className="skeleton skeleton-text" style={{ width: "80%" }} />
      </div>
      <div style={{ width: 120 }}>
        <div className="skeleton skeleton-text" style={{ width: "100%" }} />
      </div>
    </div>
  );

  return (
    <div className="tasks-wrapper fade-in">
      {/* BACK BUTTON */}
      <button className="btn-ghost" onClick={goBack}>
        ← Back to groups
      </button>

      <h2 style={{ marginTop: 10 }}>{group.name} — Tasks</h2>

      {/* CREATE FORM */}
      <form className="task-create-form" onSubmit={createTask}>
        <input
          className="input"
          placeholder="Task title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        <textarea
          className="textarea"
          placeholder="Description"
          value={desc}
          onChange={(e) => setDesc(e.target.value)}
        />

        <input
          className="input"
          type="datetime-local"
          value={deadline}
          onChange={(e) => setDeadline(e.target.value)}
        />

        <button className="btn-primary">+ Create task</button>
      </form>

      {/* TASKS LIST */}
      <div className="tasks-list">
        {loading && (
          <>
            <SkeletonTask />
            <SkeletonTask />
            <SkeletonTask />
          </>
        )}

        {!loading && tasks.length === 0 && (
          <div style={{ opacity: 0.7, marginTop: 20 }}>No tasks yet.</div>
        )}

        {!loading &&
          tasks.map((t) => (
            <div
              key={t.taskId}
              className={"task-pill" + (focusTaskId === t.taskId ? " task-pill-focus" : "")}
              data-task-id={t.taskId}
            >
              <div>
                <div className="task-title">{t.title}</div>
                <div className="task-desc">{t.description}</div>

                {t.deadline && (
                  <div className="task-deadline">
                    Deadline: {new Date(t.deadline).toLocaleString()}
                  </div>
                )}

                {/* Attachments */}
                {attachmentNames(t.taskId).length > 0 && (
                  <div className="task-attachments">
                    {attachmentNames(t.taskId).map((a) => (
                      <div key={a.id} className="task-attachment-chip">
                        <span onClick={() => apiDownload(`/api/resources/${a.id}/download`, a.name)}>
                          📎 {a.name}
                        </span>
                        <button className="btn-delete" onClick={() => detachResource(t.taskId, a.id)} title="Remove">
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <select
                className="task-select"
                value={t.status}
                onChange={(e) => changeStatus(t.taskId, e.target.value)}
              >
                <option value="OPEN">OPEN</option>
                <option value="IN_PROGRESS">IN PROGRESS</option>
                <option value="DONE">DONE</option>
              </select>

              <div className="task-attach-controls">
                <select
                  className="task-select"
                  value={attachChoice[t.taskId] || ""}
                  onChange={(e) =>
                    setAttachChoice((c) => ({ ...c, [t.taskId]: e.target.value }))
                  }
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
                <button
                  className="btn-primary"
                  type="button"
                  onClick={() => attachResource(t.taskId)}
                  disabled={!attachChoice[t.taskId]}
                >
                  Attach
                </button>
              </div>

              <button className="btn-delete" onClick={() => deleteTask(t.taskId)}>
                Delete
              </button>
            </div>
          ))}
      </div>
    </div>
  );
}
