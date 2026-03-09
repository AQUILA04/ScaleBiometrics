"use client";

import * as React from "react";
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  useReactTable,
  getPaginationRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  SortingState,
  PaginationState,
  RowSelectionState,
} from "@tanstack/react-table";
import { ChevronDown, ChevronUp, ChevronsUpDown, MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface DataTableColumnHeader<TData, TValue>
  extends React.HTMLAttributes<HTMLDivElement> {
  column: ColumnDef<TData, TValue>;
  title: string;
}

export function DataTableColumnHeader<TData, TValue>({
  column,
  title,
  className,
}: DataTableColumnHeader<TData, TValue>) {
  const columnDef = column as unknown as {
    getCanSort?: () => boolean;
    getIsSorted?: () => "asc" | "desc" | false;
    toggleSorting?: (desc?: boolean) => void;
  };
  
  if (!columnDef.getCanSort?.()) {
    return <div className={cn(className)}>{title}</div>;
  }

  const sorted = columnDef.getIsSorted?.() ?? false;

  return (
    <Button
      variant="ghost"
      size="sm"
      className={cn("-ml-4 h-8 data-[state=open]:bg-accent", className)}
      onClick={(e) => {
        columnDef.toggleSorting?.(sorted === "asc");
        e.stopPropagation();
      }}
    >
      <span>{title}</span>
      {sorted === "desc" ? (
        <ChevronDown className="ml-2 h-4 w-4" />
      ) : sorted === "asc" ? (
        <ChevronUp className="ml-2 h-4 w-4" />
      ) : (
        <ChevronsUpDown className="ml-2 h-4 w-4" />
      )}
    </Button>
  );
}

interface DataTableRowActions<TData> {
  row: TData;
  onEdit?: (row: TData) => void;
  onDelete?: (row: TData) => void;
  onView?: (row: TData) => void;
  customActions?: {
    label: string;
    onClick: (row: TData) => void;
    icon?: React.ReactNode;
  }[];
}

export function DataTableRowActions<TData>({
  row,
  onEdit,
  onDelete,
  onView,
  customActions,
}: DataTableRowActions<TData>) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-8 w-8 p-0">
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {onView && (
          <DropdownMenuItem onClick={() => onView(row)}>View</DropdownMenuItem>
        )}
        {onEdit && (
          <DropdownMenuItem onClick={() => onEdit(row)}>Edit</DropdownMenuItem>
        )}
        {customActions?.map((action, index) => (
          <DropdownMenuItem
            key={index}
            onClick={() => action.onClick(row)}
          >
            {action.icon}
            {action.label}
          </DropdownMenuItem>
        ))}
        {onDelete && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={() => onDelete(row)}
              className="text-red-600 focus:text-red-600"
            >
              Delete
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  pageCount?: number;
  pageIndex?: number;
  pageSize?: number;
  onPaginationChange?: (pageIndex: number, pageSize: number) => void;
  onRowSelectionChange?: (rowSelection: RowSelectionState) => void;
  onSortingChange?: (sorting: SortingState) => void;
  sorting?: SortingState;
  rowSelection?: RowSelectionState;
  enableRowSelection?: boolean;
  enableSorting?: boolean;
  enableFiltering?: boolean;
  showPagination?: boolean;
  emptyMessage?: string;
}

export function DataTable<TData, TValue>({
  columns,
  data,
  pageCount,
  pageIndex: controlledPageIndex,
  pageSize: controlledPageSize,
  onPaginationChange,
  onRowSelectionChange,
  onSortingChange,
  sorting,
  rowSelection,
  enableRowSelection = false,
  enableSorting = true,
  enableFiltering = false,
  showPagination = true,
  emptyMessage = "No data found",
}: DataTableProps<TData, TValue>) {
  const [internalSorting, setInternalSorting] = React.useState<SortingState>([]);
  const [internalRowSelection, setInternalRowSelection] = React.useState<RowSelectionState>({});
  const [internalPagination, setInternalPagination] = React.useState<PaginationState>({
    pageIndex: controlledPageIndex || 0,
    pageSize: controlledPageSize || 10,
  });

  const currentSorting = sorting ?? internalSorting;
  const currentRowSelection = rowSelection ?? internalRowSelection;
  const currentPagination = {
    pageIndex: controlledPageIndex ?? internalPagination.pageIndex,
    pageSize: controlledPageSize ?? internalPagination.pageSize,
  };

  const table = useReactTable({
    data,
    columns,
    pageCount: pageCount ?? -1,
    state: {
      sorting: currentSorting,
      rowSelection: currentRowSelection,
      pagination: currentPagination,
    },
    enableRowSelection,
    onSortingChange: (updater) => {
      const newSorting = typeof updater === "function" ? updater(currentSorting) : updater;
      setInternalSorting(newSorting);
      onSortingChange?.(newSorting);
    },
    onRowSelectionChange: (updater) => {
      const newSelection = typeof updater === "function" ? updater(currentRowSelection) : updater;
      setInternalRowSelection(newSelection);
      onRowSelectionChange?.(newSelection);
    },
    onPaginationChange: (updater) => {
      const newPagination = typeof updater === "function" ? updater(currentPagination) : updater;
      setInternalPagination(newPagination);
      onPaginationChange?.(newPagination.pageIndex, newPagination.pageSize);
    },
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: enableSorting ? getSortedRowModel() : undefined,
    getFilteredRowModel: enableFiltering ? getFilteredRowModel() : undefined,
    manualPagination: pageCount !== undefined,
    manualSorting: true,
  });

  return (
    <div className="space-y-4">
      <div className="rounded-md border">
        <table className="w-full caption-bottom text-sm">
          <thead className="[&_tr]:border-b">
            {table.getHeaderGroups().map((headerGroup) => (
              <tr
                key={headerGroup.id}
                className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted"
              >
                {headerGroup.headers.map((header) => (
                  <th
                    key={header.id}
                    className="h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0"
                  >
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody className="[&_tr:last-child]:border-0">
            {table.getRowModel().rows?.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="h-24 text-center text-muted-foreground"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                  className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted"
                >
                  {row.getVisibleCells().map((cell) => (
                    <td
                      key={cell.id}
                      className="p-4 align-middle [&:has([role=checkbox])]:pr-0"
                    >
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showPagination && (
        <div className="flex items-center justify-between px-2">
          <div className="flex-1 text-sm text-muted-foreground">
            {table.getFilteredSelectedRowModel().rows.length} of{" "}
            {table.getFilteredRowModel().rows.length} row(s) selected
          </div>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
