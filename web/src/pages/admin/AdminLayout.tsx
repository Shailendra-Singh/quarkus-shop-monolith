import { NavLink, Outlet } from "react-router-dom";

const tabClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
    isActive
      ? "bg-slate-900 text-white"
      : "bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100"
  }`;

export function AdminLayout() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Admin</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Manage catalog data via the shop API.
        </p>
      </div>
      <nav className="flex gap-2 border-b border-slate-200 dark:border-slate-700 pb-4">
        <NavLink to="/admin/products" className={tabClass}>
          Products
        </NavLink>
        <NavLink to="/admin/categories" className={tabClass}>
          Categories
        </NavLink>
      </nav>
      <Outlet />
    </div>
  );
}
