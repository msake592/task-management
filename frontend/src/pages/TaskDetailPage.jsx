import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { addComment, getCommentsByTask } from '../api/commentApi';
import { deleteTask, getTaskById } from '../api/taskApi';
import {
  downloadTaskAttachment,
  getTaskAttachments,
  uploadTaskAttachment,
} from '../api/taskAttachmentApi';
import { getApiErrorMessage } from '../utils/apiError';

function getAssignedUser(task) {
  return task?.assignedUsername
    || task?.assignedUserFullName
    || task?.assignedUser?.email
    || task?.assignedUser?.username
    || 'Not assigned';
}

function readJsonStorage(key) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch (error) {
    return null;
  }
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1];

    if (!payload) {
      return null;
    }

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decodedPayload = window.atob(normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '='));
    return JSON.parse(decodedPayload);
  } catch (error) {
    return null;
  }
}

function getCurrentUserId() {
  const directUserId = localStorage.getItem('userId');

  if (directUserId) {
    return directUserId;
  }

  const currentUser = readJsonStorage('currentUser') || readJsonStorage('user') || readJsonStorage('authUser');
  const storedUserId = currentUser?.id || currentUser?.userId || currentUser?.data?.id || currentUser?.data?.userId;

  if (storedUserId) {
    return storedUserId;
  }

  const tokenPayload = decodeJwtPayload(localStorage.getItem('token') || '');
  return tokenPayload?.userId || tokenPayload?.id || '';
}

