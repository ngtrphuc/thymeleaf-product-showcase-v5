"use client";

import { usePathname, useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { FilterDropdown, type FilterDropdownOption as Option } from "@/components/storefront/filter-dropdown";
import { GriddyIcon } from "@/components/ui/griddy-icon";

type CatalogFiltersProps = {
  brands: string[];
  initialValues: {
    keyword: string;
    brand: string;
    sort: string;
    storage: string;
    priceRange: string;
    priceMin: string;
    priceMax: string;
    batteryRange: string;
    batteryMin: string;
    batteryMax: string;
    screenSize: string;
  };
};

const SORT_OPTIONS: Option[] = [
  { label: "Best Match", value: "relevance" },
  { label: "Name A-Z", value: "name_asc" },
  { label: "Name Z-A", value: "name_desc" },
  { label: "Price Low to High", value: "price_asc" },
  { label: "Price High to Low", value: "price_desc" },
];

const STORAGE_OPTIONS: Option[] = [
  { label: "All storage", value: "" },
  { label: "64GB", value: "64gb" },
  { label: "128GB", value: "128gb" },
  { label: "256GB", value: "256gb" },
  { label: "512GB", value: "512gb" },
  { label: "1TB", value: "1tb" },
];

const PRICE_RANGE_OPTIONS: Option[] = [
  { label: "Any price", value: "" },
  { label: "Under 150,000", value: "under150" },
  { label: "150,000 - 200,000", value: "150to200" },
  { label: "200,000 - 250,000", value: "200to250" },
  { label: "Over 250,000", value: "over250" },
];

const BATTERY_RANGE_OPTIONS: Option[] = [
  { label: "Any battery", value: "" },
  { label: "Under 5000 mAh", value: "under5000" },
  { label: "5000+ mAh", value: "over5000" },
];

const SCREEN_SIZE_OPTIONS: Option[] = [
  { label: "Any screen size", value: "" },
  { label: "Under 6.5 inch", value: "under6.5" },
  { label: "6.5 - 6.8 inch", value: "6.5to6.8" },
  { label: "Over 6.8 inch", value: "over6.8" },
];

function filterValue(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}

export function CatalogFilters({ brands, initialValues }: CatalogFiltersProps) {
  const router = useRouter();
  const pathname = usePathname();
  const defaultSort = initialValues.keyword ? "relevance" : "name_asc";

  const [keyword, setKeyword] = useState(initialValues.keyword);
  const [brand, setBrand] = useState(initialValues.brand);
  const [sort, setSort] = useState(initialValues.sort || defaultSort);
  const [storage, setStorage] = useState(initialValues.storage);
  const [priceRange, setPriceRange] = useState(initialValues.priceRange);
  const [priceMin, setPriceMin] = useState(initialValues.priceMin);
  const [priceMax, setPriceMax] = useState(initialValues.priceMax);
  const [batteryRange, setBatteryRange] = useState(initialValues.batteryRange);
  const [batteryMin, setBatteryMin] = useState(initialValues.batteryMin);
  const [batteryMax, setBatteryMax] = useState(initialValues.batteryMax);
  const [screenSize, setScreenSize] = useState(initialValues.screenSize);

  const brandOptions = useMemo<Option[]>(
    () => [{ label: "All brands", value: "" }, ...brands.map((item) => ({ label: item, value: item }))],
    [brands],
  );

  function applyFilters() {
    const params = new URLSearchParams();
    filterValue(params, "keyword", keyword);
    filterValue(params, "brand", brand);
    filterValue(params, "sort", sort);
    filterValue(params, "storage", storage);
    filterValue(params, "priceRange", priceRange);
    filterValue(params, "priceMin", priceMin);
    filterValue(params, "priceMax", priceMax);
    filterValue(params, "batteryRange", batteryRange);
    filterValue(params, "batteryMin", batteryMin);
    filterValue(params, "batteryMax", batteryMax);
    filterValue(params, "screenSize", screenSize);
    params.set("page", "0");

    const query = params.toString();
    router.push(`${pathname}${query ? `?${query}` : ""}`);
  }

  function clearFilters() {
    setKeyword("");
    setBrand("");
    setSort("name_asc");
    setStorage("");
    setPriceRange("");
    setPriceMin("");
    setPriceMax("");
    setBatteryRange("");
    setBatteryMin("");
    setBatteryMax("");
    setScreenSize("");
    router.push(pathname);
  }

  return (
    <section className="glass-panel rounded-3xl p-5">
      <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-4">
        <label className="flex flex-col gap-2 md:col-span-2 lg:col-span-1">
          <span className="text-sm font-medium text-slate-700">Keyword</span>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Example: iPhone, Samsung"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>

        <FilterDropdown label="Brand" options={brandOptions} value={brand} onChange={setBrand} />
        <FilterDropdown label="Sort" options={SORT_OPTIONS} value={sort} onChange={setSort} />
        <FilterDropdown label="Storage" options={STORAGE_OPTIONS} value={storage} onChange={setStorage} />

        <FilterDropdown label="Price Range" options={PRICE_RANGE_OPTIONS} value={priceRange} onChange={setPriceRange} />
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Price Min</span>
          <input
            type="number"
            min="0"
            value={priceMin}
            onChange={(event) => setPriceMin(event.target.value)}
            placeholder="Optional"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Price Max</span>
          <input
            type="number"
            min="0"
            value={priceMax}
            onChange={(event) => setPriceMax(event.target.value)}
            placeholder="Optional"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>

        <FilterDropdown label="Battery Range" options={BATTERY_RANGE_OPTIONS} value={batteryRange} onChange={setBatteryRange} />
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Battery Min (mAh)</span>
          <input
            type="number"
            min="0"
            value={batteryMin}
            onChange={(event) => setBatteryMin(event.target.value)}
            placeholder="Optional"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Battery Max (mAh)</span>
          <input
            type="number"
            min="0"
            value={batteryMax}
            onChange={(event) => setBatteryMax(event.target.value)}
            placeholder="Optional"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>

        <FilterDropdown label="Screen Size" options={SCREEN_SIZE_OPTIONS} value={screenSize} onChange={setScreenSize} />
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={applyFilters}
          className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-5 py-2.5 text-sm hover:-translate-y-1 hover:shadow-[0_12px_26px_rgba(255,255,255,0.24)]"
        >
          <GriddyIcon name="check" />
          Apply Filters
        </button>
        <button
          type="button"
          onClick={clearFilters}
          className="ui-btn ui-btn-secondary inline-flex items-center gap-2 px-5 py-2.5 text-sm"
        >
          <GriddyIcon name="close-circle" />
          Clear
        </button>
      </div>
    </section>
  );
}
