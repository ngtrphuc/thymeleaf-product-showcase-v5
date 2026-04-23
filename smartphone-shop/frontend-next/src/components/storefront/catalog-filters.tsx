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
  { label: "Over 1TB", value: "over1tb" },
];

const PRICE_RANGE_OPTIONS: Option[] = [
  { label: "Any price", value: "" },
  { label: "Under 150,000", value: "under150" },
  { label: "150,000 - 179,999", value: "150to179" },
  { label: "180,000 - 209,999", value: "180to209" },
  { label: "210,000 - 239,999", value: "210to239" },
  { label: "240,000 - 269,999", value: "240to269" },
  { label: "270,000 - 299,999", value: "270to299" },
  { label: "Over 300,000", value: "over300" },
];

const BATTERY_RANGE_OPTIONS: Option[] = [
  { label: "Any battery", value: "" },
  { label: "Under 3,500 mAh", value: "under3500" },
  { label: "3,500 - 3,999 mAh", value: "3500to3999" },
  { label: "4,000 - 4,499 mAh", value: "4000to4499" },
  { label: "4,500 - 4,999 mAh", value: "4500to4999" },
  { label: "5,000 - 5,499 mAh", value: "5000to5499" },
  { label: "5,500 - 5,999 mAh", value: "5500to5999" },
  { label: "6,000 - 6,999 mAh", value: "6000to6999" },
  { label: "Over 7,000 mAh", value: "over7000" },
];

const SCREEN_SIZE_OPTIONS: Option[] = [
  { label: "Any screen size", value: "" },
  { label: "6.1 - 6.3 inch", value: "6.1to6.3" },
  { label: "6.4 - 6.6 inch", value: "6.4to6.6" },
  { label: "6.7 - 6.9 inch", value: "6.7to6.9" },
  { label: "7.0+ inch", value: "over7.0" },
  { label: "7.0 - 7.9 inch", value: "7.0to7.9" },
  { label: "8.0+ inch", value: "8.0plus" },
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
  const [screenSize, setScreenSize] = useState(initialValues.screenSize);

  const brandOptions = useMemo<Option[]>(
    () => [{ label: "All brands", value: "" }, ...brands.map((item) => ({ label: item, value: item }))],
    [brands],
  );

  type FilterDraft = {
    keyword: string;
    brand: string;
    sort: string;
    storage: string;
    priceRange: string;
    priceMin: string;
    priceMax: string;
    batteryRange: string;
    screenSize: string;
  };

  function applyFilters(overrides?: Partial<FilterDraft>) {
    const draft: FilterDraft = {
      keyword,
      brand,
      sort,
      storage,
      priceRange,
      priceMin,
      priceMax,
      batteryRange,
      screenSize,
      ...overrides,
    };

    const params = new URLSearchParams();
    filterValue(params, "keyword", draft.keyword);
    filterValue(params, "brand", draft.brand);
    filterValue(params, "sort", draft.sort);
    filterValue(params, "storage", draft.storage);
    filterValue(params, "priceRange", draft.priceRange);
    filterValue(params, "priceMin", draft.priceMin);
    filterValue(params, "priceMax", draft.priceMax);
    filterValue(params, "batteryRange", draft.batteryRange);
    filterValue(params, "screenSize", draft.screenSize);
    params.set("page", "0");

    const query = params.toString();
    router.push(`${pathname}${query ? `?${query}` : ""}`);
  }

  function onSortChange(nextSort: string) {
    setSort(nextSort);
    applyFilters({ sort: nextSort });
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
    setScreenSize("");
    router.push(pathname);
  }

  return (
    <section className="glass-panel relative z-20 rounded-3xl p-5">
      <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-4">
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Keyword</span>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Example: iPhone, Samsung"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>

        <FilterDropdown label="Sort" options={SORT_OPTIONS} value={sort} onChange={onSortChange} />
        <FilterDropdown label="Brand" options={brandOptions} value={brand} onChange={setBrand} />
        <FilterDropdown label="Storage" options={STORAGE_OPTIONS} value={storage} onChange={setStorage} />

        <FilterDropdown label="Price Range" options={PRICE_RANGE_OPTIONS} value={priceRange} onChange={setPriceRange} />
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-slate-700">Price Min</span>
          <input
            type="number"
            min="0"
            value={priceMin}
            onChange={(event) => setPriceMin(event.target.value)}
            placeholder="Min"
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
            placeholder="Max"
            className="ui-input px-3 py-2 text-sm"
          />
        </label>

        <FilterDropdown label="Battery Range" options={BATTERY_RANGE_OPTIONS} value={batteryRange} onChange={setBatteryRange} />
        <FilterDropdown label="Screen Size" options={SCREEN_SIZE_OPTIONS} value={screenSize} onChange={setScreenSize} />
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => applyFilters()}
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
