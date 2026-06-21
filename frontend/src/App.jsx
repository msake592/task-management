import Navbar from './components/Navbar.jsx';
import AppRoutes from './routes/AppRoutes.jsx';

function App() {
  return (
    <div className="app">
      <Navbar />
      <main className="main-content">
        <AppRoutes />
      </main>
    </div>
  );
}

export default App;
