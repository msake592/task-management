import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';

const initialFormData = {
  email: '',
  password: '',
};

function getTokenFromResponse(data) {
  return data?.token || data?.accessToken || data?.jwt || data?.data?.token || data?.data?.accessToken || '';
}

function LoginPage() {
  const [formData, setFormData] = useState(initialFormData);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      setLoading(true);
      setError('');
      const data = await login({
        email: formData.email.trim(),
        password: formData.password,
      });
      const token = getTokenFromResponse(data);

      if (!token) {
        setError('Login succeeded, but no token was returned.');
        return;
      }

      localStorage.setItem('token', token);
      window.dispatchEvent(new Event('authChanged'));
      navigate('/tasks');
    } catch (err) {
      setError('Login failed. Please check your credentials or backend response.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>Login</h1>
      <form className="task-form auth-form" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Email</span>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
            placeholder="Email"
          />
        </label>

        <label className="form-field">
          <span>Password</span>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            placeholder="Password"
          />
        </label>

        {error && <p className="error-message">{error}</p>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
          <Link className="secondary-button" to="/register">
            Register
          </Link>
        </div>
      </form>
    </section>
  );
}

export default LoginPage;
