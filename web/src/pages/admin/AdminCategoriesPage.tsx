import { useMemo, useState } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../../api/client";
import type { CategoryDto, UUID } from "../../api/types";
import { EmptyState, Loading } from "../../components/StatusMessage";

export function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["categories"] });

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: api.categories.list,
  });

  const categoryName = useMemo(() => {
    const map = new Map<UUID, string>();
    for (const c of categoriesQuery.data ?? []) {
      if (c.id) map.set(c.id, c.name ?? "Unnamed");
    }
    return map;
  }, [categoriesQuery.data]);

  const createMutation = useMutation({
    mutationFn: ({
      body,
      parentId,
    }: {
      body: CategoryDto;
      parentId?: UUID;
    }) => api.categories.create(body, parentId),
    onSuccess: invalidate,
  });
  const clearProductsMutation = useMutation({
    mutationFn: (id: UUID) => api.categories.clearProducts(id),
    onSuccess: invalidate,
  });

  const [form, setForm] = useState({
    name: "",
    description: "",
    parentId: "",
  });

  return (
    <div className="space-y-8">
      <section className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
        <h2 className="mb-4 font-semibold">Create category</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate(
              {
                body: {
                  name: form.name,
                  description: form.description || undefined,
                },
                parentId: form.parentId || undefined,
              },
              {
                onSuccess: () =>
                  setForm({ name: "", description: "", parentId: "" }),
              },
            );
          }}
          className="grid gap-4 md:grid-cols-2"
        >
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Name <span className="text-red-500">*</span>
            </span>
            <input
              type="text"
              required
              value={form.name}
              onChange={(e) =>
                setForm((f) => ({ ...f, name: e.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Parent category
            </span>
            <select
              value={form.parentId}
              onChange={(e) =>
                setForm((f) => ({ ...f, parentId: e.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            >
              <option value="">— none (top level) —</option>
              {categoriesQuery.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name ?? "Unnamed"}
                </option>
              ))}
            </select>
          </label>
          <label className="block md:col-span-2">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Description
            </span>
            <input
              type="text"
              value={form.description}
              onChange={(e) =>
                setForm((f) => ({ ...f, description: e.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            />
          </label>
          <div className="md:col-span-2">
            {createMutation.isError && (
              <p className="mb-2 text-sm text-red-600">
                {(createMutation.error as Error).message}
              </p>
            )}
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
            >
              {createMutation.isPending ? "Creating…" : "Create category"}
            </button>
          </div>
        </form>
      </section>

      <section className="space-y-3">
        <h2 className="font-semibold">Categories</h2>
        <p className="text-xs text-slate-400 dark:text-slate-500">
          The API exposes no endpoint to rename or delete a category; the only
          mutation on an existing category is detaching all of its products.
        </p>

        {categoriesQuery.isPending ? (
          <Loading label="Loading categories…" />
        ) : (categoriesQuery.data?.length ?? 0) === 0 ? (
          <EmptyState>No categories to display.</EmptyState>
        ) : (
          <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-950 text-left text-slate-500 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Description</th>
                  <th className="px-4 py-3 font-medium">Parent</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {categoriesQuery.data?.map((c) => (
                  <tr key={c.id}>
                    <td className="px-4 py-3 font-medium">{c.name}</td>
                    <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                      {c.description ?? "—"}
                    </td>
                    <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                      {c.parent_category_id
                        ? categoryName.get(c.parent_category_id) ??
                          c.parent_category_id.slice(0, 8)
                        : "—"}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => {
                          if (
                            c.id &&
                            window.confirm(
                              `Remove all products from "${c.name}"?`,
                            )
                          ) {
                            clearProductsMutation.mutate(c.id);
                          }
                        }}
                        disabled={clearProductsMutation.isPending}
                        className="rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-1 text-xs font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 disabled:opacity-50"
                      >
                        Clear products
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
