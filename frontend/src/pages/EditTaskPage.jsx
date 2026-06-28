import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getProjects } from '../api/projectApi';
import { getTaskById, updateTask } from '../api/taskApi';
import { getUsers } from '../api/userApi';
import { getApiErrorMessage } from '../utils/apiError';
import { isCurrentUserAdmin } from '../utils/authToken';
import { isTaskDeadlineWithinProject, TASK_DEADLINE_ERROR } from '../utils/deadlineValidation';

const initialFormData = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  projectId: '',
  assignedUserId: '',
};

const statusOptions = ['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function normalizeProjects(data) {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  return [];
}

function normalizeUsers(data) {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  return [];
}

function getUserOptionLabel(user) {
  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
  const email = user?.email || user?.username || `User #${user?.id}`;

  return fullName ? `${fullName} (${email})` : email;
}

function toFormData(task) {
  return {
    title: task?.title || '',
    description: task?.description || '',
    status: task?.status || 'TODO',
    priority: task?.priority || 'MEDIUM',
    dueDate: task?.dueDate || '',
    projectId: task?.projectId || task?.project?.id || '',
    assignedUserId: task?.assignedUserId || task?.assignedUser?.id || '',
  };
}

function EditTaskPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const canAssignTasks = isCurrentUserAdmin();
  const [formData, setFormData] = useState(initialFormData);
  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [usersError, setUsersError] = useState('');
  const [error, setError] = useState('');
  const selectedProject = projects.find((project) => String(project.id) === String(formData.projectId));

  useEffect(() => {
    const loadTask = async () => {
      try {
        setLoading(true);
        setLoadError('');
        setUsersError('');
        setError('');
        const requests = canAssignTasks ? [getTaskById(id), getProjects(), getUsers()] : [getTaskById(id), getProjects()];
        const [taskResult, projectsResult, usersResult] = await Promise.allSettled(requests);

        if (taskResult.status === 'rejected') {
          setLoadError(getApiErrorMessage(taskResult.reason, 'Task could not be loaded. Please check the backend response.'));
          return;
        }

        if (projectsResult.status === 'rejected') {
          setLoadError(getApiErrorMessage(projectsResult.reason, 'Projects could not be loaded.'));
          return;
        }

        setFormData(toFormData(taskResult.value));
        setProjects(normalizeProjects(projectsResult.value));

        if (!canAssignTasks) {
          setUsers([]);
        } else if (usersResult.status === 'fulfilled') {
          setUsers(normalizeUsers(usersResult.value));
        } else {
          setUsersError(getApiErrorMessage(usersResult.reason, 'Users could not be loaded.'));
        }
      } catch (err) {
        setLoadError(getApiErrorMessage(err, 'Task could not be loaded. Please check the backend response.'));
      } finally {
        setLoading(false);
      }
    };

    loadTask();
  }, [id, canAssignTasks]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.title.trim()) {
      setError('Title is required.');
      return;
    }

    if (!formData.projectId) {
      setError('Please select a project.');
      return;
    }

    if (!isTaskDeadlineWithinProject(selectedProject, formData.dueDate)) {
      setError(TASK_DEADLINE_ERROR);
      return;
    }

    const taskData = {
      title: formData.title.trim(),
      description: formData.description.trim(),
      status: formData.status,
      priority: formData.priority,
      dueDate: formData.dueDate || null,
      projectId: formData.projectId ? Number(formData.projectId) : null,
    };

    if (canAssignTasks) {
      taskData.assignedUserId = formData.assignedUserId ? Number(formData.assignedUserId) : null;
    }

    try {
      setSubmitting(true);
      setError('');
      await updateTask(id, taskData);
      navigate(`/tasks/${id}`);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Task could not be updated. Please check the form or backend response.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <section className="page">Loading task...</section>;
  }

  if (loadError) {
    return (
      <section className="page">
        <h1>Edit Task</h1>
        <p className="error-message">{loadError}</p>
        <Link className="secondary-button" to={`/tasks/${id}`}>
          Back to Details
        </Link>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Edit Task</h1>
      <form className="task-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Title</span>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
            placeholder="Task title"
          />
        </label>

        <label className="form-field">
          <span>Description</span>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Task description"
          />
        </label>

        <div className="form-row">
          <label className="form-field">
            <span>Status</span>
            <select name="status" value={formData.status} onChange={handleChange}>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Priority</span>
            <select name="priority" value={formData.priority} onChange={handleChange}>
              {priorityOptions.map((priority) => (
                <option key={priority} value={priority}>
                  {priority}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="form-row">
          <label className="form-field">
            <span>Due date</span>
            <input
              type="date"
              name="dueDate"
              value={formData.dueDate}
              min={selectedProject?.startDate || undefined}
              max={selectedProject?.endDate || undefined}
              onChange={handleChange}
            />
          </label>

          <label className="form-field">
            <span>Project</span>
            <select name="projectId" value={formData.projectId} onChange={handleChange} disabled={projects.length === 0} required>
              <option value="">Select a project</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        {canAssignTasks && (
          <label className="form-field">
            <span>Assigned user</span>
            <select name="assignedUserId" value={formData.assignedUserId} onChange={handleChange}>
              <option value="">Not assigned</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {getUserOptionLabel(user)}
                </option>
              ))}
            </select>
          </label>
        )}

        {projects.length === 0 && (
          <p className="empty-message">
            No projects available. <Link to="/projects/new">Create a project first.</Link>
          </p>
        )}
        {canAssignTasks && usersError && <p className="error-message">{usersError}</p>}
        {error && <p className="error-message">{error}</p>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={submitting || projects.length === 0}>
            {submitting ? 'Saving...' : 'Save Changes'}
          </button>
          <Link className="secondary-button" to={`/tasks/${id}`}>
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}

export default EditTaskPage;
