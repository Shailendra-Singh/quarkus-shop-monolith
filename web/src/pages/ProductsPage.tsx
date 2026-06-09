import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { UUID } from "../api/types";
import { ProductCard } from "../components/ProductCard";
import { EmptyState, Loading } from "../components/StatusMessage";

export function ProductsPage() {
  const [categoryId, setCategoryId] = useState<UUID | null>(null);
  const [search, setSearch] = useState("");

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: api.categories.list,
  });

  const productsQuery = useQuery({
    queryKey: ["products", { categoryId }],
    queryFn: () =>
      categoryId ? api.products.byCategory(categoryId) : api.products.list(),
  });

  const products = useMemo(() => {
    const list = productsQuery.data ?? [];
    const term = search.trim().toLowerCase();
    if (!term) return list;
    return list.filter(
      (p) =>
        p.name?.toLowerCase().includes(term) ||
        p.description?.toLowerCase().includes(term),
    );
  }, [productsQuery.data, search]);

  return (
    <div className="grid gap-8 md:grid-cols-[220px_1fr]">
      <aside className="space-y-4">
        <div>
          <label
            htmlFor="search"
            className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400"
          >
            Search
          </label>
          <input
            id="search"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Find a product…"
            className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
        </div>

        <div>
          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Categories
          </p>
          <ul className="space-y-1">
            <li>
              <button
                type="button"
                onClick={() => setCategoryId(null)}
                className={`w-full rounded px-2 py-1 text-left text-sm ${
                  categoryId === null
                    ? "bg-slate-900 text-white"
                    : "text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-700 dark:hover:text-white"
                }`}
              >
                All products
              </button>
            </li>
            {categoriesQuery.data?.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => c.id && setCategoryId(c.id)}
                  className={`w-full rounded px-2 py-1 text-left text-sm ${
                    categoryId === c.id
                      ? "bg-slate-900 text-white"
                      : "text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-700 dark:hover:text-white"
                  }`}
                >
                  {c.name ?? "Unnamed"}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </aside>

      <section>
        <h1 className="mb-4 text-2xl font-bold">Products</h1>
        {productsQuery.isPending ? (
          <Loading label="Loading products…" />
        ) : products.length === 0 ? (
          <EmptyState>
            {productsQuery.isError || (productsQuery.data?.length ?? 0) === 0
              ? "No products to display."
              : "No products match your search."}
          </EmptyState>
        ) : (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {products.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
