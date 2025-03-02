import React, { useState } from "react";
import { Link } from "react-router-dom";
import { navigationItems, NavigationItem } from "../config/navigation";
import { ChevronDown, ChevronRight } from 'lucide-react';

const Dashboard: React.FC = React.memo(() => {
  const [expandedCategories, setExpandedCategories] = useState<string[]>([]);

  const toggleCategory = (categoryId: string) => {
    setExpandedCategories(prev =>
      prev.includes(categoryId)
        ? prev.filter(id => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  return (
    <div className="dashboard">
      <main className="container">
        <div className="dashboard-grid">
          {navigationItems.map((item) => {
            const hasSubItems = item.items && item.items.length > 0;
            return hasSubItems ? (
              <CategoryCard
                key={item.id}
                item={item}
                isExpanded={expandedCategories.includes(item.id)}
                onToggle={() => toggleCategory(item.id)}
              />
            ) : (
              <SingleCard key={item.id} item={item} />
            );
          })}
        </div>
      </main>
    </div>
  );
});

const SingleCard: React.FC<{ item: NavigationItem }> = ({ item }) => {
  const Icon = item.icon;
  const href = item.href ?? "#";

  return (
    <Link to={href} className="card single-card">
      {Icon && (
        <div className="card-icon">
          <Icon />
        </div>
      )}
      <span className="card-label">{item.label}</span>
    </Link>
  );
};

interface CategoryCardProps {
  item: NavigationItem;
  isExpanded: boolean;
  onToggle: () => void;
}

const CategoryCard: React.FC<CategoryCardProps> = ({ item, isExpanded, onToggle }) => {
  const Icon = item.icon;
  const subItems = item.items || [];

  return (
    <div className={`card category-card ${isExpanded ? 'category-card-expanded' : ''}`}>
      <button className="category-header" onClick={onToggle}>
        <div className="category-header-content">
          {Icon && <Icon className="category-icon" />}
          <span className="category-title">{item.label}</span>
        </div>
        {isExpanded ?
          <ChevronDown className="category-chevron" /> :
          <ChevronRight className="category-chevron" />
        }
      </button>

      <div className={`category-items ${isExpanded ? 'category-items-expanded' : ''}`}>
        {subItems.map((sub) => (
          <Link
            key={sub.id}
            to={sub.href ?? "#"}
            className="category-link"
          >
            {sub.icon && <sub.icon className="category-subitem-icon" />}
            <span>{sub.label}</span>
          </Link>
        ))}
      </div>
    </div>
  );
};

export default Dashboard;