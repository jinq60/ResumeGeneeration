/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#fbf9f5',
        'paper-dim': '#dbdad6',
        ink: '#0f0f0e',
        stone: '#ede9e3',
        line: '#eae8e3',
        graphite: '#8a8a87',
        vermilion: '#ff3b1f',
        surface: '#fbf9f5',
        'surface-dim': '#dbdad6',
        'surface-container': '#efeeea',
        'surface-high': '#e9e8e4',
        outline: '#777871',
        'outline-variant': '#c7c7c0',
      },
      fontFamily: {
        newsreader: ['Newsreader', 'serif'],
        inter: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        sm: '0.25rem',
        DEFAULT: '0.5rem',
        md: '0.75rem',
        lg: '1rem',
        xl: '1.5rem',
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '12px',
        lg: '16px',
        xl: '24px',
        '2xl': '32px',
        '3xl': '48px',
        '4xl': '64px',
      },
      boxShadow: {
        paper: '0px 4px 24px rgba(0,0,0,0.04), 0px 2px 8px rgba(0,0,0,0.02)',
        xl: '0px 8px 32px rgba(0,0,0,0.08)',
      },
    },
  },
  plugins: [],
}
