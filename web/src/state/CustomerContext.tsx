import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { CustomerDto } from "../api/types";

const STORAGE_KEY = "shop.customer";

interface CustomerContextValue {
  customer: CustomerDto | null;
  setCustomer: (customer: CustomerDto | null) => void;
  signOut: () => void;
}

const CustomerContext = createContext<CustomerContextValue | undefined>(
  undefined,
);

function load(): CustomerDto | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CustomerDto) : null;
  } catch {
    return null;
  }
}

export function CustomerProvider({ children }: { children: ReactNode }) {
  const [customer, setCustomerState] = useState<CustomerDto | null>(load);

  useEffect(() => {
    if (customer) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(customer));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [customer]);

  const setCustomer = useCallback(
    (next: CustomerDto | null) => setCustomerState(next),
    [],
  );
  const signOut = useCallback(() => setCustomerState(null), []);

  const value = useMemo(
    () => ({ customer, setCustomer, signOut }),
    [customer, setCustomer, signOut],
  );

  return (
    <CustomerContext.Provider value={value}>
      {children}
    </CustomerContext.Provider>
  );
}

export function useCustomer(): CustomerContextValue {
  const ctx = useContext(CustomerContext);
  if (!ctx) {
    throw new Error("useCustomer must be used within a CustomerProvider");
  }
  return ctx;
}
