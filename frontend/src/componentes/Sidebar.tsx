import type React from "react"
import { Link, useLocation } from "react-router-dom"
import { useSidebar } from "../hooks/context/SideBarContext"
import { ChevronRight, ChevronLeft, ChevronDown } from "lucide-react"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "./ui/collapsible"
import { cn } from "./lib/utils"
import { navigationItems } from "../config/navigation"

interface NavItemProps {
    icon?: React.ElementType
    label: string
    href?: string
    isActive?: boolean
    isExpanded: boolean
    onClick?: () => void
    className?: string
    children?: React.ReactNode
}

const NavItem = ({
    icon: Icon,
    label,
    href,
    isActive = false,
    isExpanded,
    onClick,
    className,
    children,
}: NavItemProps) => {
    const content = (
        <>
            <div className="flex items-center gap-3 min-w-0">
                {Icon && <Icon className="w-5 h-5 shrink-0" />}
                {isExpanded && <span className="truncate">{label}</span>}
            </div>
            {children}
        </>
    )

    return href ? (
        <Link
            to={href}
            className={cn(
                "flex items-center justify-start w-full gap-3 px-3 py-2 rounded-md transition-all duration-200",
                isActive ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground",
                className,
            )}
            onClick={onClick}
        >
            {content}
        </Link>
    ) : (
        <button
            className={cn(
                "flex items-center justify-start w-full gap-3 px-3 py-2 rounded-md transition-all duration-200",
                "text-muted-foreground hover:bg-muted hover:text-foreground",
                className,
            )}
            onClick={onClick}
        >
            {content}
        </button>
    )
}

interface NavGroupProps {
    item: (typeof navigationItems)[0]
    isExpanded: boolean
    level?: number
}

const NavGroup = ({ item, isExpanded }: NavGroupProps) => {
    const location = useLocation()
    const isActive = item.href ? location.pathname.startsWith(item.href) : false

    if (!item.items) {
        return <NavItem icon={item.icon} label={item.label} href={item.href} isActive={isActive} isExpanded={isExpanded} />
    }

    return (
        <Collapsible className="w-full">
            <CollapsibleTrigger className="w-full">
                <NavItem icon={item.icon} label={item.label} isExpanded={isExpanded} className="w-full">
                    {isExpanded && <ChevronDown className="w-4 h-4 shrink-0 transition-transform duration-200 ml-auto" />}
                </NavItem>
            </CollapsibleTrigger>
            <CollapsibleContent className="pl-4">
                {item.items.map((subItem) => (
                    <NavItem
                        key={subItem.id}
                        icon={subItem.icon}
                        label={subItem.label}
                        href={subItem.href}
                        isActive={location.pathname.startsWith(subItem.href || "")}
                        isExpanded={isExpanded}
                    />
                ))}
            </CollapsibleContent>
        </Collapsible>
    )
}

export default function Sidebar() {
    const { isExpanded, toggleSidebar } = useSidebar()

    return (
        <aside
            className={cn(
                "fixed inset-y-0 left-0 z-40 flex flex-col bg-background border-r",
                "transition-all duration-300",
                isExpanded ? "w-[var(--sidebar-width)]" : "w-[4.5rem]",
            )}
        >
            <div className="flex items-center h-[var(--header-height)] px-4 border-b">
                {isExpanded ? (
                    <Link to="/" className="text-xl font-bold text-primary">
                        LE DANCE
                    </Link>
                ) : (
                    <Link to="/" className="text-xl font-bold text-primary">
                        LD
                    </Link>
                )}
                <button
                    onClick={toggleSidebar}
                    className="p-1 rounded-md hover:bg-muted ml-auto"
                    aria-label={isExpanded ? "Colapsar menu" : "Expandir menu"}
                >
                    {isExpanded ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                </button>
            </div>

            <nav className="flex-1 overflow-y-auto py-4 px-3">
                <div className="space-y-4">
                    {navigationItems.map((item) => (
                        <NavGroup key={item.id} item={item} isExpanded={isExpanded} />
                    ))}
                </div>
            </nav>
        </aside>
    )
}

