const currency = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

export function formatPrice(value: number | undefined | null): string {
  return currency.format(typeof value === "number" ? value : 0);
}

export function formatDate(value: string | undefined | null): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
