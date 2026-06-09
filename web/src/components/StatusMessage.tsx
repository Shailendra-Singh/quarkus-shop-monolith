export function Loading({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-slate-500 dark:text-slate-400">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 dark:border-slate-600 border-t-slate-600" />
      <span>{label}</span>
    </div>
  );
}

export function ErrorBox({ error }: { error: unknown }) {
  const message =
    error instanceof Error ? error.message : "Something went wrong.";
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <p className="font-medium">Could not load data</p>
      <p className="mt-1">{message}</p>
      <p className="mt-2 text-red-500">
        Check that the API is running and <code>VITE_API_BASE_URL</code> is set
        correctly.
      </p>
    </div>
  );
}

export function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-dashed border-slate-300 dark:border-slate-600 p-10 text-center text-slate-500 dark:text-slate-400">
      {children}
    </div>
  );
}
