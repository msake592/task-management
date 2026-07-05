import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getProjectMembers, getProjects } from '../api/projectApi';
import { createTask } from '../api/taskApi';
import { getApiErrorMessage } from '../utils/apiError';
import { isTaskDeadlineWithinProject, TASK_DEADLINE_ERROR } from '../utils/deadlineValidation';

const initialFormData = {
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '',
  projectId: '',
  assigneeIds: [],
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

function getUserOptionLabel(user) {
  const fullName = user?.name || [user?.firstName, user?.lastName].filter(Boolean).join(' ');
  const email = user?.email || user?.username || `User #${user?.id}`;

  return fullName ? `${fullName} (${email})` : email;
}

function CreateTaskPage() {
  const [formData, setFormData] = useState(initialFormData);
  const [projects, setProjects] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [projectsError, setProjectsError] = useState('');
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [membersError, setMembersError] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const selectedProject = projects.find((project) => String(project.id) === formData.projectId);

  useEffect(() => {
    const loadFormOptions = async () => {
      try {
        setProjectsLoading(true);
        setProjectsError('');
        const result = await getProjects();
        setProjects(normalizeProjects(result));
      } catch (err) {
        setProjectsError(getApiErrorMessage(err, 'Projects could not be loaded.'));
      } finally {
        setProjectsLoading(false);
      }
    };

    loadFormOptions();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    if (name === 'projectId') {
      setMembers([]);
      setMembersError('');
      setFormData((currentData) => ({ ...currentData, projectId: value, assigneeIds: [] }));
      if (value) {
        setMembersLoading(true);
        getProjectMembers(value)
          .then(setMembers)
          .catch((err) => setMembersError(getApiErrorMessage(err, 'Project members could not be loaded.')))
          .finally(() => setMembersLoading(false));
      }
      return;
    }
    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleAssigneeChange = (event) => {
    const userId = Number(event.target.value);
    setFormData((currentData) => ({
      ...currentData,
      assigneeIds: event.target.checked
        ? [...currentData.assigneeIds, userId]
        : currentData.assigneeIds.filter((id) => id !== userId),
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
      ...formData,
      title: formData.title.trim(),
      description: formData.description.trim(),
      dueDate: formData.dueDate || null,
      projectId: formData.projectId ? Number(formData.projectId) : null,
      assigneeIds: formData.assigneeIds,
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

        <fieldset className="member-selector" disabled={!formData.projectId || membersLoading}>
          <legend>Assignees</legend>
          {!formData.projectId && <p className="field-help">Select a project first.</p>}
          {formData.projectId && membersLoading && <span>Loading project members...</span>}
          {formData.projectId && !membersLoading && members.length === 0 && (
            <p className="field-help">No project members are available.</p>
          )}
          {members.map((member) => (
            <label className="member-option" key={member.userId}>
              <input
                type="checkbox"
                value={member.userId}
                checked={formData.assigneeIds.includes(member.userId)}
                onChange={handleAssigneeChange}
              />
              <span>{getUserOptionLabel(member)} — {member.role}</span>
            </label>
          ))}
        </fieldset>

        {!projectsLoading && projects.length === 0 && !projectsError && (
          <p className="empty-message">
            No projects available. <Link to="/projects/new">Create a project first.</Link>
          </p>
        )}
        {projectsError && <p className="error-message">{projectsError}</p>}
        {membersError && <p className="error-message">{membersError}</p>}
        {error && <p className="error-message">{error}</p>}

        <button
          className="primary-button"
          type="submit"
          disabled={loading || projectsLoading || membersLoading || projects.length === 0}
        >
          {loading ? 'Creating...' : 'Create Task'}
        </button>
      </form>
    </section>
  );
}

export default CreateTaskPage;
