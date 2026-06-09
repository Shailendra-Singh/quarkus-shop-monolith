import { Link } from "react-router-dom";
import type { ProductDto } from "../api/types";
import { useCart } from "../state/CartContext";
import { formatPrice } from "../lib/format";

export function ProductCard({ product }: { product: ProductDto }) {
  const { addItem } = useCart();

  return (
    <div className="flex flex-col overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 shadow-sm transition-shadow hover:shadow-md">
      <Link
        to={`/products/${product.id}`}
        className="flex aspect-[4/3] items-center justify-center bg-slate-100 dark:bg-slate-700 text-4xl"
        aria-hidden
      >
        📦
      </Link>
      <div className="flex flex-1 flex-col p-4">
        <Link
          to={`/products/${product.id}`}
          className="font-semibold text-slate-900 dark:text-slate-100 hover:underline"
        >
          {product.name ?? "Unnamed product"}
        </Link>
        {product.description && (
          <p className="mt-1 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">
            {product.description}
          </p>
        )}
        <div className="mt-auto flex items-center justify-between pt-4">
          <span className="text-lg font-bold">
            {formatPrice(product.price)}
          </span>
          <button
            type="button"
            onClick={() => addItem(product)}
            className="rounded-lg bg-slate-900 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-slate-700"
          >
            Add to cart
          </button>
        </div>
      </div>
    </div>
  );
}
