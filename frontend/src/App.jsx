import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import RouteProtegee from './components/RouteProtegee';
import Accueil from './pages/Accueil';
import Resultats from './pages/Resultats';
import FicheStation from './pages/FicheStation';
import Trajet from './pages/Trajet';
import Comparer from './pages/Comparer';
import Connexion from './pages/Connexion';
import Favoris from './pages/Favoris';
import Preferences from './pages/Preferences';
import PageIntrouvable from './pages/PageIntrouvable';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main className="contenu-principal conteneur">
          <Routes>
            <Route path="/" element={<Accueil />} />
            <Route path="/resultats" element={<Resultats />} />
            <Route path="/stations/:id" element={<FicheStation />} />
            <Route path="/trajet" element={<Trajet />} />
            <Route path="/comparer" element={<Comparer />} />
            <Route path="/connexion" element={<Connexion />} />
            <Route
              path="/favoris"
              element={
                <RouteProtegee>
                  <Favoris />
                </RouteProtegee>
              }
            />
            <Route
              path="/preferences"
              element={
                <RouteProtegee>
                  <Preferences />
                </RouteProtegee>
              }
            />
            <Route path="*" element={<PageIntrouvable />} />
          </Routes>
        </main>
        <Footer />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
