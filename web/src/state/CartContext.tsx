import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { api } from "../api/client";
import type { CartDto, ProductDto, UUID } from "../api/types";
import { useCustomer } from "./CustomerContext";

const STORAGE_KEY = "shop.cart";

export interface CartLine {
  productId: UUID;
  name: string;
  price: number;
  quantity: number;
}

interface CartContextValue {
  items: CartLine[];
  totalCount: number;
  totalPrice: number;
  /** Server-side cart (with generated id), created on first add-to-cart. */
  cart: CartDto | null;
  addItem: (product: ProductDto, quantity?: number) => void;
  setQuantity: (productId: UUID, quantity: number) => void;
  removeItem: (productId: UUID) => void;
  clear: () => void;
  /** Ensure a server cart exists for the current customer and return it. */
  ensureCart: () => Promise<CartDto | null>;
}

const CartContext = createContext<CartContextValue | undefined>(undefined);

function load(): CartLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CartLine[]) : [];
  } catch {
    return [];
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const { customer } = useCustomer();
  const [items, setItems] = useState<CartLine[]>(load);

  // The server cart is kept in memory only (not persisted): it carries the
  // generated `id` that the order references at checkout.
  const [cart, setCart] = useState<CartDto | null>(null);
  const cartRef = useRef<CartDto | null>(null);
  const creatingRef = useRef<Promise<CartDto | null> | null>(null);

  const updateCart = useCallback((next: CartDto | null) => {
    cartRef.current = next;
    setCart(next);
  }, []);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  // Drop the cached cart if the customer changes or signs out — its id belongs
  // to the previous customer.
  useEffect(() => {
    const owner = cartRef.current?.customerDto?.id;
    if (!customer?.id || (owner && owner !== customer.id)) {
      updateCart(null);
      creatingRef.current = null;
    }
  }, [customer?.id, updateCart]);

  const ensureCart = useCallback(async (): Promise<CartDto | null> => {
    if (cartRef.current?.id) return cartRef.current;
    if (!customer?.id) return null;
    if (creatingRef.current) return creatingRef.current;

    const pending = api.carts
      .createForCustomer(customer.id)
      .then((created) => {
        updateCart(created);
        creatingRef.current = null;
        return created;
      })
      .catch((err) => {
        creatingRef.current = null;
        throw err;
      });

    creatingRef.current = pending;
    return pending;
  }, [customer?.id, updateCart]);

  const addItem = useCallback(
    (product: ProductDto, quantity = 1) => {
      if (!product.id) return;
      setItems((prev) => {
        const existing = prev.find((l) => l.productId === product.id);
        if (existing) {
          return prev.map((l) =>
            l.productId === product.id
              ? { ...l, quantity: l.quantity + quantity }
              : l,
          );
        }
        return [
          ...prev,
          {
            productId: product.id!,
            name: product.name ?? "Unnamed product",
            price: product.price ?? 0,
            quantity,
          },
        ];
      });
      // Create the server cart on add-to-cart (best effort; needs a customer).
      void ensureCart().catch(() => {
        /* surfaced again at checkout */
      });
    },
    [ensureCart],
  );

  const setQuantity = useCallback((productId: UUID, quantity: number) => {
    setItems((prev) =>
      quantity <= 0
        ? prev.filter((l) => l.productId !== productId)
        : prev.map((l) =>
            l.productId === productId ? { ...l, quantity } : l,
          ),
    );
  }, []);

  const removeItem = useCallback((productId: UUID) => {
    setItems((prev) => prev.filter((l) => l.productId !== productId));
  }, []);

  const clear = useCallback(() => {
    setItems([]);
    updateCart(null);
    creatingRef.current = null;
  }, [updateCart]);

  const value = useMemo<CartContextValue>(() => {
    const totalCount = items.reduce((sum, l) => sum + l.quantity, 0);
    const totalPrice = items.reduce((sum, l) => sum + l.price * l.quantity, 0);
    return {
      items,
      totalCount,
      totalPrice,
      cart,
      addItem,
      setQuantity,
      removeItem,
      clear,
      ensureCart,
    };
  }, [items, cart, addItem, setQuantity, removeItem, clear, ensureCart]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return ctx;
}
