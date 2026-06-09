import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../api/client";
import type { ReviewDto } from "../api/types";
import { useCart } from "../state/CartContext";
import { formatPrice } from "../lib/format";
import { EmptyState, Loading } from "../components/StatusMessage";

function Stars({ rating = 0 }: { rating?: number }) {
  const full = Math.max(0, Math.min(5, Math.round(rating)));
  return (
    <span className="text-amber-500" aria-label={`${full} out of 5`}>
      {"★".repeat(full)}
      <span className="text-slate-300">{"★".repeat(5 - full)}</span>
    </span>
  );
}

function ReviewForm({ productId }: { productId: string }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ReviewDto>({ rating: 5 });

  const mutation = useMutation({
    mutationFn: (body: ReviewDto) => api.reviews.create(productId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
      setForm({ rating: 5 });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate(form);
      }}
      className="space-y-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-4"
    >
      <h3 className="font-semibold">Write a review</h3>
      <input
        type="text"
        required
        placeholder="Title"
        value={form.title ?? ""}
        onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
        className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
      <textarea
        placeholder="Share your thoughts…"
        value={form.description ?? ""}
        onChange={(e) =>
          setForm((f) => ({ ...f, description: e.target.value }))
        }
        className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
        rows={3}
      />
      <label className="flex items-center gap-2 text-sm">
        Rating
        <select
          value={form.rating ?? 5}
          onChange={(e) =>
            setForm((f) => ({ ...f, rating: Number(e.target.value) }))
          }
          className="rounded-lg border border-slate-300 dark:border-slate-600 px-2 py-1"
        >
          {[5, 4, 3, 2, 1].map((n) => (
            <option key={n} value={n}>
              {n} ★
            </option>
          ))}
        </select>
      </label>
      {mutation.isError && (
        <p className="text-sm text-red-600">
          {(mutation.error as Error).message}
        </p>
      )}
      <button
        type="submit"
        disabled={mutation.isPending}
        className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
      >
        {mutation.isPending ? "Submitting…" : "Submit review"}
      </button>
    </form>
  );
}

export function ProductDetailPage() {
  const { id = "" } = useParams();
  const { addItem } = useCart();
  const [added, setAdded] = useState(false);

  const productQuery = useQuery({
    queryKey: ["product", id],
    queryFn: () => api.products.get(id),
    enabled: Boolean(id),
  });

  const reviewsQuery = useQuery({
    queryKey: ["reviews", id],
    queryFn: () => api.reviews.byProduct(id),
    enabled: Boolean(id),
  });

  if (productQuery.isPending) return <Loading label="Loading product…" />;
  if (productQuery.isError || !productQuery.data) {
    return <EmptyState>This product is unavailable.</EmptyState>;
  }

  const product = productQuery.data;

  return (
    <div className="space-y-8">
      <Link to="/" className="text-sm text-slate-500 dark:text-slate-400 hover:underline">
        ← Back to products
      </Link>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="flex aspect-square items-center justify-center rounded-xl bg-slate-100 dark:bg-slate-700 text-7xl">
          📦
        </div>
        <div className="space-y-4">
          <h1 className="text-3xl font-bold">{product.name}</h1>
          <p className="text-2xl font-semibold">
            {formatPrice(product.price)}
          </p>
          {product.description && (
            <p className="text-slate-600 dark:text-slate-300">{product.description}</p>
          )}
          {product.status && (
            <span className="inline-block rounded-full bg-slate-100 dark:bg-slate-700 px-3 py-1 text-xs font-medium text-slate-600 dark:text-slate-300">
              {product.status}
            </span>
          )}
          <div>
            <button
              type="button"
              onClick={() => {
                addItem(product);
                setAdded(true);
                window.setTimeout(() => setAdded(false), 1500);
              }}
              className="rounded-lg bg-slate-900 px-5 py-2.5 font-medium text-white hover:bg-slate-700"
            >
              {added ? "Added ✓" : "Add to cart"}
            </button>
          </div>
        </div>
      </div>

      <section className="space-y-4">
        <h2 className="text-xl font-bold">Reviews</h2>
        <div className="grid gap-6 md:grid-cols-[1fr_320px]">
          <div className="space-y-3">
            {reviewsQuery.isPending ? (
              <Loading label="Loading reviews…" />
            ) : reviewsQuery.data && reviewsQuery.data.length > 0 ? (
              reviewsQuery.data.map((r) => (
                <article
                  key={r.id}
                  className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-4"
                >
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold">{r.title}</h3>
                    <Stars rating={r.rating} />
                  </div>
                  {r.description && (
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                      {r.description}
                    </p>
                  )}
                </article>
              ))
            ) : (
              <p className="text-sm text-slate-500 dark:text-slate-400">
                No reviews yet. Be the first!
              </p>
            )}
          </div>
          <ReviewForm productId={id} />
        </div>
      </section>
    </div>
  );
}
