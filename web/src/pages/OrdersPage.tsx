import { Link, useLocation } from "react-router-dom";
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../api/client";
import type { OrderDto, UUID } from "../api/types";
import { useCustomer } from "../state/CustomerContext";
import { formatDate, formatPrice } from "../lib/format";
import { EmptyState, Loading } from "../components/StatusMessage";

export function OrdersPage() {
  const { customer } = useCustomer();
  const location = useLocation();
  const justOrdered = (location.state as { justOrdered?: boolean } | null)
    ?.justOrdered;

  const ordersQuery = useQuery({
    queryKey: ["orders", customer?.id],
    queryFn: () => api.orders.byCustomer(customer!.id!),
    enabled: Boolean(customer?.id),
  });

  if (!customer) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-bold">Orders</h1>
        <EmptyState>
          <Link to="/account" className="font-medium text-slate-900 dark:text-slate-100 underline">
            Sign in
          </Link>{" "}
          to view your order history.
        </EmptyState>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Your orders</h1>

      {justOrdered && (
        <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">
          🎉 Order placed successfully!
        </div>
      )}

      {ordersQuery.isPending ? (
        <Loading label="Loading orders…" />
      ) : !ordersQuery.data || ordersQuery.data.length === 0 ? (
        <EmptyState>
          {ordersQuery.isError
            ? "No orders to display."
            : "You haven't placed any orders yet."}
        </EmptyState>
      ) : (
        <div className="space-y-4">
          {ordersQuery.data.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              customerId={customer.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function OrderCard({
  order,
  customerId,
}: {
  order: OrderDto;
  customerId?: UUID;
}) {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["orders", customerId] });

  const payMutation = useMutation({
    mutationFn: () => api.payments.createForOrder(order.id!),
    onSuccess: invalidate,
  });
  const cancelMutation = useMutation({
    mutationFn: () => api.orders.cancel(order.id!),
    onSuccess: invalidate,
  });

  const busy = payMutation.isPending || cancelMutation.isPending;
  const error = payMutation.error ?? cancelMutation.error;
  const isCreation = order.status === "CREATION";

  return (
    <article className="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="font-semibold">Order {order.id}</p>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Shipped: {formatDate(order.shipped)}
          </p>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold">{formatPrice(order.price)}</p>
        </div>
      </div>

      {order.orderItems && order.orderItems.length > 0 && (
        <ul className="mt-3 space-y-1 border-t border-slate-100 dark:border-slate-800 pt-3 text-sm text-slate-600 dark:text-slate-300">
          {order.orderItems.map((item) => (
            <li
              key={item.id ?? item.productId}
              className="flex justify-between"
            >
              <span>Product {item.productId}</span>
              <span>
                {item.quantity} × {formatPrice(item.unitPrice)}
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-4 border-t border-slate-100 dark:border-slate-800 pt-4">
        {isCreation ? (
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => payMutation.mutate()}
                disabled={busy || !order.id}
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {payMutation.isPending ? "Paying…" : "Pay Order"}
              </button>
              <button
                type="button"
                onClick={() => {
                  if (order.id && window.confirm(`Cancel order ${order.id}?`)) {
                    cancelMutation.mutate();
                  }
                }}
                disabled={busy || !order.id}
                className="rounded-lg border border-slate-300 dark:border-slate-600 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {cancelMutation.isPending ? "Cancelling…" : "Cancel Order"}
              </button>
            </div>
            {error && (
              <p className="text-sm text-red-600">{(error as Error).message}</p>
            )}
          </div>
        ) : (
          <span className="inline-block rounded-full bg-slate-100 dark:bg-slate-700 px-3 py-1 text-xs font-medium text-slate-600 dark:text-slate-300">
            {order.status ?? "—"}
          </span>
        )}
      </div>
    </article>
  );
}
