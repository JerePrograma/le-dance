import { useState, type ReactNode } from "react";
import { SidebarContext } from "./sidebar-context";

export function SidebarProvider({ children }: { children: ReactNode }) {
    const [isExpanded, setIsExpanded] = useState(true);
    const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

    const toggleSidebar = () => {
        setIsExpanded((prev) => !prev);
    };

    const closeSidebar = () => {
        setIsExpanded(false);
    };

    return (
        <SidebarContext.Provider
            value={{ isExpanded, toggleSidebar, closeSidebar, mobileSidebarOpen, setMobileSidebarOpen }}
        >
            {children}
        </SidebarContext.Provider>
    );
}
