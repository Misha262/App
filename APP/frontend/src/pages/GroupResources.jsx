import { useEffect, useState, useCallback } from "react";
import { apiGet, apiUploadForm, apiDelete, apiDownload } from "../services/api";
import "../css/GroupResources.css";

export default function GroupResources({ group, goBack }) {
  const groupId =
    group?.groupId ||
    group?.id ||
    group?.group_id ||
    null;

  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  const [file, setFile] = useState(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  // ======================
  // Load resources
  // ======================
  const loadResources = useCallback(async () => {
    if (!groupId) return;

    setLoading(true);
    setError("");

    try {
      const data = await apiGet(`/api/resources?groupId=${groupId}`);

      if (Array.isArray(data)) setResources(data);
      else if (Array.isArray(data.content)) setResources(data.content);
      else setResources([]);
    } catch (e) {
      setError(e.message || "Failed to load resources");
      setResources([]);
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    loadResources();
  }, [loadResources]);

  // ======================
  // WebSocket auto-update
  // ======================
  useEffect(() => {
    if (!groupId) return;

    const ws = new WebSocket("ws://localhost:8080/ws/chat");

    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: "join",
        groupId: groupId,
        userId: 0,
        userName: "resourceWatcher"
      }));
    };

    ws.onmessage = (evt) => {
      let msg = {};
      try { msg = JSON.parse(evt.data); } catch { return; }

      if (msg.type === "EVENT") {
        if (msg.event === "RESOURCE_UPLOADED" ||
            msg.event === "RESOURCE_DELETED") {
          loadResources();
        }
      }
    };

    return () => ws.close();
  }, [groupId, loadResources]);

  // ======================
  // Upload file
  // ======================
  const handleUpload = async (e) => {
    e.preventDefault();
    setError("");
    setInfo("");

    if (!groupId) return setError("No group selected");
    if (!file) return setError("Choose a file first");

    const fd = new FormData();
    fd.append("file", file);

    if (title) fd.append("title", title);
    if (description) fd.append("description", description);

    setUploading(true);

    try {
      await apiUploadForm(`/api/resources?groupId=${groupId}`, fd);

      setFile(null);
      setTitle("");
      setDescription("");
      setInfo("File uploaded successfully");
      loadResources();
    } catch (e) {
      setError(e.message || "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // ======================
  // Delete
  // ======================
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this file?")) return;

    try {
      await apiDelete(`/api/resources/${id}`);
      setResources((prev) => prev.filter((r) => (r.id ?? r.resourceId) !== id));
      setInfo("File deleted");
    } catch {
      setError("Delete failed");
    }
  };

  const canManage =
    group?.role === "OWNER" ||
    group?.role === "ADMIN" ||
    group?.isOwner ||
    group?.isAdmin;

  const formatSize = (bytes) => {
    if (!bytes) return "--";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  const formatDate = (d) => {
    if (!d) return "--";
    const date = new Date(d);
    return isNaN(date) ? d : date.toLocaleString();
  };

  return (
    <div className="gr-container">
      <div className="gr-header">
        <button className="gr-back-btn" onClick={goBack}>← Back</button>

        <div className="gr-header-main">
          <h1 className="gr-title">
            Group resources {group?.name ? `: ${group.name}` : ""}
          </h1>
          <p className="gr-subtitle">Upload and manage documents.</p>
        </div>
      </div>

      {/* UPLOAD */}
      <section className="gr-upload-card">
        <h2 className="gr-section-title">Upload new file</h2>

        <form className="gr-upload-form" onSubmit={handleUpload}>
          <div className="gr-form-row">
            <label className="gr-label">File *</label>

            <input
              type="file"
              className="gr-input"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </div>

          <div className="gr-form-row">
            <label className="gr-label">Title</label>
            <input
              className="gr-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          <div className="gr-form-row">
            <label className="gr-label">Description</label>
            <textarea
              className="gr-textarea"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <button className="gr-upload-btn" disabled={uploading}>
            {uploading ? "Uploading..." : "Upload"}
          </button>
        </form>

        {error && <div className="gr-alert gr-alert-error">{error}</div>}
        {info && <div className="gr-alert gr-alert-success">{info}</div>}
      </section>

      {/* LIST */}
      <section className="gr-list-card">
        <div className="gr-list-header">
          <h2 className="gr-section-title">Files in this group</h2>

          {loading && <span className="gr-badge">Loading...</span>}
          {!loading && resources.length === 0 && (
            <span className="gr-badge gr-badge-muted">No files uploaded yet</span>
          )}
        </div>

        {resources.length > 0 && (
          <div className="gr-table">
            <div className="gr-table-head">
              <div>File</div>
              <div>Uploader</div>
              <div>Size</div>
              <div>Uploaded</div>
              <div>Actions</div>
            </div>

            <div className="gr-table-body">
              {resources.map((r) => {
                const id = r.id ?? r.resourceId;

                const name =
                  r.title ||
                  r.originalFilename ||
                  r.fileName ||
                  r.storedFilename ||
                  "Unnamed";

                const uploader =
                  r.uploadedByName ||
                  r.uploaderName ||
                  (r.user?.name ?? "Unknown");

                return (
                  <div className="gr-row" key={id}>
                    <div className="gr-col-name">
                      <span className="gr-file-icon">📄</span> {name}
                    </div>

                    <div className="gr-col-uploader">{uploader}</div>

                    <div className="gr-col-size">{formatSize(r.size || r.fileSize)}</div>

                    <div className="gr-col-date">
                      {formatDate(r.uploadedAt || r.createdAt)}
                    </div>

                    <div className="gr-col-actions">
                      <button
                        className="gr-btn gr-btn-outline"
                        onClick={() => apiDownload(`/api/resources/${id}/download`, name)}
                      >
                        Download
                      </button>

                      {canManage && (
                        <button
                          className="gr-btn gr-btn-danger"
                          onClick={() => handleDelete(id)}
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
