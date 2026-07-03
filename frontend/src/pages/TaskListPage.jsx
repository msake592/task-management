import { useEffect, useState } from 'react';
import { getProjects } from '../api/projectApi';
import { getTasks } from '../api/taskApi';
import { getUsers } from '../api/userApi';
import TaskCard from '../components/TaskCard.jsx';
import { isCurrentUserAdmin } from '../utils/authToken';

const initialFilters = {
  status: '',
  priority: '',
  projectId: '',
  assignedUserId: '',
};

const initialSort = {
  sortBy: 'createdAt',
  direction: 'desc',
};

const pageSize = 10;
const statusOptions = ['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function normalizeTasks(data) {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  return [];
}

function normalizeOptions(data) {
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

function buildTaskQuery(filters, sort, page, isAdmin) {
  return {
    page,
    size: pageSize,
    sortBy: sort.sortBy,
    direction: sort.direction,
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.priority ? { priority: filters.priority } : {}),
    ...(filters.projectId ? { projectId: Number(filters.projectId) } : {}),
    ...(isAdmin && filters.assignedUserId
      ? { assignedUserId: Number(filters.assignedUserId) }
      : {}),
  };
}

function TaskListPage() {
  const isAdmin = isCurrentUserAdmin();
  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [filters, setFilters] = useState(initialFilters);
  const [sort, setSort] = useState(initialSort);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [optionsError, setOptionsError] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const loadFilterOptions = async () => {
      const optionRequests = [getProjects()];

      if (isAdmin) {
        optionRequests.push(getUsers());
      }

      const [projectsResult, usersResult] = await Promise.allSettled(optionRequests);

      if (projectsResult.status === 'fulfilled') {
        setProjects(normalizeOptions(projectsResult.value));
      } else {
        setOptionsError('Filter options could not be fully loaded.');
      }

      if (isAdmin && usersResult?.status === 'fulfilled') {
        setUsers(normalizeOptions(usersResult.value));
      } else if (isAdmin && usersResult?.status === 'rejected') {
        setOptionsError('Filter options could not be fully loaded.');
      }
    };

    loadFilterOptions();
  }, [isAdmin]);

  useEffect(() => {
    const loadTasks = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await getTasks(buildTaskQuery(filters, sort, page, isAdmin));
        setTasks(normalizeTasks(data));
        setPageData(Array.isArray(data) ? null : data);
      } catch (err) {
        setError('Tasks could not be loaded. Please check that the backend is running.');
      } finally {
        setLoading(false);
      }
    };

    loadTasks();
  }, [filters, sort, page, isAdmin]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setPage(0);
    setFilters((currentFilters) => ({
      ...currentFilters,
      [name]: value,
    }));
  };

  const handleSortChange = (event) => {
    const { name, value } = event.target;
    setPage(0);
    setSort((currentSort) => ({
      ...currentSort,
      [name]: value,
    }));
  };

  const clearFilters = () => {
    setPage(0);
    setFilters(initialFilters);
    setSort(initialSort);
  };

  const totalPages = pageData?.totalPages || 0;
  const isFirstPage = pageData ? pageData.first : page === 0;
  const isLastPage = pageData ? pageData.last : true;

  if (loading) {
    return <section className="page">Loading tasks...</section>;
  }

  if (error) {
    return (
      <section className="page">
        <h1>Tasks</h1>
        <p className="error-message">{error}</p>
      </section>
    );
  }

  return (
    <section className="page">
      <h1>Tasks</h1>
      <div className="task-list-controls">
        <div className="form-row">
          <label className="form-field">
            <span>Status</span>
            <select name="status" value={filters.status} onChange={handleFilterChange}>
              <option value="">All statuses</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Priority</span>
            <select name="priority" value={filters.priority} onChange={handleFilterChange}>
              <option value="">All priorities</option>
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
            <span>Project</span>
            <select name="projectId" value={filters.projectId} onChange={handleFilterChange}>
              <option value="">All projects</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>

          {isAdmin && (
            <label className="form-field">
              <span>Assigned user</span>
              <select name="assignedUserId" value={filters.assignedUserId} onChange={handleFilterChange}>
                <option value="">All users</option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {getUserOptionLabel(user)}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>

        <div className="form-row">
          <label className="form-field">
            <span>Sort by</span>
            <select name="sortBy" value={sort.sortBy} onChange={handleSortChange}>
              <option value="createdAt">Created at</option>
              <option value="updatedAt">Updated at</option>
              <option value="dueDate">Due date</option>
              <option value="priority">Priority</option>
              <option value="status">Status</option>
              <option value="title">Title</option>
            </select>
          </label>

          <label className="form-field">
            <span>Direction</span>
            <select name="direction" value={sort.direction} onChange={handleSortChange}>
              <option value="desc">Desc</option>
              <option value="asc">Asc</option>
            </select>
          </label>
        </div>

        <button className="secondary-button" type="button" onClick={clearFilters}>
          Clear filters
        </button>
      </div>

      {optionsError && <p className="error-message">{optionsError}</p>}
      {tasks.length === 0 ? (
        <p>No tasks found.</p>
      ) : (
        <div className="task-list">
          {tasks.map((task, index) => (
            <TaskCard key={task?.id || index} task={task} />
          ))}
        </div>
      )}
      <div className="pagination-controls">
        <button className="secondary-button" type="button" onClick={() => setPage((currentPage) => Math.max(currentPage - 1, 0))} disabled={loading || isFirstPage}>
          Previous
        </button>
        <span>
          Page {page + 1}{totalPages ? ` of ${totalPages}` : ''}
        </span>
        <button className="secondary-button" type="button" onClick={() => setPage((currentPage) => currentPage + 1)} disabled={loading || isLastPage}>
          Next
        </button>
      </div>
    </section>
  );
}

export default TaskListPage;
