"use client";

import React, { useState, useRef, useEffect } from "react";

export interface SelectProps {
  value: string;
  onValueChange: (value: string) => void;
  children: React.ReactNode;
  placeholder?: string;
}

// SelectTrigger and SelectValue components
export const SelectTrigger: React.FC<{
  onClick?: () => void;
  children: React.ReactNode;
}> = ({ onClick, children }) => (
  <button
    type="button"
    className="flex w-full cursor-default select-none items-center rounded-md border border-input bg-background p-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
    onClick={onClick}
  >
    {children}
  </button>
);

export const SelectValue: React.FC<{ value: string; placeholder?: string }> = ({
  value,
  placeholder,
}) => {
  return (
    <span className="flex items-center space-x-1.5">
      {value === "" ? (
        <span className="text-muted-foreground">{placeholder}</span>
      ) : (
        value
      )}
    </span>
  );
};

export const Select: React.FC<SelectProps> = ({
  value,
  onValueChange,
  children,
  placeholder,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        selectRef.current &&
        !selectRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleSelectItem = (itemValue: string) => {
    onValueChange(itemValue);
    setIsOpen(false);
  };

  return (
    <div ref={selectRef} className="relative">
      <SelectTrigger onClick={() => setIsOpen(!isOpen)}>
        <SelectValue value={value} placeholder={placeholder} />
      </SelectTrigger>
      {isOpen && (
        <SelectContent>
          {React.Children.map(children, (child) => {
            if (React.isValidElement(child) && child.type === SelectItem) {
              return React.cloneElement(
                child as React.ReactElement<SelectItemProps>,
                {
                  onClick: () => handleSelectItem(child.props.value),
                }
              );
            }
            return child;
          })}
        </SelectContent>
      )}
    </div>
  );
};

export interface SelectContentProps {
  children: React.ReactNode;
}

export const SelectContent: React.FC<SelectContentProps> = ({ children }) => (
  <div className="absolute mt-1 max-h-60 w-full overflow-auto rounded-md bg-popover text-popover-foreground shadow-md z-50">
    <ul className="p-1">{children}</ul>
  </div>
);

export interface SelectItemProps extends React.LiHTMLAttributes<HTMLLIElement> {
  value: string;
  onClick?: () => void;
}

export const SelectItem: React.FC<SelectItemProps> = ({
  className = "",
  children,
  onClick,
  ...props
}) => (
  <li
    className={`relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 ${className}`}
    onClick={onClick}
    {...props}
  >
    {children}
  </li>
);
