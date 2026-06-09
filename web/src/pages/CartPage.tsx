import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../state/CartContext";
import { formatPrice } from "../lib/format";
import { EmptyState } from "../components/StatusMessage";

export function CartPage() {
  const { items, totalPrice, setQuantity, removeItem, clear } = useCart();
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-bold">Your cart</h1>
        <EmptyState>
          Your cart is empty.{" "}
          <Link to="/" className="font-medium text-slate-900 dark:text-slate-100 underline">
            Browse products
          </Link>
        </EmptyState>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Your cart</h1>

      <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 dark:bg-slate-950 text-left text-slate-500 dark:text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Product</th>
              <th className="px-4 py-3 font-medium">Price</th>
              <th className="px-4 py-3 font-medium">Qty</th>
              <th className="px-4 py-3 text-right font-medium">Subtotal</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
            {items.map((line) => (
              <tr key={line.productId}>
                <td className="px-4 py-3">
                  <Link
                    to={`/products/${line.productId}`}
                    className="font-medium hover:underline"
                  >
                    {line.name}
                  </Link>
                </td>
                <td className="px-4 py-3">{formatPrice(line.price)}</td>
                <td className="px-4 py-3">
                  <input
                    type="number"
                    min={1}
                    value={line.quantity}
                    onChange={(e) =>
                      setQuantity(line.productId, Number(e.target.value))
                    }
                    className="w-16 rounded-lg border border-slate-300 dark:border-slate-600 px-2 py-1"
                  />
                </td>
                <td className="px-4 py-3 text-right font-medium">
                  {formatPrice(line.price * line.quantity)}
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => removeItem(line.productId)}
                    className="text-slate-400 dark:text-slate-500 hover:text-red-600"
                    aria-label="Remove item"
                  >
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={clear}
          className="text-sm text-slate-500 dark:text-slate-400 hover:text-red-600"
        >
          Clear cart
        </button>
        <div className="flex items-center gap-6">
          <span className="text-lg">
            Total:{" "}
            <span className="font-bold">{formatPrice(totalPrice)}</span>
          </span>
          <button
            type="button"
            onClick={() => navigate("/checkout")}
            className="rounded-lg bg-slate-900 px-6 py-2.5 font-medium text-white hover:bg-slate-700"
          >
            Checkout
          </button>
        </div>
      </div>
    </div>
  );
}
