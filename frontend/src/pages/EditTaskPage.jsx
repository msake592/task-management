import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getProjects } from '../api/projectApi';
import { getTaskById, updateTask } from '../api/taskApi';
import { getApiErrorMessage } from '../utils/apiError';

const initialFormData = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  projectId: '',
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

function toFormData(task) {
  return {
    title: task?.title || '',
    description: task?.description || '',
    status: task?.status || 'TODO',
    priority: task?.priority || 'MEDIUM',
    dueDate: task?.dueDate || '',
    projectId: task?.projectId || task?.project?.id || '',
  };
}

function EditTaskPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [formData, setFormData] = useState(initialFormData);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const loadTask = async () => {
      try {
        setLoading(true);
        setLoadError('');
        setError('');
        const [taskData, projectsData] = await Promise.all([getTaskById(id), getProjects()]);
        setFormData(toFormData(taskData));
        setProjects(normalizeProjects(projectsData));
      } catch (err) {
        setLoadError(getApiErrorMessage(err, 'Task could not be loaded. Please check the backend response.'));
      } finally {
        setLoading(false);
      }
    };

    loadTask();
  }, [id]);

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

    const taskData = {
      title: formData.title.trim(),
      description: formData.description.trim(),
      status: formData.status,
      priority: formData.priority,
      dueDate: formData.dueDate || null,
      projectId: formData.projectId ? Number(formData.projectId) : null,
    };

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
            <input type="date" name="dueDate" value={formData.dueDate} onChange={handleChange} />
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

        {projects.length === 0 && <p className="empty-message">No projects available. Please create a project first.</p>}
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
