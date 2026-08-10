export const mockAccounts = [
  {
    userId: '71131447-a2a0-4996-a336-a8c3555bb327',
    displayName: 'Fagner',
    username: 'fagner',
    password: 'MinhaSenha123!',
    role: 'USER' as const,
  },
  {
    userId: 'b5d94b7e-3c45-4d28-99fe-70b96065b6c4',
    displayName: 'Administração',
    username: 'admin',
    password: 'AdminSenha123!',
    role: 'ADMIN' as const,
  },
]

export const mockCredentials = {
  user: {
    username: 'fagner',
    password: 'MinhaSenha123!',
  },
  admin: {
    username: 'admin',
    password: 'AdminSenha123!',
  },
} as const
