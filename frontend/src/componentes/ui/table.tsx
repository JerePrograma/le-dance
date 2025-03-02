import type React from "react";

interface TableProps extends React.HTMLAttributes<HTMLTableElement> { }

export const Table: React.FC<TableProps> = ({ className = "", ...props }) => (
  <table className={`w-full caption-bottom text-sm ${className}`} {...props} />
);

interface TableHeaderProps
  extends React.HTMLAttributes<HTMLTableSectionElement> { }

export const TableHeader: React.FC<TableHeaderProps> = ({
  className = "",
  ...props
}) => <thead className={`[&_tr]:border-b ${className}`} {...props} />;

interface TableBodyProps
  extends React.HTMLAttributes<HTMLTableSectionElement> { }

export const TableBody: React.FC<TableBodyProps> = ({
  className = "",
  ...props
}) => (
  <tbody className={`[&_tr:last-child]:border-0 ${className}`} {...props} />
);

interface TableRowProps extends React.HTMLAttributes<HTMLTableRowElement> { }

export const TableRow: React.FC<TableRowProps> = ({
  className = "",
  ...props
}) => (
  <tr
    className={`border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted ${className}`}
    {...props}
  />
);

interface TableHeadProps extends React.ThHTMLAttributes<HTMLTableCellElement> { }

export const TableHead: React.FC<TableHeadProps> = ({
  className = "",
  ...props
}) => (
  <th
    className={`h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0 ${className}`}
    {...props}
  />
);

interface TableCellProps extends React.TdHTMLAttributes<HTMLTableCellElement> { }

export const TableCell: React.FC<TableCellProps> = ({
  className = "",
  ...props
}) => (
  <td
    className={`p-4 align-middle [&:has([role=checkbox])]:pr-0 ${className}`}
    {...props}
  />
);
