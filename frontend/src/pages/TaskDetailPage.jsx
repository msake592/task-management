import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteTask, getTaskById } from '../api/taskApi';

function getAssignedUser(task) {
  return task?.assignedUsername
    || task?.assignedUserFullName
    || task?.assignedUser?.email
    || task?.assignedUser?.username
    || 'Not assigned';
}

function TaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

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
          <strong>{task?.createdAt || 'Not available'}</strong>
        </div>
        <div className="detail-item">
          <span>Updated at</span>
          <strong>{task?.updatedAt || 'Not available'}</strong>
        </div>
      </div>
    </section>
  );
}

export default TaskDetailPage;
