import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

export default function PageTransitionLoader() {
  const location = useLocation();
  const [progress, setProgress] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Scroll to top immediately on navigation
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });

    setLoading(true);
    setProgress(20);

    const step1 = setTimeout(() => setProgress(55), 80);
    const step2 = setTimeout(() => setProgress(90), 200);
    const step3 = setTimeout(() => setProgress(100), 320);
    const step4 = setTimeout(() => {
      setLoading(false);
      setProgress(0);
    }, 500);

    return () => {
      clearTimeout(step1);
      clearTimeout(step2);
      clearTimeout(step3);
      clearTimeout(step4);
    };
  }, [location.pathname, location.search]);

  if (!loading && progress === 0) return null;

  return (
    <>
      {/* 1. Top Red Glowing Loading Bar (YouTube / GitHub style) */}
      <div className="fixed top-0 left-0 right-0 z-50 h-[3px] pointer-events-none overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-red-600 via-rose-500 to-red-600 shadow-[0_0_12px_#dc2626] transition-all ease-out"
          style={{
            width: `${progress}%`,
            transitionDuration: progress === 100 ? '150ms' : '220ms',
            opacity: progress === 100 ? 0 : 1,
          }}
        />
      </div>

      {/* 2. Sleek Floating Indicator on top-right */}
      {loading && progress < 100 && (
        <div className="fixed top-3 right-4 z-50 pointer-events-none flex items-center gap-2 bg-slate-900/85 backdrop-blur-md text-white px-3 py-1 rounded-full shadow-xl text-xs font-semibold animate-in fade-in duration-150 border border-slate-700/60">
          <div className="w-3.5 h-3.5 border-2 border-red-500 border-t-transparent rounded-full animate-spin" />
          <span>Đang chuyển trang...</span>
        </div>
      )}
    </>
  );
}
