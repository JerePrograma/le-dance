import { createContext, useContext, useState, type ReactNode } from "react";

interface SidebarContextType {
    isExpanded: boolean;
    toggleSidebar: () => void;
    closeSidebar: () => void;
    mobileSidebarOpen: boolean;
    setMobileSidebarOpen: (open: boolean) => void;
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

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

export function useSidebar() {
    const context = useContext(SidebarContext);
    if (context === undefined) {
        throw new Error("useSidebar must be used within a SidebarProvider");
    }
    return context;
}
