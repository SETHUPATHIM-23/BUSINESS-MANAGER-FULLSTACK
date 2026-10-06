import React, { createContext, useContext, useState, useEffect } from 'react';

export type Theme = 'light' | 'dark';
export type ThemeColor = 'blue' | 'emerald' | 'violet' | 'rose' | 'slate' | 'amber';

export const THEME_COLORS: Record<ThemeColor, { name: string, primary: string, secondary: string, gradient: string }> = {
  blue: {
    name: 'Royal Blue',
    primary: '#2563eb',
    secondary: '#3b82f6',
    gradient: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)'
  },
  emerald: {
    name: 'Emerald Green',
    primary: '#059669',
    secondary: '#10b981',
    gradient: 'linear-gradient(135deg, #059669 0%, #047857 100%)'
  },
  violet: {
    name: 'Deep Violet',
    primary: '#7c3aed',
    secondary: '#8b5cf6',
    gradient: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)'
  },
  rose: {
    name: 'Rose Red',
    primary: '#e11d48',
    secondary: '#f43f5e',
    gradient: 'linear-gradient(135deg, #e11d48 0%, #be123c 100%)'
  },
  slate: {
    name: 'Slate Gray',
    primary: '#475569',
    secondary: '#64748b',
    gradient: 'linear-gradient(135deg, #475569 0%, #334155 100%)'
  },
  amber: {
    name: 'Amber Warning',
    primary: '#d97706',
    secondary: '#f59e0b',
    gradient: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)'
  }
};

interface ThemeContextType {
  theme: Theme;
  themeColor: ThemeColor;
  toggleTheme: () => void;
  setThemeColor: (color: ThemeColor) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem('bm_theme') as Theme;
    return saved || 'light';
  });

  const [themeColor, setThemeColorState] = useState<ThemeColor>(() => {
    const saved = localStorage.getItem('bm_theme_color') as ThemeColor;
    return THEME_COLORS[saved] ? saved : 'blue';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('bm_theme', theme);
  }, [theme]);

  useEffect(() => {
    const colorTheme = THEME_COLORS[themeColor];
    if (colorTheme) {
      document.documentElement.style.setProperty('--accent-primary', colorTheme.primary);
      document.documentElement.style.setProperty('--accent-secondary', colorTheme.secondary);
      document.documentElement.style.setProperty('--accent-gradient', colorTheme.gradient);
      localStorage.setItem('bm_theme_color', themeColor);
    }
  }, [themeColor]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  const setThemeColor = (color: ThemeColor) => {
    if (THEME_COLORS[color]) {
      setThemeColorState(color);
    }
  };

  return (
    <ThemeContext.Provider value={{ theme, themeColor, toggleTheme, setThemeColor }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
