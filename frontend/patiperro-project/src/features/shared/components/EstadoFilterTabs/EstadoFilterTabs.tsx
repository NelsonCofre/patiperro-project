import { NavLink } from "react-router-dom";
import styles from "./EstadoFilterTabs.module.css";

export type EstadoFilterTab = {
  key: string;
  label: string;
  count: number;
  route?: string;
  end?: boolean;
};

type Props = {
  tabs: EstadoFilterTab[];
  ariaLabel: string;
  mode?: "route" | "button";
  activeKey?: string;
  onSelect?: (key: string) => void;
};

export default function EstadoFilterTabs({
  tabs,
  ariaLabel,
  mode = "route",
  activeKey,
  onSelect
}: Props) {
  return (
    <nav className={styles.tabs} aria-label={ariaLabel}>
      {tabs.map((tab) => {
        const content = (
          <>
            <span className={styles.tabLabel}>{tab.label}</span>
            <span className={styles.tabCount} aria-label={`${tab.count} en ${tab.label}`}>
              {tab.count}
            </span>
          </>
        );

        if (mode === "button") {
          const isActive = activeKey === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              className={`${styles.tab} ${isActive ? styles.tabActive : ""}`}
              aria-current={isActive ? "true" : undefined}
              onClick={() => onSelect?.(tab.key)}
            >
              {content}
            </button>
          );
        }

        if (!tab.route) {
          return null;
        }

        return (
          <NavLink
            key={tab.key}
            to={tab.route}
            end={tab.end}
            className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ""}`}
          >
            {content}
          </NavLink>
        );
      })}
    </nav>
  );
}
