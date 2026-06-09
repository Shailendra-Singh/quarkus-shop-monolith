import type {
  CartDto,
  CategoryDto,
  CustomerDto,
  OrderDto,
  PaymentDto,
  ProductDto,
  ReviewDto,
  UUID,
} from "./types";

declare global {
  interface Window {
    __APP_CONFIG__?: { apiBaseUrl?: string };
  }
}

// Resolution order: runtime config (config.js, set per-container) →
// build-time Vite env → localhost default.
const runtimeBaseUrl =
  typeof window !== "undefined" ? window.__APP_CONFIG__?.apiBaseUrl : undefined;

const BASE_URL = (
  runtimeBaseUrl ||
  import.meta.env.VITE_API_BASE_URL ||
  "/api"
).replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers ?? {}),
    },
    ...options,
  });

  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.text();
      if (body) detail = body;
    } catch {
      /* ignore body read errors */
    }
    throw new ApiError(res.status, `Request failed (${res.status}): ${detail}`);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? (JSON.parse(text) as T) : (undefined as T));
}

// Storefront-relevant subset of the shop API.
export const api = {
  products: {
    list: () => request<ProductDto[]>("/products"),
    get: (id: UUID) => request<ProductDto>(`/products/${id}`),
    byCategory: (categoryId: UUID) =>
      request<ProductDto[]>(`/products/categories/${categoryId}`),
    count: () => request<number>("/products/counts"),
    countByCategory: (categoryId: UUID) =>
      request<number>(`/products/counts/categories/${categoryId}`),
    create: (body: ProductDto) =>
      request<ProductDto>("/products", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    remove: (id: UUID) =>
      request<void>(`/products/${id}`, { method: "DELETE" }),
    // The spec has no PUT for product fields — "updating" a product means
    // managing its category associations.
    addCategory: (id: UUID, categoryId: UUID) =>
      request<void>(`/products/${id}/categories/${categoryId}`, {
        method: "PUT",
      }),
    removeCategory: (id: UUID, categoryId: UUID) =>
      request<void>(`/products/${id}/categories/${categoryId}`, {
        method: "DELETE",
      }),
    setCategories: (id: UUID, categoryIds: UUID[]) =>
      request<void>(`/products/${id}/categories`, {
        method: "POST",
        body: JSON.stringify(categoryIds),
      }),
  },
  categories: {
    list: () => request<CategoryDto[]>("/categories"),
    get: (id: UUID) => request<CategoryDto>(`/categories/${id}`),
    create: (body: CategoryDto, parentCategoryId?: UUID) => {
      const qs = parentCategoryId
        ? `?parentCategoryId=${encodeURIComponent(parentCategoryId)}`
        : "";
      return request<CategoryDto>(`/categories${qs}`, {
        method: "POST",
        body: JSON.stringify(body),
      });
    },
    // The spec exposes no delete/update for a category itself; this only
    // detaches all products from it.
    clearProducts: (id: UUID) =>
      request<void>(`/categories/${id}/products`, { method: "DELETE" }),
  },
  reviews: {
    byProduct: (productId: UUID) =>
      request<ReviewDto[]>(`/reviews/products/${productId}`),
    countByProduct: (productId: UUID) =>
      request<number>(`/reviews/products/${productId}/count`),
    create: (productId: UUID, body: ReviewDto) =>
      request<ReviewDto>(`/reviews/products/${productId}`, {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },
  customers: {
    get: (id: UUID) => request<CustomerDto>(`/customers/${id}`),
    create: (body: CustomerDto) =>
      request<CustomerDto>("/customers", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },
  orders: {
    byCustomer: (customerId: UUID) =>
      request<OrderDto[]>(`/orders/customers/${customerId}`),
    get: (id: UUID) => request<OrderDto>(`/orders/${id}`),
    create: (body: OrderDto) =>
      request<OrderDto>("/orders", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    cancel: (id: UUID) =>
      request<void>(`/orders/${id}/cancel`, { method: "POST" }),
  },
  payments: {
    createForOrder: (orderId: UUID) =>
      request<PaymentDto>(`/payments/orders/${orderId}`, { method: "POST" }),
  },
  carts: {
    getActiveForCustomer: (customerId: UUID) =>
      request<CartDto>(`/carts/customers/${customerId}`),
    createForCustomer: (customerId: UUID) =>
      request<CartDto>(`/carts/customers/${customerId}`, { method: "POST" }),
  },
};
