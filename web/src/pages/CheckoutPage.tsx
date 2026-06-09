import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import type { AddressDto, OrderDto } from "../api/types";
import { useCart } from "../state/CartContext";
import { useCustomer } from "../state/CustomerContext";
import { formatPrice } from "../lib/format";
import { CustomerForm } from "./AccountPage";

export function CheckoutPage() {
  const navigate = useNavigate();
  const { customer } = useCustomer();
  const { items, totalPrice, clear, ensureCart } = useCart();

  const [address, setAddress] = useState<AddressDto>({
    postalCode: "",
    countryCode: "US",
  });

  const placeOrder = useMutation({
    mutationFn: async () => {
      // Ensure the customer's server cart exists; its generated id is sent
      // with the order.
      const cart = await ensureCart();
      const order: OrderDto = {
        status: "NEW",
        price: totalPrice,
        shipmentAddress: address,
        orderItems: items.map((line) => ({
          productId: line.productId,
          quantity: line.quantity,
          unitPrice: line.price,
        })),
        cart: cart ?? undefined,
      };
      return api.orders.create(order);
    },
    onSuccess: () => {
      clear();
      navigate("/orders", { state: { justOrdered: true } });
    },
  });

  if (items.length === 0) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-bold">Checkout</h1>
        <p className="text-slate-500 dark:text-slate-400">
          Your cart is empty.{" "}
          <Link to="/" className="font-medium text-slate-900 dark:text-slate-100 underline">
            Browse products
          </Link>
        </p>
      </div>
    );
  }

  if (!customer) {
    return (
      <div className="mx-auto max-w-md space-y-4">
        <h1 className="text-2xl font-bold">Who's ordering?</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Enter your details to continue to checkout.
        </p>
        <CustomerForm />
      </div>
    );
  }

  return (
    <div className="grid gap-8 md:grid-cols-[1fr_320px]">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          placeOrder.mutate();
        }}
        className="space-y-4"
      >
        <h1 className="text-2xl font-bold">Shipping address</h1>
        <Field
          label="Address line 1"
          value={address.address1 ?? ""}
          onChange={(v) => setAddress((a) => ({ ...a, address1: v }))}
        />
        <Field
          label="Address line 2"
          value={address.address2 ?? ""}
          onChange={(v) => setAddress((a) => ({ ...a, address2: v }))}
        />
        <div className="grid grid-cols-2 gap-4">
          <Field
            label="City"
            value={address.city ?? ""}
            onChange={(v) => setAddress((a) => ({ ...a, city: v }))}
          />
          <Field
            label="Postal code"
            required
            value={address.postalCode}
            onChange={(v) => setAddress((a) => ({ ...a, postalCode: v }))}
          />
        </div>
        <Field
          label="Country code (2 letters)"
          required
          value={address.countryCode ?? ""}
          maxLength={2}
          onChange={(v) =>
            setAddress((a) => ({ ...a, countryCode: v.toUpperCase() }))
          }
        />

        {placeOrder.isError && (
          <p className="text-sm text-red-600">
            {(placeOrder.error as Error).message}
          </p>
        )}

        <button
          type="submit"
          disabled={placeOrder.isPending}
          className="rounded-lg bg-slate-900 px-6 py-2.5 font-medium text-white hover:bg-slate-700 disabled:opacity-50"
        >
          {placeOrder.isPending ? "Placing order…" : "Place order"}
        </button>
      </form>

      <aside className="h-fit space-y-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
        <h2 className="font-semibold">Order summary</h2>
        <ul className="space-y-1 text-sm">
          {items.map((line) => (
            <li key={line.productId} className="flex justify-between">
              <span className="text-slate-600 dark:text-slate-300">
                {line.name} × {line.quantity}
              </span>
              <span>{formatPrice(line.price * line.quantity)}</span>
            </li>
          ))}
        </ul>
        <div className="flex justify-between border-t border-slate-200 dark:border-slate-700 pt-3 text-lg font-bold">
          <span>Total</span>
          <span>{formatPrice(totalPrice)}</span>
        </div>
        <p className="text-xs text-slate-400 dark:text-slate-500">
          Ordering as {customer.firstName} {customer.lastName}
        </p>
      </aside>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  required,
  maxLength,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  maxLength?: number;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
        {label}
        {required && <span className="text-red-500"> *</span>}
      </span>
      <input
        type="text"
        value={value}
        required={required}
        maxLength={maxLength}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
    </label>
  );
}
