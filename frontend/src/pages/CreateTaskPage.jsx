import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProjects } from '../api/projectApi';
import { createTask } from '../api/taskApi';
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

function CreateTaskPage() {
  const [formData, setFormData] = useState(initialFormData);
  const [projects, setProjects] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [projectsError, setProjectsError] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const loadProjects = async () => {
      try {
        setProjectsLoading(true);
        setProjectsError('');
        const data = await getProjects();
        setProjects(normalizeProjects(data));
      } catch (err) {
        setProjectsError(getApiErrorMessage(err, 'Projects could not be loaded.'));
      } finally {
        setProjectsLoading(false);
      }
    };

    loadProjects();
  }, []);

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
      ...formData,
      title: formData.title.trim(),
      description: formData.description.trim(),
      dueDate: formData.dueDate || null,
      projectId: formData.projectId ? Number(formData.projectId) : null,
    };

    try {
      setLoading(true);
      setError('');
      await createTask(taskData);
      navigate('/tasks');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Task could not be created. Please check the form or backend response.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>Create Task</h1>
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
            <select
              name="projectId"
              value={formData.projectId}
              onChange={handleChange}
              disabled={projectsLoading || projects.length === 0}
              required
            >
              <option value="">{projectsLoading ? 'Loading projects...' : 'Select a project'}</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        {!projectsLoading && projects.length === 0 && !projectsError && (
          <p className="empty-message">No projects available. Please create a project first.</p>
        )}
        {projectsError && <p className="error-message">{projectsError}</p>}
        {error && <p className="error-message">{error}</p>}

        <button className="primary-button" type="submit" disabled={loading || projectsLoading || projects.length === 0}>
          {loading ? 'Creating...' : 'Create Task'}
        </button>
      </form>
    </section>
  );
}

export default CreateTaskPage;
