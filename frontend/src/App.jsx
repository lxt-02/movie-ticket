import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import MtkFooter from './components/MtkFooter';
import PageTransitionLoader from './components/PageTransitionLoader';

// Pages
import HomePage from './pages/HomePage';
import NowShowingPage from './pages/NowShowingPage';
import MovieDetailPage from './pages/MovieDetailPage';
import CinemasPage from './pages/CinemasPage';
import BookingPage from './pages/BookingPage';
import PaymentPage from './pages/PaymentPage';
import MyTicketsPage from './pages/MyTicketsPage';
import ProfilePage from './pages/ProfilePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AboutPage from './pages/AboutPage';
import FeaturesPage from './pages/FeaturesPage';
import TermsPage from './pages/TermsPage';
import StaffDashboardPage from './pages/StaffDashboardPage';
import AdminDashboardPage from './pages/AdminDashboardPage';

import {
  getMovies,
  searchMovies,
  getCurrentUserProfile,
  isAuthenticated
} from './api';

function AppLayout({ children, onSearch, currentUserProfile }) {
  const location = useLocation();
  const isDashboard = location.pathname.startsWith('/admin') || location.pathname.startsWith('/staff');

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans antialiased selection:bg-red-500 selection:text-white">
      {/* Top Loading Progress Bar across all page transitions */}
      <PageTransitionLoader />

      {/* Do not render standard cinema navbar on dedicated Staff and Admin Portals */}
      {!isDashboard && (
        <Navbar onSearch={onSearch} currentUserProfile={currentUserProfile} />
      )}

      <main className="flex-1">
        {children}
      </main>

      {/* Do not render customer footer on Staff and Admin portals */}
      {!isDashboard && <MtkFooter />}
    </div>
  );
}

export default function App() {
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentUserProfile, setCurrentUserProfile] = useState(null);

  const loadUserProfile = async () => {
    try {
      const p = await getCurrentUserProfile();
      setCurrentUserProfile(p);
    } catch (e) {
      console.warn('Profile fetch note:', e);
      setCurrentUserProfile(null);
    }
  };

  const loadMovies = async () => {
    setLoading(true);
    try {
      const data = await getMovies('NOW_SHOWING');
      const list = Array.isArray(data) ? data : data?.content || [];
      setMovies(list);
    } catch (e) {
      console.error('Error fetching movies:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMovies();
    if (isAuthenticated()) {
      loadUserProfile();
    }
  }, []);

  const handleSearch = async (keyword) => {
    if (!keyword || !keyword.trim()) {
      loadMovies();
      return;
    }
    setLoading(true);
    try {
      const data = await searchMovies(keyword);
      const list = Array.isArray(data) ? data : data?.content || [];
      setMovies(list);
    } catch (e) {
      console.error('Search error:', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <BrowserRouter>
      <AppLayout onSearch={handleSearch} currentUserProfile={currentUserProfile}>
        <Routes>
          {/* 1. Public & Customer Routes */}
          <Route path="/" element={<HomePage movies={movies} loading={loading} />} />
          <Route path="/movies" element={<NowShowingPage movies={movies} />} />
          <Route path="/movies/:id" element={<MovieDetailPage />} />
          <Route path="/cinemas" element={<CinemasPage />} />
          <Route path="/booking/:showtimeId" element={<BookingPage currentUserProfile={currentUserProfile} />} />
          <Route path="/payment/:bookingId" element={<PaymentPage />} />
          <Route path="/my-tickets" element={<MyTicketsPage currentUserProfile={currentUserProfile} />} />
          <Route
            path="/profile"
            element={
              <ProfilePage
                currentUserProfile={currentUserProfile}
                onProfileUpdated={loadUserProfile}
              />
            }
          />
          <Route path="/login" element={<LoginPage onLoginSuccess={loadUserProfile} />} />
          <Route path="/register" element={<RegisterPage onRegisterSuccess={loadUserProfile} />} />

          {/* 2. Static Information Pages */}
          <Route path="/about" element={<AboutPage />} />
          <Route path="/features" element={<FeaturesPage />} />
          <Route path="/terms" element={<TermsPage />} />

          {/* 3. Dedicated Staff Dashboard Route */}
          <Route
            path="/staff/dashboard"
            element={<StaffDashboardPage currentUserProfile={currentUserProfile} />}
          />

          {/* 4. Dedicated Admin Dashboard Route */}
          <Route
            path="/admin/dashboard"
            element={<AdminDashboardPage currentUserProfile={currentUserProfile} />}
          />

          {/* Fallback to Home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
