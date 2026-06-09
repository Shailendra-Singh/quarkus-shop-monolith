import { Link, NavLink } from "react-router-dom";
import { useCart } from "../state/CartContext";
import { useCustomer } from "../state/CustomerContext";
import { useTheme } from "../state/ThemeContext";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-2 text-sm font-medium transition-colors ${
    isActive
      ? "bg-slate-900 text-white"
      : "text-slate-600 dark:text-slate-300 hover:bg-slate-100 hover:text-slate-900"
  }`;

export function Navbar() {
  const { totalCount } = useCart();
  const { customer } = useCustomer();
  const { theme, toggle } = useTheme();

  const customerName = customer
    ? [customer.firstName, customer.lastName].filter(Boolean).join(" ") ||
      customer.email ||
      "Account"
    : null;

  return (
    <header className="sticky top-0 z-10 border-b border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900/90 backdrop-blur">
      <nav className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-3">
        <Link to="/" className="mr-4 flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-slate-100">
          <img src="/quarkus-icon.svg" alt="Quarkus" className="h-7 w-7" />
          Quarkus Shop
        </Link>
        <NavLink to="/" className={linkClass} end>
          Products
        </NavLink>
        <NavLink to="/orders" className={linkClass}>
          Orders
        </NavLink>
        <NavLink to="/admin" className={linkClass}>
          Admin
        </NavLink>

        <div className="ml-auto flex items-center gap-2">
          <button
            type="button"
            onClick={toggle}
            aria-label="Toggle dark mode"
            title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
            className="rounded px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            {theme === "dark" ? "☀️" : "🌙"}
          </button>
          <NavLink to="/account" className={linkClass}>
            {customerName ?? "Sign in"}
          </NavLink>
          <NavLink to="/cart" className={linkClass}>
            <span className="inline-flex items-center gap-2">
              Cart
              <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-slate-900 px-1.5 text-xs font-semibold text-white">
                {totalCount}
              </span>
            </span>
          </NavLink>
        </div>
      </nav>
    </header>
  );
}
