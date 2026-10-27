module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}', './index.html'],
  theme: {
    extend: {
      colors: {
        primary: 'var(--color-primary)',
        secondary: 'var(--color-secondary)',
        background: 'var(--color-background)',
        surface: 'var(--color-surface)',
        text: 'var(--color-text)',
        'text-secondary': 'var(--color-text-secondary)',
        border: 'var(--color-border)',
        hover: 'var(--color-hover)',
        mark: 'var(--color-mark)',
        danger: 'var(--color-danger)',
      },
      borderRadius: {
        xl: '1.375rem',
        '2xl': '1.75rem',
      },
      fontSize: {
        display: ['2.75rem', { lineHeight: '1.05', letterSpacing: '-0.03em', fontWeight: '700' }],
        hero: ['2rem', { lineHeight: '1.12', letterSpacing: '-0.025em', fontWeight: '700' }],
      },
      transitionDuration: {
        DEFAULT: '180ms',
      },
    },
  },
  plugins: [],
};
