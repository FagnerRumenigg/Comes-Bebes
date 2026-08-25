export const mockAccounts = [
  {
    userId: '71131447-a2a0-4996-a336-a8c3555bb327',
    displayName: 'Fagner',
    username: 'fagner',
    email: 'fagner@exemplo.com.br',
    password: 'MinhaSenha123!',
    role: 'USER' as const,
  },
  {
    userId: 'b5d94b7e-3c45-4d28-99fe-70b96065b6c4',
    displayName: 'Administração',
    username: 'admin',
    email: 'admin@exemplo.com.br',
    password: 'AdminSenha123!',
    role: 'ADMIN' as const,
  },
]

export const mockCredentials = {
  user: {
    email: 'fagner@exemplo.com.br',
    password: 'MinhaSenha123!',
  },
  admin: {
    email: 'admin@exemplo.com.br',
    password: 'AdminSenha123!',
  },
} as const
