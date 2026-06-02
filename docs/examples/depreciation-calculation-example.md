## Problem Statement
On January 1, 2024, an enterprise purchased a new server system with the following financial details:
* Invoice price (excluding VAT): 250,000,000 VND.
* Trade discount received: 5,000,000 VND.
* Transport, installation, and trial run costs: 10,000,000 VND.
* Estimated useful life: 5 years.

**Requirement** 
1. Determine the Original Cost of this fixed asset.
2. Assume the asset is depreciated using the straight-line method. After 3 years of use (on January 1, 2027), the enterprise upgrades the server system at a total cost of 50,000,000 VND. Following the upgrade, the reassessed useful life is 4 years (starting from 2027). Determine the annual depreciation expense before and after the upgrade.
3. Assume the asset is NOT upgraded and is depreciated using the adjusted declining balance method from the beginning. Create a 5-year depreciation schedule and pinpoint the exact year the enterprise must switch to the straight-line method.

**Solutions**
1. According to Article 4, Circular 45/2013/TT-BTC, the original cost of a purchased tangible fixed asset includes the actual purchase price plus any directly attributable costs incurred to bring the asset to its working condition, minus any trade discounts.

    `Original Cost = Purchase Price - Trade Discount + Installation Costs`

    `Original Cost = 250,000,000 - 5,000,000 + 10,000,000 = 255,000,000 VND`

2. 
* **Phase 1**: Before Upgrade (2024 - 2026)

    `Annual Depreciation Expense = Original Cost / Useful Life = 255,000,000 / 5 = 51,000,000 VND/year`.

    After 3 years (end of 2026), `Accumulated Depreciation = 51,000,000 × 3 = 153,000,000 VND`.

* **Phase 2**: At the point of Upgrade (January 1, 2027)

    According to Point b, Clause 4, Appendix 1 of Circular 45, when an asset is upgraded, the net book value is locked in to calculate the new cycle: 

    `New Original Cost = 255,000,000 + 50,000,000 (upgrade cost) = 305,000,000 VND`.

    `Accumulated Depreciation (remains unchanged) = 153,000,000 VND`.

    `Net Book Value = 305,000,000 - 153,000,000 = 152,000,000 VND`.

* **Phase 3**: After Upgrade (2027 - 2030)

    `New Annual Depreciation Expense = Net Book Value / Reassessed Useful Life = 152,000,000 / 4 = 38,000,000 VND/year`.

3. According to Clause 2, Appendix 2 of Circular 45, an asset with a 5-year useful life falls into the "Over 4 years to 6 years" category, which applies an adjustment multiplier of 2.0.

    `Straight-line rate = 1 / 5 = 20%`

    `Accelerated rate = 20% × 2.0 = 40%`

**Detailed Schedule:**

| Year | Opening Net Book Value (VND) | Declining Balance Depr. (40%) | Average Straight-Line Depr. | Actual Depreciation Expense (VND) | Closing Accumulated Depreciation (VND) |
|---|---|---|---|---|---|
| 1 (2024) | 255,000,000 | 102,000,000 | 51,000,000 (Divide by 5) | 102,000,000 | 102,000,000 |
| 2 (2025) | 153,000,000 | 61,200,000 | 38,250,000 (Divide by 4) | 61,200,000 | 163,200,000 | 
| 3 (2026) | 91,800,000 | 36,720,000 | 30,600,000 (Divide by 3) | 36,720,000 | 199,920,000 | 
| 4 (2027) | 55,080,000 | 22,032,000 | 27,540,000 (Divide by 2) | 27,540,000 | 227,460,000 | 
| 5 (2028) | 27,540,000 | - | 27,540,000 (Divide by 1) | 27,540,000 | 255,000,000 | 

**Crossover Explanation**: In Year 4, the depreciation expense calculated using the declining balance method (22,032,000 VND) is lower than the average expense divided evenly over the remaining 2 years (27,540,000 VND). Complying with Circular 45, from Year 4 onwards, the enterprise is required to switch to the straight-line method (27,540,000 VND) to ensure full capital recovery by the end of Year 5.