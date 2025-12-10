// src/pages/Groups.jsx
import { useEffect, useState, useCallback, useRef } from "react";
import { apiGet, apiPost, apiMembers, apiPut, apiDelete } from "../services/api";

export default function Groups({
  user,
  openTasks,
  selectedGroup,
  onNotify,
  onGroupsSnapshot,
  onSelectGroup,
}) {
  const [groups, setGroups] = useState([]);
  const [active, setActive] = useState(null);

  const [name, setName] = useState("");
  const [desc, setDesc] = useState("");

  const [loadingGroups, setLoadingGroups] = useState(false);
  const [loadingTasks, setLoadingTasks] = useState(false);

  const [allTasks, setAllTasks] = useState([]);
  const [tasksMode, setTasksMode] = useState("all");
  const [members, setMembers] = useState([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [newMemberId, setNewMemberId] = useState("");
  const [newMemberRole, setNewMemberRole] = useState("MEMBER");
  const [showSettings, setShowSettings] = useState(false);
  const [editName, setEditName] = useState("");
  const [editDesc, setEditDesc] = useState("");

  const now = new Date();
  const remindersCache = useRef(new Set());

  const loadGroups = useCallback(async () => {
    if (!user?.userId) return;

    setLoadingGroups(true);

    try {
      const res = await apiGet(`/api/groups?userId=${user.userId}`);

      if (Array.isArray(res)) {
        setGroups(res);
        onGroupsSnapshot?.(res);
        setActive((old) => old || selectedGroup || res[0] || null);
      } else {
        setGroups([]);
        onGroupsSnapshot?.([]);
      }
    } catch (e) {
      console.error("Failed to load groups:", e);
      setGroups([]);
      onGroupsSnapshot?.([]);
    } finally {
      setLoadingGroups(false);
    }
  }, [user, selectedGroup, onGroupsSnapshot]);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

  const activeGroup = active || selectedGroup || groups[0] || null;

  useEffect(() => {
    if (activeGroup) {
      onSelectGroup?.(activeGroup);
      loadMembers(activeGroup.groupId);
      setEditName(activeGroup.name || "");
      setEditDesc(activeGroup.description || "");
    }
  }, [activeGroup, onSelectGroup]);

  const loadMembers = useCallback(
    async (groupId) => {
      if (!groupId) return;
      setLoadingMembers(true);
      try {
        const data = await apiMembers.list(groupId);
        if (Array.isArray(data)) setMembers(data);
        else setMembers([]);
      } catch {
        setMembers([]);
      } finally {
        setLoadingMembers(false);
      }
    },
    []
  );

  const checkReminders = useCallback(
    (tasksList) => {
      if (!user?.userId || !onNotify) return;
      const nowMs = Date.now();

      tasksList.forEach((task) => {
        if (!task.deadline || task.createdBy !== user.userId) return;

        const deadlineMs = new Date(task.deadline).getTime();
        if (Number.isNaN(deadlineMs)) return;

        const diff = deadlineMs - nowMs;
        let key = null;
        let message = null;
        const title = task.title || `Task #${task.taskId}`;

        if (diff <= 0) {
          key = `${task.taskId}-overdue`;
          message = `Task "${title}" is overdue.`;
        } else if (diff <= 2 * 60 * 60 * 1000) {
          key = `${task.taskId}-2h`;
          message = `Task "${title}" is due within 2 hours.`;
        } else if (diff <= 24 * 60 * 60 * 1000) {
          key = `${task.taskId}-24h`;
          message = `Task "${title}" is due within 24 hours.`;
        }

        if (key && !remindersCache.current.has(key)) {
          remindersCache.current.add(key);
          onNotify({
            title: "Task reminder",
            text: message,
            action: {
              label: "Open task",
              handler: () => openTasks(task.groupId, { focusTaskId: task.taskId }),
            },
          });
        }
      });
    },
    [user, onNotify, openTasks]
  );

  const loadAllTasks = useCallback(async () => {
    if (!groups.length) {
      setAllTasks([]);
      return;
    }

    setLoadingTasks(true);

    try {
      const results = await Promise.all(
        groups.map((g) => apiGet(`/api/tasks?groupId=${g.groupId}`))
      );

      const flat = [];

      results.forEach((arr, i) => {
        const g = groups[i];
        if (Array.isArray(arr)) {
          arr.forEach((t) =>
            flat.push({
              ...t,
              groupName: g.name,
              groupId: t.groupId,
            })
          );
        }
      });

      flat.sort((a, b) => {
        if (!a.deadline && !b.deadline) return 0;
        if (!a.deadline) return 1;
        if (!b.deadline) return -1;
        return new Date(a.deadline) - new Date(b.deadline);
      });

      setAllTasks(flat);
      checkReminders(flat);
    } catch (e) {
      console.error("Failed to load all tasks:", e);
      setAllTasks([]);
    } finally {
      setLoadingTasks(false);
    }
  }, [groups, checkReminders]);

  useEffect(() => {
    loadAllTasks();
  }, [loadAllTasks]);

  const createGroup = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    await apiPost("/api/groups", {
      name,
      description: desc,
      creatorId: user.userId,
    });

    setName("");
    setDesc("");

    await loadGroups();
    await loadAllTasks();
  };

  const addMember = async () => {
    if (!activeGroup || !newMemberId) return;
    await apiMembers.add(activeGroup.groupId, Number(newMemberId), newMemberRole);
    setNewMemberId("");
    await loadMembers(activeGroup.groupId);
  };

  const removeMember = async (membershipId) => {
    if (!activeGroup) return;
    await apiMembers.remove(activeGroup.groupId, membershipId);
    await loadMembers(activeGroup.groupId);
  };

  const saveGroupSettings = async () => {
    if (!activeGroup) return;
    await apiPut(`/api/groups/${activeGroup.groupId}`, {
      name: editName,
      description: editDesc,
    });
    await loadGroups();
    setShowSettings(false);
  };

  const deleteGroup = async () => {
    if (!activeGroup) return;
    if (!window.confirm("Delete this group? This cannot be undone.")) return;
    await apiDelete(`/api/groups/${activeGroup.groupId}`);
    setShowSettings(false);
    setActive(null);
    await loadGroups();
  };

  const createGroupWithUser = async (member) => {
    try {
      const g = await apiPost("/api/groups", {
        name: `Chat with ${member.userName || "member"}`,
        description: "One-off space",
        creatorId: user.userId,
      });
      if (g?.groupId) {
        await apiMembers.add(g.groupId, member.userId, "MEMBER");
        await loadGroups();
      }
    } catch (e) {
      console.error("Failed to create group with user", e);
    }
  };

  const SkeletonGroupItem = () => (
    <li className="group-item">
      <div className="skeleton skeleton-text" style={{ width: "60%" }} />
      <div className="skeleton skeleton-text" style={{ width: "90%" }} />
      <div className="skeleton skeleton-text" style={{ width: "40%" }} />
    </li>
  );

  const SkeletonTaskRow = () => (
    <div className="task-overview-item">
      <div className="skeleton skeleton-text" style={{ width: "40%" }} />
      <div
        className="skeleton skeleton-text"
        style={{ width: "80%", marginTop: 6 }}
      />
      <div
        className="skeleton skeleton-text"
        style={{ width: "30%", marginTop: 6 }}
      />
    </div>
  );

  return (
    <>
    <div className="dashboard-grid fade-in">
      {/* LEFT COLUMN ? GROUPS LIST */}
      <section className="card">
        <div className="card-header">
          <div>
            <div className="card-title">Your groups</div>
            <div className="card-subtitle">
              {loadingGroups ? "Loading?" : `${groups.length} active spaces`}
            </div>
          </div>
          <span className="card-tag">Today</span>
          {activeGroup && (
            <button className="btn-outline" onClick={() => setShowSettings(true)}>
              Group settings
            </button>
          )}
        </div>

        <ul className="group-list">
          {loadingGroups && [1, 2, 3].map((i) => <SkeletonGroupItem key={i} />)}

          {!loadingGroups &&
            groups.map((g) => (
              <li
                key={g.groupId}
                className={
                  "group-item " + (activeGroup?.groupId === g.groupId ? "active" : "")
                }
                onClick={() => setActive(g)}
              >
                <div className="group-item-name">{g.name}</div>
                <div className="group-item-desc">
                  {g.description || "No description yet"}
                </div>
                <div className="group-item-meta">
                  <span>ID #{g.groupId}</span>
                  <span>Owner #{g.createdBy}</span>
                </div>
              </li>
            ))}

          {!loadingGroups && !groups.length && (
            <div style={{ fontSize: 12, color: "#94a3b8" }}>
              You don't have groups yet. Create one ??
            </div>
          )}
        </ul>

        <form className="create-group-form" onSubmit={createGroup}>
          <div style={{ fontSize: 11, color: "#64748b" }}>Create new group</div>

          <input
            className="input"
            placeholder="Group name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <textarea
            className="textarea"
            placeholder="Description"
            value={desc}
            onChange={(e) => setDesc(e.target.value)}
          />

          <button type="submit" className="btn-primary">+ Create group</button>
        </form>
      </section>

      {/* MIDDLE COLUMN ? TASKS OVERVIEW */}
      <section className="card">
        <div className="card-header">
          <div>
            <div className="card-title">Tasks overview</div>
            <div className="card-subtitle">
              {loadingTasks
                ? "Loading?"
                : `${allTasks.length} tasks across ${groups.length} groups`}
            </div>
          </div>

          <div className="tasks-links">
            <button
              type="button"
              className={"link-button " + (tasksMode === "all" ? "link-active" : "")}
              onClick={() => setTasksMode("all")}
            >
              All tasks
            </button>
            <span style={{ margin: "0 4px" }}>/</span>
            <button
              type="button"
              className={"link-button " + (tasksMode === "grouped" ? "link-active" : "")}
              onClick={() => setTasksMode("grouped")}
            >
              By groups
            </button>
          </div>
        </div>

        <div className="tasks-overview-body">
          {loadingTasks && (
            <>
              <SkeletonTaskRow />
              <SkeletonTaskRow />
              <SkeletonTaskRow />
            </>
          )}

          {!loadingTasks && !allTasks.length && (
            <div style={{ opacity: 0.6, fontSize: 13 }}>
              No tasks yet. Create your first one in any group.
            </div>
          )}

          {!loadingTasks && allTasks.length > 0 && tasksMode === "all" && (
            <div className="tasks-list-flat">
              {allTasks.map((t) => {
                const overdue = t.deadline && new Date(t.deadline) < now;

                return (
                  <div
                    key={t.taskId}
                    className="task-overview-item"
                    style={{
                      borderLeft: overdue ? "3px solid #ef4444" : "3px solid #22c55e",
                    }}
                  >
                    <div className="task-overview-header">
                      <span className="task-overview-title">{t.title}</span>
                      <span className="task-chip">{t.groupName}</span>
                      <span className="task-status-chip">{t.status}</span>
                    </div>

                    <div className="task-overview-desc">{t.description}</div>

                    {t.deadline && (
                      <div
                        className="task-overview-deadline"
                        style={{ color: overdue ? "#ef4444" : "#64748b" }}
                      >
                        {overdue ? "Overdue: " : "Deadline: "}
                        {new Date(t.deadline).toLocaleString()}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {!loadingTasks && tasksMode === "grouped" && allTasks.length > 0 && (
            <div className="tasks-list-grouped">
              {groups
                .map((g) => ({
                  group: g,
                  tasks: allTasks.filter((t) => t.groupId === g.groupId),
                }))
                .filter((entry) => entry.tasks.length > 0)
                .map(({ group, tasks }) => (
                  <div key={group.groupId} className="tasks-group-block">
                    <div className="tasks-group-title">{group.name}</div>

                    {tasks.map((t) => {
                      const overdue = t.deadline && new Date(t.deadline) < now;

                      return (
                        <div
                          key={t.taskId}
                          className="task-overview-item"
                          style={{
                            borderLeft: overdue ? "3px solid #ef4444" : "3px solid #22c55e",
                          }}
                        >
                          <div className="task-overview-header">
                            <span className="task-overview-title">{t.title}</span>
                            <span className="task-status-chip">{t.status}</span>
                          </div>

                          <div className="task-overview-desc">{t.description}</div>

                          {t.deadline && (
                            <div
                              className="task-overview-deadline"
                              style={{ color: overdue ? "#ef4444" : "#64748b" }}
                            >
                              {overdue ? "Overdue: " : "Deadline: "}
                              {new Date(t.deadline).toLocaleString()}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ))}
            </div>
          )}
        </div>
      </section>

      {/* RIGHT COLUMN ? MEMBERS */}
      <section className="card">
        <div className="card-header">
          <div>
            <div className="card-title">Members</div>
            <div className="card-subtitle">
              {activeGroup ? `Group #${activeGroup.groupId}` : "Select a group"}
            </div>
          </div>
        </div>

        {!activeGroup && (
          <div className="neo-empty-state" style={{ background: "transparent", border: "none" }}>
            Pick a group to manage members.
          </div>
        )}

        {activeGroup && (
          <>
            {loadingMembers && <div style={{ opacity: 0.6 }}>Loading membersâ€¦</div>}
            {!loadingMembers && members.length === 0 && (
              <div style={{ opacity: 0.6 }}>No members yet.</div>
            )}

            {!loadingMembers && members.length > 0 && (
              <ul className="group-list">
                {members.map((m) => (
                  <li key={m.membershipId} className="group-item">
                    <div className="group-item-name">
                      {m.userName || `User #${m.userId}`}
                    </div>
                    <div className="group-item-meta">
                      <span>Role: {m.role}</span>
                      <div style={{ display: "flex", gap: 6 }}>
                        <button
                          className="link-button"
                          onClick={() => createGroupWithUser(m)}
                        >
                          Create group
                        </button>
                        <button
                          className="link-button"
                          onClick={() => removeMember(m.membershipId)}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <div className="create-group-form" style={{ marginTop: 10 }}>
              <div style={{ fontSize: 11, color: "#64748b" }}>Add member</div>
              <input
                className="input"
                placeholder="User ID"
                value={newMemberId}
                onChange={(e) => setNewMemberId(e.target.value)}
              />
              <select
                className="input"
                value={newMemberRole}
                onChange={(e) => setNewMemberRole(e.target.value)}
              >
                <option value="MEMBER">MEMBER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <button className="btn-primary" type="button" onClick={addMember}>
                + Add member
              </button>
            </div>
          </>
        )}
      </section>

    </div>

      {showSettings && activeGroup && (
        <div className="modal-backdrop" onClick={() => setShowSettings(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Group settings</h3>
              <button className="neo-toast-close" onClick={() => setShowSettings(false)}>×</button>
            </div>

            <div className="modal-section">
              <div className="label">Name</div>
              <input
                className="input"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
              />
            </div>

            <div className="modal-section">
              <div className="label">Description</div>
              <textarea
                className="textarea"
                value={editDesc}
                onChange={(e) => setEditDesc(e.target.value)}
              />
            </div>

            <div className="modal-section" style={{ display: "flex", gap: 12 }}>
              <button className="btn-primary" onClick={saveGroupSettings}>
                Save
              </button>
              <button className="btn-delete" type="button" onClick={deleteGroup}>
                Delete group
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

