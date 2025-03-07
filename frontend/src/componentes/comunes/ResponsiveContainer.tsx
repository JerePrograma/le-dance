// components/ResponsiveContainer.tsx
import { ReactNode } from "react";
import { cn } from "../lib/utils";

interface ResponsiveContainerProps {
    children: ReactNode;
    className?: string;
}

export default function ResponsiveContainer({ children, className }: ResponsiveContainerProps) {
    return (
        <div className={cn("w-full mx-auto px-[var(--container-padding)]", className)}>
            {children}
        </div>
    );
}
