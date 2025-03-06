// src/componentes/NavGroup.tsx
import React from "react";
import { Link, useLocation } from "react-router-dom";
import { ChevronDown } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "../componentes/ui/collapsible";
import { cn } from "../componentes/lib/utils";
import type { NavigationItem } from "../config/navigation";

interface NavGroupProps {
  item: NavigationItem;
  isExpanded: boolean;
}

const NavGroup: React.FC<NavGroupProps> = ({ item, isExpanded }) => {
  const location = useLocation();
  const isActive = item.href ? location.pathname.startsWith(item.href) : false;
  const Icon = item.icon;

  // Si no tiene sub-items, se muestra como un link simple
  if (!item.items || item.items.length === 0) {
    return (
      <Link
        to={item.href || "#"}
        className={cn(
          "flex items-center justify-start w-full gap-3 px-3 py-2 rounded-md transition-all duration-200",
          isActive
            ? "bg-[hsl(var(--primary))] text-[hsl(var(--primary-foreground))]"
            : "text-[hsl(var(--muted-foreground))] hover:bg-[hsl(var(--muted))] hover:text-[hsl(var(--foreground))]"
        )}
      >
        {Icon && <Icon className="w-5 h-5 shrink-0" />}
        {isExpanded && <span>{item.label}</span>}
      </Link>
    );
  }

  // Si tiene sub-items, se muestra como collapsible
  return (
    <Collapsible className="w-full">
      <CollapsibleTrigger className="w-full">
        <div className={cn("flex items-center justify-between w-full px-3 py-2 rounded-md transition-colors hover:bg-[hsl(var(--muted))]")}>
          <div className="flex items-center gap-3">
            {Icon && <Icon className="w-5 h-5 shrink-0" />}
            {isExpanded && <span className="text-[hsl(var(--foreground))]">{item.label}</span>}
          </div>
          {isExpanded && <ChevronDown className="w-4 h-4 text-[hsl(var(--muted-foreground))] transition-transform duration-200" />}
        </div>
      </CollapsibleTrigger>
      <CollapsibleContent className="pl-4">
        {item.items.map((subItem) => {
          const SubIcon = subItem.icon;
          return (
            <Link
              key={subItem.id}
              to={subItem.href || "#"}
              className={cn(
                "block px-3 py-2 rounded-md transition-colors hover:bg-[hsl(var(--muted))] hover:text-[hsl(var(--foreground))]",
                location.pathname.startsWith(subItem.href || "")
                  ? "bg-[hsl(var(--primary))] text-[hsl(var(--primary-foreground))]"
                  : "text-[hsl(var(--muted-foreground))]"
              )}
            >
              <div className="flex items-center gap-2">
                {SubIcon && <SubIcon className="w-4 h-4 shrink-0" />}
                <span>{subItem.label}</span>
              </div>
            </Link>
          );
        })}
      </CollapsibleContent>
    </Collapsible>
  );
};

export default NavGroup;
