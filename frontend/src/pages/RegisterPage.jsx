import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/authApi';

const initialFormData = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  roleId: '',
};

function RegisterPage() {
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

    const userData = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      email: formData.email.trim(),
      password: formData.password,
      roleId: formData.roleId ? Number(formData.roleId) : null,
    };

    try {
      setLoading(true);
      setError('');
      await register(userData);
      navigate('/login');
    } catch (err) {
      setError('Registration failed. Please check the form or backend response.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="page">
      <h1>Register</h1>
      <form className="task-form auth-form" onSubmit={handleSubmit}>
        <div className="form-row">
          <label className="form-field">
            <span>First name</span>
            <input
              type="text"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              required
              placeholder="First name"
            />
          </label>

          <label className="form-field">
            <span>Last name</span>
            <input
              type="text"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              placeholder="Last name"
            />
          </label>
        </div>

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

        <label className="form-field">
          <span>Role ID</span>
          <input type="number" name="roleId" value={formData.roleId} onChange={handleChange} min="1" placeholder="Role ID" />
        </label>

        {error && <p className="error-message">{error}</p>}

        <div className="form-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Registering...' : 'Register'}
          </button>
          <Link className="secondary-button" to="/login">
            Login
          </Link>
        </div>
      </form>
    </section>
  );
}

export default RegisterPage;
