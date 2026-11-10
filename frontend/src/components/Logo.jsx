import React from 'react';

export default function Logo({ className = '', onClick }) {
  return (
    <div
      onClick={onClick}
      className={`flex items-center gap-2.5 cursor-pointer select-none group ${className}`}
    >
      {/* Cinema Ticket Emblem Badge */}
      <div className="relative inline-flex items-center justify-center px-3.5 py-1.5 rounded-xl bg-gradient-to-br from-red-600 to-rose-700 shadow-md shadow-red-900/30 border border-red-500/50 group-hover:scale-105 transition-all">
        {/* Notch */}
        <div className="absolute -left-1 top-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-white" />
        <div className="absolute -right-1 top-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-white" />
        
        {/* Centered text */}
        <span className="font-black text-sm text-white tracking-widest leading-none">
          MTK
        </span>
      </div>

      {/* Brand title */}
      <div className="flex flex-col">
        <span className="text-base font-black tracking-tight text-slate-900 uppercase leading-none group-hover:text-red-600 transition-colors">
          CINEMAS
        </span>
        <span className="text-[9px] font-extrabold text-red-600 tracking-widest uppercase mt-0.5">
          VIETNAM
        </span>
      </div>
    </div>
  );
}