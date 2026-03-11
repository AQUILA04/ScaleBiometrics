"use client";

import { useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader } from "@/components/ui/data-table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useMatchingResults } from "@/hooks/use-matching";
import { Search, Eye, CheckCircle, XCircle } from "lucide-react";
import type { MatchingResult, MatchResultType } from "@/types/matching";
import { toast } from "sonner";

const mockResults: MatchingResult[] = [
  {
    id: "1",
    jobId: "job-001",
    type: "1:N",
    probeRid: "RID-001",
    probeFilename: "fingerprint_001.png",
    matchFound: true,
    candidateCount: 3,
    topScore: 45,
    topConfidence: 92.5,
    processedAt: "2026-03-09T10:35:00Z",
  },
  {
    id: "2",
    jobId: "job-002",
    type: "1:1",
    probeRid: "RID-002",
    probeFilename: "fingerprint_002.png",
    matchFound: true,
    candidateCount: 1,
    topScore: 52,
    topConfidence: 98.0,
    processedAt: "2026-03-09T10:32:00Z",
  },
  {
    id: "3",
    jobId: "job-003",
    type: "1:N",
    probeRid: "RID-003",
    probeFilename: "fingerprint_003.png",
    matchFound: false,
    candidateCount: 0,
    processedAt: "2026-03-09T10:28:00Z",
  },
  {
    id: "4",
    jobId: "job-004",
    type: "1:N",
    probeRid: "RID-004",
    probeFilename: "fingerprint_004.png",
    matchFound: true,
    candidateCount: 5,
    topScore: 38,
    topConfidence: 85.0,
    processedAt: "2026-03-09T10:25:00Z",
  },
  {
    id: "5",
    jobId: "job-005",
    type: "1:1",
    probeRid: "RID-005",
    probeFilename: "fingerprint_005.png",
    matchFound: false,
    candidateCount: 0,
    processedAt: "2026-03-09T10:20:00Z",
  },
  {
    id: "6",
    jobId: "job-006",
    type: "1:N",
    probeRid: "RID-006",
    probeFilename: "fingerprint_006.png",
    matchFound: true,
    candidateCount: 2,
    topScore: 41,
    topConfidence: 89.0,
    processedAt: "2026-03-09T10:15:00Z",
  },
  {
    id: "7",
    jobId: "job-007",
    type: "1:N",
    probeRid: "RID-007",
    probeFilename: "fingerprint_007.png",
    matchFound: false,
    candidateCount: 0,
    processedAt: "2026-03-09T10:10:00Z",
  },
  {
    id: "8",
    jobId: "job-008",
    type: "1:1",
    probeRid: "RID-008",
    probeFilename: "fingerprint_008.png",
    matchFound: true,
    candidateCount: 1,
    topScore: 55,
    topConfidence: 99.0,
    processedAt: "2026-03-09T10:05:00Z",
  },
  {
    id: "9",
    jobId: "job-009",
    type: "1:N",
    probeRid: "RID-009",
    probeFilename: "fingerprint_009.png",
    matchFound: true,
    candidateCount: 4,
    topScore: 35,
    topConfidence: 82.0,
    processedAt: "2026-03-09T10:00:00Z",
  },
  {
    id: "10",
    jobId: "job-010",
    type: "1:N",
    probeRid: "RID-010",
    probeFilename: "fingerprint_010.png",
    matchFound: false,
    candidateCount: 0,
    processedAt: "2026-03-09T09:55:00Z",
  },
];

const columns: ColumnDef<MatchingResult>[] = [
  {
    accessorKey: "id",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Result ID" />
    ),
    cell: ({ row }) => (
      <span className="font-mono text-xs">{row.getValue("id")}</span>
    ),
  },
  {
    accessorKey: "type",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Type" />
    ),
    cell: ({ row }) => (
      <span
        className={`rounded-full px-2 py-1 text-xs font-medium ${
          row.getValue("type") === "1:N"
            ? "bg-blue-500/10 text-blue-500"
            : "bg-purple-500/10 text-purple-500"
        }`}
      >
        {row.getValue("type")}
      </span>
    ),
  },
  {
    accessorKey: "probeRid",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Probe RID" />
    ),
  },
  {
    accessorKey: "matchFound",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Result" />
    ),
    cell: ({ row }) => {
      const matchFound = row.getValue("matchFound") as boolean;
      return (
        <div className="flex items-center gap-2">
          {matchFound ? (
            <CheckCircle className="h-4 w-4 text-green-500" />
          ) : (
            <XCircle className="h-4 w-4 text-muted-foreground" />
          )}
          <span className={matchFound ? "text-green-500" : "text-muted-foreground"}>
            {matchFound ? "Match Found" : "No Match"}
          </span>
        </div>
      );
    },
  },
  {
    accessorKey: "candidateCount",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Candidates" />
    ),
    cell: ({ row }) => {
      const count = row.getValue("candidateCount") as number;
      return count > 0 ? count : "-";
    },
  },
  {
    accessorKey: "topConfidence",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Confidence" />
    ),
    cell: ({ row }) => {
      const confidence = row.getValue("topConfidence") as number | undefined;
      return confidence ? `${confidence}%` : "-";
    },
  },
  {
    accessorKey: "processedAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Processed" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("processedAt"));
      return date.toLocaleString();
    },
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const result = row.original;
      return (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            toast.info(`View result: ${result.id}`);
          }}
        >
          <Eye className="h-4 w-4" />
        </Button>
      );
    },
  },
];

export default function MatchingPage() {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [resultFilter, setResultFilter] = useState<MatchResultType | "">("");
  const [searchTerm, setSearchTerm] = useState("");

  const { data } = useMatchingResults("default", pageIndex, pageSize, {
    result: resultFilter || undefined,
    search: searchTerm || undefined,
  });

  const results = data?.content || mockResults;
  const totalPages = data?.totalPages || Math.ceil(mockResults.length / pageSize);

  const matchCount = mockResults.filter(r => r.matchFound).length;
  const noMatchCount = mockResults.filter(r => !r.matchFound).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Matching Results</h1>
        <p className="text-muted-foreground mt-1">
          View historical matching results and verify algorithm accuracy
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockResults.length}</p>
              <p className="text-sm text-muted-foreground">Total Results</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-500">{matchCount}</p>
              <p className="text-sm text-muted-foreground">Matches Found</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-muted-foreground">{noMatchCount}</p>
              <p className="text-sm text-muted-foreground">No Matches</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">
                {mockResults.length > 0 
                  ? Math.round((matchCount / mockResults.length) * 100) 
                  : 0}%
              </p>
              <p className="text-sm text-muted-foreground">Match Rate</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Results History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-center gap-4 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search by RID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm"
              />
            </div>
            <select
              value={resultFilter}
              onChange={(e) => setResultFilter(e.target.value as MatchResultType | "")}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Results</option>
              <option value="MATCH_FOUND">Match Found</option>
              <option value="NO_MATCH">No Match</option>
            </select>
          </div>

          <DataTable
            columns={columns}
            data={results}
            pageCount={totalPages}
            pageIndex={pageIndex}
            pageSize={pageSize}
            onPaginationChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
            enableSorting
            emptyMessage="No matching results found"
          />
        </CardContent>
      </Card>
    </div>
  );
}
