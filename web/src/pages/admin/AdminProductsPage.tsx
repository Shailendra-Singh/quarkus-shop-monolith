import { useMemo, useState } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/client";
import type { CategoryDto, ProductDto, UUID } from "../../api/types";
import { formatPrice } from "../../lib/format";
import { EmptyState, Loading } from "../../components/StatusMessage";

export function AdminProductsPage() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["products"] });

  const productsQuery = useQuery({
    queryKey: ["products", { categoryId: null }],
    queryFn: api.products.list,
  });
  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: api.categories.list,
  });
  const countQuery = useQuery({
    queryKey: ["products", "count"],
    queryFn: api.products.count,
  });

  const categoryName = useMemo(() => {
    const map = new Map<UUID, string>();
    for (const c of categoriesQuery.data ?? []) {
      if (c.id) map.set(c.id, c.name ?? "Unnamed");
    }
    return map;
  }, [categoriesQuery.data]);

  const createMutation = useMutation({
    // Create the product, then assign the chosen category via
    // PUT /api/products/{id}/categories/{categoryId}.
    mutationFn: async (vars: { body: ProductDto; categoryId: UUID }) => {
      const created = await api.products.create(vars.body);
      if (created?.id) {
        await api.products.addCategory(created.id, vars.categoryId);
      }
      return created;
    },
    onSettled: invalidate,
  });
  const deleteMutation = useMutation({
    mutationFn: (id: UUID) => api.products.remove(id),
    onSuccess: invalidate,
  });

  const [form, setForm] = useState({
    name: "",
    description: "",
    price: "",
    status: "AVAILABLE",
    categoryId: "",
  });

  const priceNumber = Number(form.price);
  const priceValid =
    form.price.trim() !== "" &&
    Number.isFinite(priceNumber) &&
    priceNumber > 0;
  const priceError =
    form.price.trim() !== "" && !priceValid
      ? "Price must be greater than 0."
      : undefined;
  const canSubmit =
    form.name.trim() !== "" && form.categoryId !== "" && priceValid;

  const categories = categoriesQuery.data ?? [];

  return (
    <div className="space-y-8">
      <section className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
        <h2 className="mb-4 font-semibold">Create product</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!canSubmit) return;
            createMutation.mutate(
              {
                body: {
                  name: form.name,
                  description: form.description || undefined,
                  price: priceNumber,
                  status: form.status || undefined,
                },
                categoryId: form.categoryId,
              },
              {
                onSuccess: () =>
                  setForm({
                    name: "",
                    description: "",
                    price: "",
                    status: "AVAILABLE",
                    categoryId: "",
                  }),
              },
            );
          }}
          className="grid gap-4 md:grid-cols-2"
        >
          <Input
            label="Name"
            required
            value={form.name}
            onChange={(v) => setForm((f) => ({ ...f, name: v }))}
          />
          <Input
            label="Price"
            type="number"
            required
            min="0.01"
            value={form.price}
            error={priceError}
            onChange={(v) => setForm((f) => ({ ...f, price: v }))}
          />
          <Input
            label="Description"
            value={form.description}
            onChange={(v) => setForm((f) => ({ ...f, description: v }))}
          />
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Status
            </span>
            <select
              value={form.status}
              onChange={(e) =>
                setForm((f) => ({ ...f, status: e.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            >
              <option value="AVAILABLE">AVAILABLE</option>
              <option value="DISCONTINUED">DISCONTINUED</option>
            </select>
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Category <span className="text-red-500">*</span>
            </span>
            <select
              required
              value={form.categoryId}
              onChange={(e) =>
                setForm((f) => ({ ...f, categoryId: e.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            >
              <option value="">Select category</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name ?? "Unnamed"}
                </option>
              ))}
            </select>
            {categories.length === 0 && (
              <span className="mt-1 block text-xs text-amber-600">
                No categories yet — create one on the Categories tab first.
              </span>
            )}
          </label>
          <div className="md:col-span-2">
            {createMutation.isError && (
              <p className="mb-2 text-sm text-red-600">
                {(createMutation.error as Error).message}
              </p>
            )}
            <button
              type="submit"
              disabled={!canSubmit || createMutation.isPending}
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {createMutation.isPending ? "Creating…" : "Create product"}
            </button>
          </div>
        </form>
      </section>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">
            Products{" "}
            {typeof countQuery.data === "number" && (
              <span className="text-slate-400 dark:text-slate-500">({countQuery.data})</span>
            )}
          </h2>
        </div>

        {productsQuery.isPending ? (
          <Loading label="Loading products…" />
        ) : (productsQuery.data?.length ?? 0) === 0 ? (
          <EmptyState>No products to display.</EmptyState>
        ) : (
          <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-950 text-left text-slate-500 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Price</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Categories</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {productsQuery.data?.map((product) => (
                  <ProductRow
                    key={product.id}
                    product={product}
                    categories={categoriesQuery.data ?? []}
                    categoryName={categoryName}
                    onDelete={() => {
                      if (
                        product.id &&
                        window.confirm(`Delete "${product.name}"?`)
                      ) {
                        deleteMutation.mutate(product.id);
                      }
                    }}
                    onCategoriesChanged={invalidate}
                    deleting={deleteMutation.isPending}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function ProductRow({
  product,
  categories,
  categoryName,
  onDelete,
  onCategoriesChanged,
  deleting,
}: {
  product: ProductDto;
  categories: CategoryDto[];
  categoryName: Map<UUID, string>;
  onDelete: () => void;
  onCategoriesChanged: () => void;
  deleting: boolean;
}) {
  const assigned = product.categories ?? [];
  const available = categories.filter(
    (c) => c.id && !assigned.includes(c.id),
  );

  const addMutation = useMutation({
    mutationFn: (categoryId: UUID) =>
      api.products.addCategory(product.id!, categoryId),
    onSuccess: onCategoriesChanged,
  });
  const removeMutation = useMutation({
    mutationFn: (categoryId: UUID) =>
      api.products.removeCategory(product.id!, categoryId),
    onSuccess: onCategoriesChanged,
  });

  return (
    <tr>
      <td className="px-4 py-3 font-medium">{product.name}</td>
      <td className="px-4 py-3">{formatPrice(product.price)}</td>
      <td className="px-4 py-3">
        {product.status && (
          <span className="rounded-full bg-slate-100 dark:bg-slate-700 px-2 py-0.5 text-xs">
            {product.status}
          </span>
        )}
      </td>
      <td className="px-4 py-3">
        <div className="flex flex-wrap items-center gap-1">
          {assigned.map((cid) => (
            <span
              key={cid}
              className="inline-flex items-center gap-1 rounded-full bg-slate-100 dark:bg-slate-700 px-2 py-0.5 text-xs"
            >
              {categoryName.get(cid) ?? cid.slice(0, 8)}
              <button
                type="button"
                onClick={() => removeMutation.mutate(cid)}
                className="text-slate-400 dark:text-slate-500 hover:text-red-600"
                aria-label="Remove category"
              >
                ✕
              </button>
            </span>
          ))}
          {available.length > 0 && (
            <select
              value=""
              onChange={(e) => {
                if (e.target.value) addMutation.mutate(e.target.value);
              }}
              className="rounded border border-slate-300 dark:border-slate-600 px-1 py-0.5 text-xs"
            >
              <option value="">+ add…</option>
              {available.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name ?? "Unnamed"}
                </option>
              ))}
            </select>
          )}
        </div>
      </td>
      <td className="px-4 py-3 text-right">
        <button
          type="button"
          onClick={onDelete}
          disabled={deleting}
          className="rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
        >
          Delete
        </button>
      </td>
    </tr>
  );
}

function Input({
  label,
  value,
  onChange,
  type = "text",
  required,
  min,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
  min?: string;
  error?: string;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
        {label}
        {required && <span className="text-red-500"> *</span>}
      </span>
      <input
        type={type}
        value={value}
        required={required}
        min={min}
        step={type === "number" ? "0.01" : undefined}
        onChange={(e) => onChange(e.target.value)}
        className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none ${
          error
            ? "border-red-400 focus:border-red-500"
            : "border-slate-300 dark:border-slate-600 focus:border-slate-500"
        }`}
      />
      {error && (
        <span className="mt-1 block text-xs text-red-600">{error}</span>
      )}
    </label>
  );
}
