/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
        navy: {
          950: '#070b14',
          900: '#0b1120',
          850: '#0e172a',
          800: '#131d33',
          750: '#17233f',
          700: '#1e2d4f',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'Courier New', 'monospace'],
      },
      boxShadow: {
        'glow-sm': '0 0 15px rgba(59, 130, 246, 0.15)',
        'glow': '0 0 25px rgba(59, 130, 246, 0.25)',
      },
    },
  },
  plugins: [],
}
