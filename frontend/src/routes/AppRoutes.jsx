import { Route, Routes } from 'react-router-dom';
import HomePage from '../pages/HomePage.jsx';
import CreateTaskPage from '../pages/CreateTaskPage.jsx';
import EditTaskPage from '../pages/EditTaskPage.jsx';
import LoginPage from '../pages/LoginPage.jsx';
import RegisterPage from '../pages/RegisterPage.jsx';
import TaskDetailPage from '../pages/TaskDetailPage.jsx';
import TaskListPage from '../pages/TaskListPage.jsx';

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/tasks" element={<TaskListPage />} />
      <Route path="/tasks/new" element={<CreateTaskPage />} />
      <Route path="/tasks/:id/edit" element={<EditTaskPage />} />
      <Route path="/tasks/:id" element={<TaskDetailPage />} />
    </Routes>
  );
}

export default AppRoutes;
