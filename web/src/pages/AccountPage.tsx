import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import type { CustomerDto } from "../api/types";
import { useCustomer } from "../state/CustomerContext";

/**
 * Creates a customer via the API (POST /api/customers) and stores it as the
 * active identity. The API defines no auth, so "signing in" simply means
 * creating or loading a customer record we keep in localStorage.
 */
export function CustomerForm({ onDone }: { onDone?: () => void }) {
  const { setCustomer } = useCustomer();
  const [form, setForm] = useState<CustomerDto>({});

  const mutation = useMutation({
    mutationFn: (body: CustomerDto) => api.customers.create(body),
    onSuccess: (created) => {
      setCustomer(created);
      onDone?.();
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate(form);
      }}
      className="space-y-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5"
    >
      <div className="grid grid-cols-2 gap-3">
        <Input
          label="First name"
          value={form.firstName ?? ""}
          onChange={(v) => setForm((f) => ({ ...f, firstName: v }))}
        />
        <Input
          label="Last name"
          value={form.lastName ?? ""}
          onChange={(v) => setForm((f) => ({ ...f, lastName: v }))}
        />
      </div>
      <Input
        label="Email"
        type="email"
        required
        value={form.email ?? ""}
        onChange={(v) => setForm((f) => ({ ...f, email: v }))}
      />
      <Input
        label="Telephone"
        value={form.telephone ?? ""}
        onChange={(v) => setForm((f) => ({ ...f, telephone: v }))}
      />
      {mutation.isError && (
        <p className="text-sm text-red-600">
          {(mutation.error as Error).message}
        </p>
      )}
      <button
        type="submit"
        disabled={mutation.isPending}
        className="rounded-lg bg-slate-900 px-5 py-2.5 font-medium text-white hover:bg-slate-700 disabled:opacity-50"
      >
        {mutation.isPending ? "Saving…" : "Continue"}
      </button>
    </form>
  );
}

export function AccountPage() {
  const { customer, setCustomer, signOut } = useCustomer();
  const [loadId, setLoadId] = useState("");
  const loadMutation = useMutation({
    mutationFn: (id: string) => api.customers.get(id),
    onSuccess: (loaded) => setCustomer(loaded),
  });

  if (customer) {
    return (
      <div className="mx-auto max-w-md space-y-4">
        <h1 className="text-2xl font-bold">Account</h1>
        <div className="space-y-1 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-5">
          <p className="text-lg font-semibold">
            {customer.firstName} {customer.lastName}
          </p>
          {customer.email && (
            <p className="text-sm text-slate-600 dark:text-slate-300">{customer.email}</p>
          )}
          {customer.telephone && (
            <p className="text-sm text-slate-600 dark:text-slate-300">{customer.telephone}</p>
          )}
          {customer.id && (
            <p className="pt-2 text-xs text-slate-400 dark:text-slate-500">ID: {customer.id}</p>
          )}
        </div>
        <button
          type="button"
          onClick={signOut}
          className="rounded-lg border border-slate-300 dark:border-slate-600 px-4 py-2 text-sm font-medium hover:bg-slate-100"
        >
          Sign out
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Sign in</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Create a customer profile to place orders and track them.
        </p>
      </div>
      <CustomerForm />

      <div className="space-y-2 border-t border-slate-200 dark:border-slate-700 pt-4">
        <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
          Already have a customer ID?
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (loadId.trim()) loadMutation.mutate(loadId.trim());
          }}
          className="flex gap-2"
        >
          <input
            type="text"
            value={loadId}
            onChange={(e) => setLoadId(e.target.value)}
            placeholder="customer UUID"
            className="flex-1 rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={loadMutation.isPending}
            className="rounded-lg border border-slate-300 dark:border-slate-600 px-4 py-2 text-sm font-medium hover:bg-slate-100 disabled:opacity-50"
          >
            Load
          </button>
        </form>
        {loadMutation.isError && (
          <p className="text-sm text-red-600">
            {(loadMutation.error as Error).message}
          </p>
        )}
      </div>
    </div>
  );
}

function Input({
  label,
  value,
  onChange,
  type = "text",
  required,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
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
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
      />
    </label>
  );
}
