import { createContext } from "react";

export interface SidebarContextType {
  isExpanded: boolean;
  toggleSidebar: () => void;
  closeSidebar: () => void;
  mobileSidebarOpen: boolean;
  setMobileSidebarOpen: (open: boolean) => void;
}

export const SidebarContext = createContext<SidebarContextType | undefined>(undefined);
