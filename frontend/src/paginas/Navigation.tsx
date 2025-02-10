// src/paginas/Navigation.tsx
import { Link } from "react-router-dom";
import { navigationItems, NavigationItem } from "../config/navigation";

const Navigation = () => {
    return (
        <nav>
            <ul className="menu">
                {navigationItems.map((item: NavigationItem) => (
                    <li key={item.id} className="menu-item">
                        <Link to={item.href ?? "#"}>
                            {item.icon && <item.icon className="w-5 h-5 mr-1" />}
                            {item.label}
                        </Link>
                    </li>
                ))}
            </ul>
        </nav>
    );
};

export default Navigation;
