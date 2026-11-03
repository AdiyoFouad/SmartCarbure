import { render, screen } from '@testing-library/react';
import App from './App';

test('affiche le titre de l\'application', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: 'SmartCarbure' })).toBeInTheDocument();
});
