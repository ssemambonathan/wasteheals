# WasteHeals Financial System Specification

## 1. Numeric Precision
- **No binary floating-point (`Float` / `Double`)** is used anywhere in financial or mass math.
- Money is stored as whole **Uganda Shillings (`UGX` `Long`)** and calculated with `java.math.BigDecimal` (`RoundingMode.HALF_UP`).
- Mass is stored as **`milliKg` (`Long`)** where `1,000 milliKg = 1.000 kg`.

## 2. Double-Entry Chart of Accounts
Every financial event posts a balanced `JournalEntry` + `JournalLine` batch verified by `DoubleEntryAccountingEngine.buildAndVerify`:
- `1010` **Operating Cash (Bank / MoMo Float)** (`ASSET`)
- `1020` **Restricted Healthcare Cash Fund** (`RESTRICTED_FUND`)
- `1100` **Buyer Accounts Receivable** (`ASSET`)
- `1200` **Recovered Material Inventory** (`ASSET`)
- `2010` **Guaranteed Picker Earnings Payable** (`LIABILITY`)
- `2110` **Healthcare Contribution Accrued (Unfunded)** (`LIABILITY`)
- `2120` **Healthcare Fund Liability (Funded)** (`LIABILITY`)
- `4010` **Recovered Material & Compost Sales Revenue** (`REVENUE`)
- `5010` **Cost of Sold Waste Inventory** (`EXPENSE`)
- `5110` **Healthcare Contribution Expense (UGX 30/kg)** (`EXPENSE`)
- `5200` **Sorting & Organic Processing Expense** (`EXPENSE`)

## 3. Event Posting Rules
1. **Delivery Acceptance**: `DR 1200 Inventory` / `CR 2010 Picker Payable`.
2. **MoMo Payout Success**: `DR 2010 Picker Payable` / `CR 1010 Operating Cash` (posted ONLY after verified `SUCCESS`).
3. **Confirmed Buyer Sale**:
   - `DR 1100 Buyer Receivable` / `CR 4010 Sales Revenue`
   - `DR 5010 COGS` / `CR 1200 Inventory`
   - `DR 5110 HC Expense` / `CR 2110 HC Accrued Unfunded` at `policy.healthcareUgxPerKg` (`UGX 30/kg`) * confirmed sold kg.
4. **Buyer Cash Receipt**:
   - `DR 1010 Operating Cash` / `CR 1100 Buyer Receivable`
   - Proportional transfer: `DR 1020 Restricted HC Cash` / `CR 1010 Operating Cash`, and `DR 2110 HC Accrued Unfunded` / `CR 2120 HC Liability Funded`.
5. **Healthcare Claim Payment**: `DR 2120 HC Liability Funded` / `CR 1020 Restricted HC Cash`.

## 4. Surplus & Liquidity Protection Gate
- Accounting surplus deducts COGS, transport/sorting, healthcare contributions, operating expenses, rejections/losses, and taxes.
- Even when accounting surplus > 0, `SurplusAndLiquidityEngine` **blocks distribution** unless `Operating Cash - (Unsettled Picker Payables + Unfunded HC Transfers + Operating Payables + Taxes + Minimum Working Capital + Contingency Reserve) > 0`.
- Default Policy v1 split: **30% Picker Reward Pool** (weighted 50% kg / 25% quality / 15% consistency / 10% safety), **35% Operations**, **20% Contingency**, **15% Expansion**.
