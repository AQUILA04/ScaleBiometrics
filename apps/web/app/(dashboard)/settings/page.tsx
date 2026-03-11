"use client";

import { useState } from "react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Save, Upload, Settings, Fingerprint, Database } from "lucide-react";
import { toast } from "sonner";

const mockSettings = {
  general: {
    name: "Acme Corporation",
    logo: "",
    contactEmail: "admin@acme.com",
    contactPhone: "+1234567890",
  },
  matching: {
    confidenceThreshold: 30,
    topK: 10,
    annEnabled: true,
    algorithm: "SOURCEAFIS",
  },
  storage: {
    retentionDays: 90,
    compressionEnabled: true,
    autoArchiveEnabled: false,
  },
};

export default function SettingsPage() {
  const [generalSettings, setGeneralSettings] = useState(mockSettings.general);
  const [matchingSettings, setMatchingSettings] = useState(mockSettings.matching);
  const [storageSettings, setStorageSettings] = useState(mockSettings.storage);

  const handleSave = () => {
    // Mock - in real implementation would call updateSettings.mutate
    toast.success("Settings saved successfully");
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Settings</h1>
        <p className="text-muted-foreground mt-1">
          Configure your tenant settings and preferences
        </p>
      </div>

      <Tabs defaultValue="general" className="space-y-4">
        <TabsList>
          <TabsTrigger value="general" className="gap-2">
            <Settings className="h-4 w-4" />
            General
          </TabsTrigger>
          <TabsTrigger value="matching" className="gap-2">
            <Fingerprint className="h-4 w-4" />
            Matching
          </TabsTrigger>
          <TabsTrigger value="storage" className="gap-2">
            <Database className="h-4 w-4" />
            Storage
          </TabsTrigger>
        </TabsList>

        <TabsContent value="general">
          <Card>
            <CardHeader>
              <CardTitle>General Settings</CardTitle>
              <CardDescription>
                Manage your organization details and contact information
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="tenantName">Organization Name</Label>
                  <Input
                    id="tenantName"
                    value={generalSettings.name}
                    onChange={(e) => setGeneralSettings({ ...generalSettings, name: e.target.value })}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Logo</Label>
                  <div className="flex items-center gap-4">
                    <div className="relative h-16 w-16 rounded-lg border bg-muted flex items-center justify-center">
                      {generalSettings.logo ? (
                        <Image src={generalSettings.logo} alt="Logo" fill className="object-contain p-1" />
                      ) : (
                        <Upload className="h-6 w-6 text-muted-foreground" />
                      )}
                    </div>
                    <Button variant="outline" type="button">
                      <Upload className="mr-2 h-4 w-4" />
                      Upload Logo
                    </Button>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    PNG, JPG or SVG. Max 2MB.
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="contactEmail">Contact Email</Label>
                    <Input
                      id="contactEmail"
                      type="email"
                      value={generalSettings.contactEmail}
                      onChange={(e) => setGeneralSettings({ ...generalSettings, contactEmail: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="contactPhone">Contact Phone</Label>
                    <Input
                      id="contactPhone"
                      type="tel"
                      value={generalSettings.contactPhone}
                      onChange={(e) => setGeneralSettings({ ...generalSettings, contactPhone: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <Button onClick={handleSave}>
                <Save className="mr-2 h-4 w-4" />
                Save Changes
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="matching">
          <Card>
            <CardHeader>
              <CardTitle>Matching Configuration</CardTitle>
              <CardDescription>
                Configure biometric matching algorithm parameters
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>Confidence Threshold: {matchingSettings.confidenceThreshold}</Label>
                  <Slider
                    value={[matchingSettings.confidenceThreshold]}
                    onValueChange={([value]) => setMatchingSettings({ ...matchingSettings, confidenceThreshold: value })}
                    min={0}
                    max={100}
                    step={1}
                  />
                  <p className="text-xs text-muted-foreground">
                    Minimum confidence score required for a match (0-100)
                  </p>
                </div>

                <div className="space-y-2">
                  <Label>Top-K: {matchingSettings.topK}</Label>
                  <Slider
                    value={[matchingSettings.topK]}
                    onValueChange={([value]) => setMatchingSettings({ ...matchingSettings, topK: value })}
                    min={1}
                    max={100}
                    step={1}
                  />
                  <p className="text-xs text-muted-foreground">
                    Number of top candidates to return (1-100)
                  </p>
                </div>

                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <Label>Enable ANN (Approximate Nearest Neighbor)</Label>
                    <p className="text-sm text-muted-foreground">
                      Use ANN for faster searching with large datasets
                    </p>
                  </div>
                  <Switch
                    checked={matchingSettings.annEnabled}
                    onCheckedChange={(checked) => setMatchingSettings({ ...matchingSettings, annEnabled: checked })}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="algorithm">Algorithm</Label>
                  <select
                    id="algorithm"
                    className="w-full h-10 rounded-md border border-input bg-background px-3 py-2"
                    value={matchingSettings.algorithm}
                    onChange={(e) => setMatchingSettings({ ...matchingSettings, algorithm: e.target.value })}
                  >
                    <option value="SOURCEAFIS">SourceAFIS</option>
                    <option value="MINDTCT">MINDTCT</option>
                    <option value="NBIS">NBIS</option>
                  </select>
                </div>
              </div>

              <Button onClick={handleSave}>
                <Save className="mr-2 h-4 w-4" />
                Save Changes
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="storage">
          <Card>
            <CardHeader>
              <CardTitle>Storage Configuration</CardTitle>
              <CardDescription>
                Configure data retention and storage policies
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>Retention Period: {storageSettings.retentionDays} days</Label>
                  <Slider
                    value={[storageSettings.retentionDays]}
                    onValueChange={([value]) => setStorageSettings({ ...storageSettings, retentionDays: value })}
                    min={1}
                    max={365}
                    step={1}
                  />
                  <p className="text-xs text-muted-foreground">
                    Number of days to retain biometric data (1-365)
                  </p>
                </div>

                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <Label>Enable Compression</Label>
                    <p className="text-sm text-muted-foreground">
                      Compress stored templates to reduce storage usage
                    </p>
                  </div>
                  <Switch
                    checked={storageSettings.compressionEnabled}
                    onCheckedChange={(checked) => setStorageSettings({ ...storageSettings, compressionEnabled: checked })}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <Label>Enable Auto-Archive</Label>
                    <p className="text-sm text-muted-foreground">
                      Automatically archive inactive records to cold storage
                    </p>
                  </div>
                  <Switch
                    checked={storageSettings.autoArchiveEnabled}
                    onCheckedChange={(checked) => setStorageSettings({ ...storageSettings, autoArchiveEnabled: checked })}
                  />
                </div>
              </div>

              <Button onClick={handleSave}>
                <Save className="mr-2 h-4 w-4" />
                Save Changes
              </Button>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
