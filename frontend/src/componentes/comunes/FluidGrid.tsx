// components/FluidGrid.tsx
import React from "react";
import { cn } from "../lib/utils";

type GridSize = "xs" | "sm" | "md" | "lg" | "xl";

interface FluidGridProps {
    children: React.ReactNode;
    size?: GridSize;
    gap?: "none" | "sm" | "md" | "lg";
    className?: string;
}

export default function FluidGrid({
    children,
    size = "md",
    gap = "md",
    className,
}: FluidGridProps) {
    const sizeMap = {
        xs: "min(100%, 180px)",
        sm: "min(100%, 250px)",
        md: "min(100%, 320px)",
        lg: "min(100%, 380px)",
        xl: "min(100%, 450px)",
    };

    const gapMap = {
        none: "gap-0",
        sm: "gap-2 sm:gap-3",
        md: "gap-3 sm:gap-4 lg:gap-6",
        lg: "gap-4 sm:gap-6 lg:gap-8",
    };

    const gridStyle = {
        display: "grid",
        gridTemplateColumns: `repeat(auto-fit, minmax(${sizeMap[size]}, 1fr))`,
    };

    return (
        <div className={cn(gapMap[gap], className)} style={gridStyle}>
            {children}
        </div>
    );
}