function formatCommentDate(value) {
  if (!value) {
    return 'Not available';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString('tr-TR');
}

function formatDateTime(value) {
  if (!value) {
    return 'Not available';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString('tr-TR');
}

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return 'Unknown size';
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function TaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentsError, setCommentsError] = useState('');
  const [commentContent, setCommentContent] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);
  const [attachmentError, setAttachmentError] = useState('');
  const [attachmentSuccess, setAttachmentSuccess] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const [downloadingAttachmentId, setDownloadingAttachmentId] = useState(null);

  useEffect(() => {
    const loadTask = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getTaskById(id);
        setTask(data);
      } catch (err) {
        setError('Task details could not be loaded. Please check the backend response.');
      } finally {
        setLoading(false);
      }
    };

    loadTask();
  }, [id]);

  useEffect(() => {
    const loadComments = async () => {
      try {
        setCommentsLoading(true);
        setCommentsError('');
        const data = await getCommentsByTask(id);
        setComments(Array.isArray(data) ? data : []);
      } catch (err) {
        setCommentsError('Comments could not be loaded.');
      } finally {
        setCommentsLoading(false);
      }
    };

    loadComments();
  }, [id]);

  useEffect(() => {
    const loadAttachments = async () => {
      try {
        setAttachmentsLoading(true);
        setAttachmentError('');
        const data = await getTaskAttachments(id);
        setAttachments(Array.isArray(data) ? data : []);
      } catch (err) {
        setAttachmentError(getApiErrorMessage(err, 'Attachments could not be loaded.'));
      } finally {
        setAttachmentsLoading(false);
      }
    };

    loadAttachments();
  }, [id]);

  const handleCommentSubmit = async (event) => {
    event.preventDefault();

    const trimmedContent = commentContent.trim();

    if (!trimmedContent) {
      setCommentsError('Empty comments cannot be submitted.');
      return;
    }

    const userId = getCurrentUserId();

    if (!userId) {
      setCommentsError('User information not found.');
      return;
    }

    try {
      setCommentSubmitting(true);
      setCommentsError('');
      const newComment = await addComment(id, userId, trimmedContent);
      setComments((currentComments) => [newComment, ...currentComments]);
      setCommentContent('');
    } catch (err) {
      setCommentsError('Comment could not be added.');
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleDelete = async () => {
    const confirmed = window.confirm('Are you sure you want to delete this task?');

    if (!confirmed) {
      return;
    }

    try {
      setDeleting(true);
      setError('');
      await deleteTask(id);
      navigate('/tasks');
    } catch (err) {
      setError('Task could not be deleted. Please check the backend response.');
    } finally {
      setDeleting(false);
    }
  };

  const handleAttachmentUpload = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;

    if (!selectedFile) {
      setAttachmentError('Select a file to upload.');
      return;
    }

    try {
      setUploadingAttachment(true);
      setAttachmentError('');
      setAttachmentSuccess('');
      const uploadedAttachment = await uploadTaskAttachment(id, selectedFile);
      setAttachments((currentAttachments) => [uploadedAttachment, ...currentAttachments]);
      setSelectedFile(null);
      form.reset();
      setAttachmentSuccess('Attachment uploaded successfully.');
    } catch (err) {
      setAttachmentError(getApiErrorMessage(err, 'Attachment could not be uploaded.'));
    } finally {
      setUploadingAttachment(false);
    }
  };

  const handleAttachmentDownload = async (attachment) => {
    try {
      setDownloadingAttachmentId(attachment.id);
      setAttachmentError('');
      setAttachmentSuccess('');
      const blob = await downloadTaskAttachment(id, attachment.id);
      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = attachment.originalFileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(objectUrl);
    } catch (err) {
      setAttachmentError(getApiErrorMessage(err, 'Attachment could not be downloaded.'));
    } finally {
      setDownloadingAttachmentId(null);
    }
  };

  if (loading) {
    return <section className="page">Loading task details...</section>;
  }

  if (error) {
    return (
      <section className="page">
        <h1>Task Details</h1>
        <p className="error-message">{error}</p>
        <Link className="secondary-button" to="/tasks">
          Back to Tasks
        </Link>
      </section>
    );
  }

  if (!task) {
    return (
      <section className="page">
        <h1>Task Details</h1>
        <p>No task found.</p>
        <Link className="secondary-button" to="/tasks">
          Back to Tasks
        </Link>
      </section>
    );
  }

  return (
    <section className="page">
      <div className="detail-header">
        <div>
          <h1>{task?.title || 'Untitled task'}</h1>
          <p>{task?.description || 'No description'}</p>
        </div>
        <div className="detail-actions">
          <Link className="primary-button" to={`/tasks/${id}/edit`}>
            Edit Task
          </Link>
          <button className="danger-button" type="button" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Deleting...' : 'Delete Task'}
          </button>
          <Link className="secondary-button" to="/tasks">
            Back to Tasks
          </Link>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="detail-panel">
        <div className="detail-item">
          <span>Status</span>
          <strong>{task?.status || 'No status'}</strong>
        </div>
        <div className="detail-item">
          <span>Priority</span>
          <strong>{task?.priority || 'No priority'}</strong>
        </div>
        <div className="detail-item">
          <span>Due date</span>
          <strong>{task?.dueDate || 'No due date'}</strong>
        </div>
        <div className="detail-item">
          <span>Project</span>
          <strong>{task?.projectName || task?.project?.name || task?.projectId || task?.project?.id || 'No project'}</strong>
        </div>
        <div className="detail-item">
          <span>Assigned user</span>
          <strong>{getAssignedUser(task)}</strong>
        </div>
        <div className="detail-item">
          <span>Created at</span>
          <strong>{formatDateTime(task?.createdAt)}</strong>
        </div>
        <div className="detail-item">
          <span>Updated at</span>
          <strong>{formatDateTime(task?.updatedAt)}</strong>
        </div>
      </div>

      <div className="attachments-panel">
        <div className="attachments-header">
          <h2>Attachments</h2>
          {attachmentsLoading && <span>Loading...</span>}
        </div>

        <form className="attachment-form" onSubmit={handleAttachmentUpload}>
          <label className="form-field">
            <span>File</span>
            <input
              type="file"
              name="file"
              onChange={(event) => {
                setSelectedFile(event.target.files?.[0] || null);
                setAttachmentError('');
                setAttachmentSuccess('');
              }}
            />
          </label>
          <button
            className="primary-button"
            type="submit"
            disabled={uploadingAttachment || !selectedFile}
          >
            {uploadingAttachment ? 'Uploading...' : 'Upload Attachment'}
          </button>
        </form>

        {attachmentSuccess && <p className="success-message">{attachmentSuccess}</p>}
        {attachmentError && <p className="error-message">{attachmentError}</p>}

        <div className="attachment-list">
          {attachments.length === 0 && !attachmentsLoading ? (
            <p className="empty-message">No attachments yet.</p>
          ) : (
            attachments.map((attachment) => (
              <article className="attachment-item" key={attachment.id}>
                <div>
                  <strong>{attachment.originalFileName}</strong>
                  <span>
                    {formatFileSize(attachment.fileSize)}
                    {' · '}
                    {attachment.uploadedByUsername || 'Unknown user'}
                    {' · '}
                    {formatDateTime(attachment.uploadedAt)}
                  </span>
                </div>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => handleAttachmentDownload(attachment)}
                  disabled={downloadingAttachmentId === attachment.id}
                >
                  {downloadingAttachmentId === attachment.id ? 'Downloading...' : 'Download'}
                </button>
              </article>
            ))
          )}
        </div>
      </div>

      <div className="comments-panel">
        <div className="comments-header">
          <h2>Comments</h2>
          {commentsLoading && <span>Loading...</span>}
        </div>

        <form className="comment-form" onSubmit={handleCommentSubmit}>
          <label className="form-field">
            <span>Comment</span>
            <textarea
              name="comment"
              value={commentContent}
              onChange={(event) => setCommentContent(event.target.value)}
              maxLength={1000}
              rows="4"
              placeholder="Write your comment"
            />
          </label>
          <div className="comment-form-footer">
            <span>{commentContent.length}/1000</span>
            <button
              className="primary-button"
              type="submit"
              disabled={commentSubmitting || !commentContent.trim()}
            >
              {commentSubmitting ? 'Adding...' : 'Add Comment'}
            </button>
          </div>
        </form>

        {commentsError && <p className="error-message">{commentsError}</p>}

        <div className="comment-list">
          {comments.length === 0 && !commentsLoading ? (
            <p className="empty-message">No comments yet.</p>
          ) : (
            comments.map((comment) => (
              <article className="comment-item" key={comment.id}>
                <div className="comment-meta">
                  <strong>{comment.username || comment.email || 'Unknown user'}</strong>
                  <span>{formatCommentDate(comment.createdAt)}</span>
                </div>
                <p>{comment.content}</p>
              </article>
            ))
          )}
        </div>
      </div>
    </section>
  );
}

export default TaskDetailPage;
