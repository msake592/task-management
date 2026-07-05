import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { addProjectMember, createProject } from '../api/projectApi';
import { getUsers } from '../api/userApi';
import { getApiErrorMessage } from '../utils/apiError';
import { getCurrentUserInfo } from '../utils/authToken';

const initialFormData = {
  name: '',
  description: '',
  startDate: '',
  endDate: '',
};

function CreateProjectPage() {
  const [formData, setFormData] = useState(initialFormData);
  const [users, setUsers] = useState([]);
  const [selectedMemberIds, setSelectedMemberIds] = useState([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [projectCreated, setProjectCreated] = useState(false);
  const navigate = useNavigate();
  const currentUserEmail = getCurrentUserInfo().username;

  useEffect(() => {
    const loadUsers = async () => {
      try {
        setUsersLoading(true);
        const data = await getUsers();
        setUsers(Array.isArray(data) ? data : data?.content || []);
      } catch (err) {
        setError(getApiErrorMessage(err, 'Users could not be loaded.'));
      } finally {
        setUsersLoading(false);
      }
    };

    loadUsers();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleMemberChange = (event) => {
    const userId = Number(event.target.value);
    setSelectedMemberIds((currentIds) => event.target.checked
      ? [...currentIds, userId]
      : currentIds.filter((id) => id !== userId));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.name.trim()) {
      setError('Project name is required.');
      return;
    }

    const projectData = {
      name: formData.name.trim(),
      description: formData.description.trim(),
      startDate: formData.startDate || null,
      endDate: formData.endDate || null,
    };

    try {
      setLoading(true);
      setError('');
      const project = await createProject(projectData);
      setProjectCreated(true);
      const memberResults = await Promise.allSettled(selectedMemberIds.map((userId) =>
        addProjectMember(project.id, { userId, role: 'MEMBER' })));
      const failedMemberCount = memberResults.filter((result) => result.status === 'rejected').length;

      if (failedMemberCount > 0) {
        setError(`Project created, but ${failedMemberCount} member(s) could not be added.`);
        return;
      }
      navigate('/projects');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Project could not be created.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>New Project</h1>
      <form className="task-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Name</span>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            placeholder="Project name"
          />
        </label>

        <label className="form-field">
          <span>Description</span>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Project description"
          />
        </label>

        <div className="form-row">
          <label className="form-field">
            <span>Start date</span>
            <input type="date" name="startDate" value={formData.startDate} onChange={handleChange} />
          </label>

          <label className="form-field">
            <span>End date</span>
            <input type="date" name="endDate" value={formData.endDate} onChange={handleChange} />
          </label>
        </div>

        <fieldset className="member-selector" disabled={usersLoading || projectCreated}>
          <legend>Project Members</legend>
          <p className="field-help">The project owner is added automatically. Selecting additional members is optional.</p>
          {usersLoading ? (
            <span>Loading users...</span>
          ) : (
            users.map((user) => {
              const isOwner = user.email === currentUserEmail;
              const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ');
              return (
                <label className="member-option" key={user.id}>
                  <input
                    type="checkbox"
                    value={user.id}
                    checked={isOwner || selectedMemberIds.includes(user.id)}
                    disabled={isOwner || projectCreated}
                    onChange={handleMemberChange}
                  />
                  <span>{fullName || user.email} ({user.email}){isOwner ? ' — Owner' : ''}</span>
                </label>
              );
            })
          )}
        </fieldset>

        {error && <p className="error-message">{error}</p>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={loading || usersLoading || projectCreated}>
            {loading ? 'Creating...' : 'Create Project'}
          </button>
          <Link className="secondary-button" to="/projects">
            Cancel
          </Link>
        </div>
      </form>
    </section>
  );
}

export default CreateProjectPage;
