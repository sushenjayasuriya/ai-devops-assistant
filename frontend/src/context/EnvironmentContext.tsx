import React, { createContext, useContext, useState, useEffect } from 'react';
import { Environment } from '../types/infrastructure';
import { infraService } from '../services/infraService';

interface EnvironmentContextType {
  environments: Environment[];
  selectedEnv: Environment | null;
  setSelectedEnv: (env: Environment | null) => void;
  loading: boolean;
}

const EnvironmentContext = createContext<EnvironmentContextType | undefined>(undefined);

export const EnvironmentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [selectedEnv, setSelectedEnv] = useState<Environment | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    infraService.getEnvironments()
      .then((data) => {
        setEnvironments(data);
        if (data.length > 0) {
          // Default to PRODUCTION or first
          const prod = data.find(e => e.isProduction) || data[0];
          setSelectedEnv(prod);
        }
      })
      .catch((err) => console.error('Failed to load environments', err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <EnvironmentContext.Provider value={{ environments, selectedEnv, setSelectedEnv, loading }}>
      {children}
    </EnvironmentContext.Provider>
  );
};

export const useEnvironment = () => {
  const context = useContext(EnvironmentContext);
  if (!context) throw new Error('useEnvironment must be used within an EnvironmentProvider');
  return context;
};
