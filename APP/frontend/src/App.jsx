import { useState, useEffect, useCallback } from "react";
import "./App.css";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Groups from "./pages/Groups";
import Tasks from "./pages/Tasks";
import Chat from "./pages/Chat";
import GroupResources from "./pages/GroupResources";
import { connectWebSocket, subscribe, joinGroups } from "./services/ws";
import { apiChangePassword, apiChangeEmail } from "./services/api";

const sidebarLinks = [
  { id: "groups", icon: "☰", label: "Groups" },
  { id: "tasks", icon: "🗂", label: "Tasks" },
  { id: "chat", icon: "💬", label: "Chat" },
  { id: "resources", icon: "📁", label: "Resources" },
];

const headerTabs = [
  { id: "groups", label: "Dashboard" },
  { id: "tasks", label: "Tasks" },
  { id: "chat", label: "Chat" },
  { id: "resources", label: "Resources" },
];

function App() {
  const [page, setPage] = useState("login");
  const [user, setUser] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);

  const [notification, setNotification] = useState(null);
  const [unread, setUnread] = useState({});
  const [knownGroups, setKnownGroups] = useState([]);
  const [taskFocusId, setTaskFocusId] = useState(null);
  const [showAccount, setShowAccount] = useState(false);
  const [accountMessage, setAccountMessage] = useState("");
  const [accountError, setAccountError] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [currentPass, setCurrentPass] = useState("");
  const [newPass, setNewPass] = useState("");
  const [accountBusy, setAccountBusy] = useState(false);

  const resolveGroup = useCallback(
    (groupOrId) => {
      if (!groupOrId) return null;
      if (typeof groupOrId === "object") return groupOrId;

      const cached = knownGroups.find((g) => g.groupId === groupOrId);
      if (cached) return cached;

      if (selectedGroup?.groupId === groupOrId) return selectedGroup;

      return { groupId: groupOrId, name: `Group #${groupOrId}` };
    },
    [knownGroups, selectedGroup]
  );

  const handleLogin = (userData) => {
    setUser(userData);
    setPage("groups");
  };

  const handleLogout = () => {
    setUser(null);
    setSelectedGroup(null);
    setNotification(null);
    setUnread({});
    setKnownGroups([]);
    setTaskFocusId(null);
    setPage("login");
  };

  const openAccountSettings = () => {
    setAccountError("");
    setAccountMessage("");
    setNewEmail(user?.email || "");
    setCurrentPass("");
    setNewPass("");
    setShowAccount(true);
  };

  const saveEmail = async () => {
    setAccountError("");
    setAccountMessage("");
    setAccountBusy(true);
    try {
      const res = await apiChangeEmail(newEmail);
      if (res?.user) {
        setUser(res.user);
        localStorage.setItem("user", JSON.stringify(res.user));
      }
      setAccountMessage("Email updated");
    } catch (e) {
      setAccountError(e.message || "Failed to update email");
    } finally {
      setAccountBusy(false);
    }
  };

  const savePassword = async () => {
    setAccountError("");
    setAccountMessage("");
    setAccountBusy(true);
    try {
      await apiChangePassword(currentPass, newPass);
      setAccountMessage("Password updated");
      setCurrentPass("");
      setNewPass("");
    } catch (e) {
      setAccountError(e.message || "Failed to update password");
    } finally {
      setAccountBusy(false);
    }
  };

  const openTasks = useCallback(
    (groupOrId, options = {}) => {
      const target = resolveGroup(groupOrId ?? selectedGroup);
      if (!target) return;
      if (options.focusTaskId) setTaskFocusId(options.focusTaskId);
      else setTaskFocusId(null);
      setSelectedGroup(target);
      setPage("tasks");
    },
    [resolveGroup, selectedGroup]
  );

  const openChat = useCallback(
    (groupOrId) => {
      const target = resolveGroup(groupOrId ?? selectedGroup);
      if (!target) return;
      setUnread((u) => ({ ...u, [target.groupId]: 0 }));
      setSelectedGroup(target);
      setPage("chat");
    },
    [resolveGroup, selectedGroup]
  );

  const openResources = useCallback(
    (groupOrId) => {
      const target = resolveGroup(groupOrId ?? selectedGroup);
      if (!target) return;
      setSelectedGroup(target);
      setPage("resources");
    },
    [resolveGroup, selectedGroup]
  );

  const handlePrimaryNav = (destination) => {
    if (destination === "groups") {
      setPage("groups");
      return;
    }

    if (destination === "tasks") {
      openTasks();
      return;
    }

    if (destination === "chat") {
      openChat();
      return;
    }

    if (destination === "resources") {
      openResources();
    }
  };

  const showNotification = useCallback(({ title, text, action }) => {
    setNotification({ title, text, action });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const handleTaskEvent = useCallback(
    (message) => {
      if (!message || message.type !== "EVENT") return;
      const { event, payload, groupId } = message;
      if (!event || !event.startsWith("TASK_")) return;

      const taskName = payload?.title || payload?.taskId || "Task";
      let text = `Task "${taskName}" was updated`;

      if (event === "TASK_CREATED") {
        text = `Task "${taskName}" was created`;
      } else if (event === "TASK_DELETED") {
        text = `Task "${taskName}" was deleted`;
      } else if (event === "TASK_STATUS_CHANGED") {
        text = `Task "${taskName}" status changed`;
      }

      showNotification({
        title: "Task activity",
        text,
        action: groupId
          ? {
              label: "Open task",
              handler: () => openTasks(groupId, { focusTaskId: payload?.taskId }),
            }
          : undefined,
      });
    },
    [openTasks, showNotification]
  );

  useEffect(() => {
    if (!user) return;
    connectWebSocket();
    const unsubscribe = subscribe(handleTaskEvent);
    return unsubscribe;
  }, [user, handleTaskEvent]);

  useEffect(() => {
    if (user && knownGroups.length) {
      joinGroups(knownGroups.map((g) => g.groupId), user);
    }
  }, [user, knownGroups]);

  const renderMainContent = () => {
    if (!user) {
      if (page === "register") {
        return <Register goLogin={() => setPage("login")} />;
      }

      return <Login onLogin={handleLogin} goRegister={() => setPage("register")} />;
    }

    if (page === "groups") {
      return (
        <Groups
          user={user}
          unread={unread}
          selectedGroup={selectedGroup}
          openTasks={openTasks}
          onNotify={showNotification}
          onGroupsSnapshot={setKnownGroups}
          onSelectGroup={setSelectedGroup}
        />
      );
    }

    if (page === "tasks" && selectedGroup) {
      return (
        <Tasks
          user={user}
          group={selectedGroup}
          goBack={() => setPage("groups")}
          focusTaskId={taskFocusId}
          onFocusHandled={() => setTaskFocusId(null)}
        />
      );
    }

    if (page === "chat" && selectedGroup) {
      return (
        <Chat
          user={user}
          group={selectedGroup}
          goBack={() => setPage("groups")}
          onNotify={showNotification}
          onUnread={(groupId) =>
            setUnread((u) => ({
              ...u,
              [groupId]: (u[groupId] || 0) + 1,
            }))
          }
        />
      );
    }

    if (page === "resources" && selectedGroup) {
      return <GroupResources group={selectedGroup} goBack={() => setPage("groups")} />;
    }

    return (
      <div className="neo-empty-state">
        <p>Select a group from the dashboard to open this section.</p>
      </div>
    );
  };

  const filters = [
    { label: "Date", value: "Now" },
    { label: "Product", value: "All" },
    { label: "Profile", value: user?.name || "You" },
  ];

  const isAuth = Boolean(user);
  const viewKey = isAuth ? `${page}-${selectedGroup?.groupId || "none"}` : page;

  return (
    <div className="neo-app">
      {notification && (
        <div className="neo-toast">
          <div className="neo-toast-content">
            <div>
              <div className="neo-toast-title">{notification.title}</div>
              <div className="neo-toast-text">{notification.text}</div>
            </div>
            {notification.action && (
              <button
                className="neo-toast-action"
                onClick={() => {
                  notification.action?.handler?.();
                  setNotification(null);
                }}
              >
                {notification.action.label || "View"}
              </button>
            )}
          </div>
          <button className="neo-toast-close" onClick={() => setNotification(null)}>
            ×
          </button>
        </div>
      )}

      {!isAuth && (
        <div className="neo-auth-stage">
          <div className="neo-auth-spotlight" />
          <div className="neo-auth-card">{renderMainContent()}</div>
        </div>
      )}

      {isAuth && (
        <div className="neo-shell">
          <aside className="neo-sidebar">
            <div className="neo-brand">
              <div className="neo-logo-mark">SG</div>
              <span className="neo-brand-name">Check Box</span>
            </div>

            <div className="neo-sidebar-links">
              {sidebarLinks.map((link) => {
                const isDisabled =
                  (link.id === "tasks" || link.id === "chat" || link.id === "resources") &&
                  !selectedGroup;
                const isActive =
                  (link.id === "groups" && page === "groups") ||
                  (link.id === "tasks" && page === "tasks") ||
                  (link.id === "chat" && page === "chat") ||
                  (link.id === "resources" && page === "resources");

                return (
                  <button
                    key={link.id}
                    className={`neo-sidebar-link ${isActive ? "is-active" : ""}`}
                    disabled={isDisabled}
                    onClick={() => handlePrimaryNav(link.id)}
                  >
                    <span>{link.icon}</span>
                    <small>{link.label}</small>
                  </button>
                );
              })}
            </div>

          </aside>

          <div className="neo-main">
            <header className="neo-topbar">
              <div className="neo-top-tabs">
                {headerTabs.map((tab) => {
                  const isDisabled =
                    (tab.id === "tasks" || tab.id === "chat" || tab.id === "resources") &&
                    !selectedGroup;
                  const isActive = page === tab.id;

                  return (
                    <button
                      key={tab.id}
                      type="button"
                      className={`neo-top-tab ${isActive ? "is-active" : ""}`}
                      disabled={isDisabled}
                      onClick={() => handlePrimaryNav(tab.id)}
                    >
                      {tab.label}
                    </button>
                  );
                })}
              </div>

              <div className="neo-top-actions">
                <div className="neo-filter-chips">
                  {filters.map((f) => (
                    <button key={f.label} className="neo-chip" type="button">
                      <span>{f.label}:</span> <strong>{f.value}</strong>
                    </button>
                  ))}
                </div>

                <div className="neo-profile-card">
                  <div className="neo-avatar">
                    {user?.name?.[0]?.toUpperCase() || "U"}
                  </div>
                  <div className="neo-profile-text" onClick={openAccountSettings} style={{cursor:"pointer"}}>
                    <span className="neo-profile-name">{user?.name}</span>
                    <span className="neo-profile-handle">@study</span>
                  </div>
                  <button className="neo-logout" onClick={handleLogout}>
                    Logout
                  </button>
                </div>
              </div>
            </header>

            <main className="neo-content">
              <div key={viewKey} className="view-animate">
                {renderMainContent()}
              </div>
            </main>
          </div>
        </div>
      )}

      {showAccount && (
        <div className="modal-backdrop" onClick={() => setShowAccount(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Account settings</h3>
              <button className="neo-toast-close" onClick={() => setShowAccount(false)}>×</button>
            </div>

            <div className="modal-section">
              <div className="label">Email</div>
              <input
                className="input"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                disabled={accountBusy}
              />
              <button className="btn-primary" onClick={saveEmail} disabled={accountBusy}>
                Update email
              </button>
            </div>

            <div className="modal-section">
              <div className="label">Change password</div>
              <input
                className="input"
                type="password"
                placeholder="Current password"
                value={currentPass}
                onChange={(e) => setCurrentPass(e.target.value)}
                disabled={accountBusy}
              />
              <input
                className="input"
                type="password"
                placeholder="New password"
                value={newPass}
                onChange={(e) => setNewPass(e.target.value)}
                disabled={accountBusy}
              />
              <button className="btn-primary" onClick={savePassword} disabled={accountBusy}>
                Update password
              </button>
            </div>

            {accountError && <div className="error">{accountError}</div>}
            {accountMessage && <div className="success">{accountMessage}</div>}
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
